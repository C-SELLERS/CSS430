import java.util.Vector;

public class FileTable {

    private Vector<FileTableEntry> table;         // the actual entity of this file table
    private Directory dir;        // the root directory 
    private final static int READ = 2;
    private static final int WRITE = 3;

 
    public FileTable( Directory directory ) {       // constructor
       table = new Vector<FileTableEntry>();        // instantiate a file (structure) table
       dir = directory;                             // receive a reference to the Director
    }                                               // from the file system
 
    // allocate a new file (structure) table entry for this file name
    // allocate/retrieve and register the corresponding inode using dir
    // increment this inode'fileName count
    // immediately write back this inode to the disk
    // return a reference to this file (structure) table entry
    public synchronized FileTableEntry falloc(String fileName, String mode) {
        short iNum;
        Inode inode = null;

        while (true) {

            //If its the root, iNum = 0
            if (fileName.equals("/")) {
                iNum = 0;
            } else {
                //Otherwise find the file
                iNum = this.dir.namei(fileName);
            }

            //If we have a file that exists
            if (iNum >= 0) {
                inode = new Inode(iNum);

                //If mode is not read
                if (!mode.equals("r")) {
                    
                    //If the inode is read or write, just wait
                    if(inode.flag == READ || inode.flag == WRITE)
                    {
                        try { wait(); }
                        catch (InterruptedException e) {}

                    //Set flad to write
                    } else {
                        inode.flag = WRITE;
                        break;
                    }
                   
                //Mode is read
                } else {

                    //If its already read we are done
                    if(inode.flag == READ) {
                        break;
                    
                    //If the inode flag is write wait for it
                    } else if(inode.flag == WRITE) {
                        try { wait(); }
                        catch (InterruptedException e) {}

                    //Set the flag to read 
                    } else {
                        inode.flag = READ;
                        break;
                    }
                }

            //If file doesn't exist, create it.
            } else {
                if (!mode.equals("r")) {
                    //Allocate the file 
                    iNum = dir.ialloc(fileName);

                    //Create the iNode and set flag to write
                    inode = new Inode();
                    inode.flag = WRITE;
                }
                break;
            }
        }

        if(inode == null ){
            return null;
        }

        FileTableEntry newEntry = new FileTableEntry(inode,iNum,mode); // allocate a new file table entry for this file name
        table.add(newEntry);    // add new entry to filetable
        inode.count++;          // increment this inode's count
        inode.toDisk(iNum);     // write back inode to the disk 
        return newEntry;
    }
 
    // receive a file table entry reference
    // save the corresponding inode to the disk
    // free this file table entry.
    // return true if this file table entry found in my table
    public synchronized boolean ffree(FileTableEntry entry) {

        //Attempt to remove the entry from the table
        if (this.table.removeElement(entry)) {

            //Get the entry's Inode and reduce the count of active uses
            Inode inode = entry.inode;
            --inode.count;

            //If the count is now 0 set the flag to unused
            if (entry.inode.count == 0) {
                entry.inode.flag = 0;
            }

            //Update the disk and notify any waiting threads
            entry.inode.toDisk(entry.iNumber);
            notifyAll();
            return true;
        }
        return false;
    }
    
    // return if table is empty 
    // should be called before starting a format
    public synchronized boolean fempty( ) {
       return table.isEmpty( ); 
    }  
                              
 }