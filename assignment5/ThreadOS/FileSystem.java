
public class FileSystem {

    private SuperBlock superBlock;
    private Directory directory;
    private FileTable fileTable;
    
    //Constructor
    public FileSystem(int diskBlocks) {
        
        //intialize all the objects we need
    	superBlock = new SuperBlock(diskBlocks);
    	directory = new Directory(superBlock.totalInodes);
        fileTable = new FileTable(directory);
        
        //Open the file
        FileTableEntry dirEntry = open("/", "r");
        int dirSize = fsize(dirEntry);
        
        //if its size is greater than zero build the directory from the bytes
        if (dirSize > 0) {
            byte[] data = new byte[dirSize];
            read(dirEntry, data);
            directory.bytes2directory(data);
        }

        //Close it 
        close(dirEntry);
    }
    
    
    // Opens a file with given name
    public FileTableEntry open(String filename, String mode) {

    	//Create a new entry for the file to be opened
        FileTableEntry newEntry = fileTable.falloc( filename, mode );
        
        // if the mode is write, check if it can dealloccate block or not
        if ( mode.equals("w") && !this.deallocAllBlocks(newEntry)) {
            return null;
        }

        //Return the new entry
        return newEntry;
    }  

    //closes a file
    public boolean close(FileTableEntry entry) {

        //Update the entry
        synchronized (entry) {
            --entry.count;

            //If its greater than zero don't free it up yet
            if (entry.count > 0) {
                return true;
            }
        }

        //Free the file from the table
        return fileTable.ffree(entry);
    }


    //Update superblock
    public void sync() {

        //Open directory location
        FileTableEntry dirEnt = open( "/", "w" );

        //Get directory data
        byte[] dirData = directory.directory2bytes();

        //Update the directory entry and close it
        write(dirEnt, dirData);
        close(dirEnt);

        //Sync the superBlock
        superBlock.sync();
    }


    //Format the disk
    public boolean format(int files) {

        //If the filetable is not empty wait
        while (!fileTable.fempty()) {}

        superBlock.format(files);
        directory = new Directory(superBlock.totalInodes);
        fileTable = new FileTable(directory);
        return true;
    }


    //Delete a given file by name
    public boolean delete(String filename) {

        //Open the file and get the iNumber 
        FileTableEntry file = open(filename, "r");
        if(file == null){
            return false;
        }

        short iNum = file.iNumber;

        //Close the file and free up the location
        return close(file) && directory.ifree(iNum);
    }


    //Get the size of the file in bytes
    public int fsize( FileTableEntry fileTableEntry ) {

        //To ensure one at a time
        synchronized (fileTableEntry) {
            return fileTableEntry.inode.length;
        }
    }

    
    //Deallocates all blocks
    private boolean deallocAllBlocks(FileTableEntry entry) {

        //If we don't only have one entry pointing here we cannot deallocate
        if (entry.inode.count != 1) {
            return false;
        }

        //Free the indirect
        final byte[] data = entry.inode.freeIndexBlock();
        if (data != null) {
            int indirect = SysLib.bytes2short(data, 0);
            if(indirect != -1){
                superBlock.returnBlock(indirect);
            }
        }

        //Free each direct
        for (int i = 0; i < entry.inode.direct.length; i++){

             //If it is not unused make it unused
            if (entry.inode.direct[i] != -1) {
                superBlock.returnBlock((int)entry.inode.direct[i]);
                entry.inode.direct[i] = -1;
            }

        }

        //Update it to Disk
        entry.inode.toDisk(entry.iNumber);
        return true;
    }
 
    //Write the given buffer to the file directed to by entry
    public int write( FileTableEntry entry, byte[] buffer ) {

        //Check to see if it can't write to the file
        if(entry == null || entry.mode == "r"){
            return -1;
        }

        int bytesWritten = 0;

        //Begin the process of writing
        synchronized(entry) {

            while(bytesWritten < buffer.length) 
            {
                //Get the target block
                int targetBlock = entry.inode.findTargetBlock(entry.seekPtr);

                //If no block is returned
                if(targetBlock == -1) {

                    //Get a free block and point the target there
                    short freeBlock = (short) superBlock.getFreeBlock();

                    targetBlock = entry.inode.setTargetBlock(entry.seekPtr, freeBlock);

                    switch(targetBlock) {
                        case -1:
                        case -2: return -1;
                        case -3: {
                            if (!entry.inode.setIndexBlock((short)superBlock.getFreeBlock()) || entry.inode.setTargetBlock(entry.seekPtr, freeBlock) != 0) {
                                return -1;
                            }
                            break;
                        }
                    }
                }

                //Read the targetBlock to a buffer
                byte[] writeBuf = new byte[Disk.blockSize];
                SysLib.rawread(targetBlock, writeBuf);

                //Determine offset of where to write
                short blockOffset = (short) (entry.seekPtr % Disk.blockSize);

                //Determine the bytes left to write, space on the block
                int bytesRemaining = buffer.length - bytesWritten;
                int bytesAvailableOnBlock = Disk.blockSize - blockOffset;

                //The amount of bytes we will write is the minimum of these
                int bytesToWrite = Math.min(bytesRemaining, bytesAvailableOnBlock);

                //Copy this piece of the array to the buffer and write it to the block
                System.arraycopy(buffer, bytesWritten, writeBuf, blockOffset, bytesToWrite);
                SysLib.rawwrite(targetBlock, writeBuf);

                //Update our trackers
                bytesWritten += bytesToWrite;
                entry.seekPtr += bytesToWrite;

                //Update the length of the inode
                if(entry.seekPtr > entry.inode.length){
                    entry.inode.length = entry.seekPtr;
                }
            }

            //Write to disk
            entry.inode.toDisk(entry.iNumber);
            return bytesWritten;
        }
    }
    

    //Read from the file defined by entry up to the size of the buffer given
    //Returns the count of the bytes read.
    public int read( FileTableEntry entry, byte[] buffer ) {

        //Check to see if it can't read from the file
        if(entry == null || entry.mode == "w" || entry.mode == "a"){
            return -1;
        }


        int fileSize = fsize(entry);
        int bytesRead = 0;


        synchronized (entry) {

            //While there is still more to read
            while (bytesRead < buffer.length && entry.seekPtr < fileSize) {

                //Get the target block
                final int targetBlock = entry.inode.findTargetBlock(entry.seekPtr);

                //If not found exit
                if (targetBlock == -1) {
                    break;
                }

                //Create read buffer and read into it
                byte[] data = new byte[Disk.blockSize];
                SysLib.rawread(targetBlock, data);

                //Determine offset
                int offset = entry.seekPtr % Disk.blockSize;

                //Determine bytes left to read, block bytes available, and the entry bytes left
                int bytesLeft = buffer.length - bytesRead;
                int blockBytesAvailable = Disk.blockSize - offset;
                int entryBytesLeft = fileSize - entry.seekPtr;

                //The minimum is the size to read
                int sizeToRead = Math.min(bytesLeft, Math.min(blockBytesAvailable, entryBytesLeft));

                //Copy data to buffer
                System.arraycopy(data, offset, buffer, bytesRead, sizeToRead);

                //Update trackers
                bytesRead += sizeToRead;
                entry.seekPtr += sizeToRead;
            }
            return bytesRead;
        }
    }

    //Update the seek pointer dependent on type
    public int seek( FileTableEntry entry, int offset, int seekType ) {
        
        synchronized(entry) {
            int fileSize = fsize(entry);

            switch(seekType){
                case 0:{ //seek set
                    if (offset >= 0 && offset <= fileSize) {
                        entry.seekPtr = offset;
                        break;
                    }
                    return -1;
                }
                case 1:{ //seek cur
                    if (entry.seekPtr + offset >= 0 && entry.seekPtr + offset <= fileSize) {
                        entry.seekPtr += offset;
                        break;
                    }
                    return -1;
                }
                case 2:{ //seek end
                    entry.seekPtr = fileSize + offset;
                    break;
                }
            }
        }
        return entry.seekPtr;
    }
    

    
}
