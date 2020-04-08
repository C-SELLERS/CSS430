public class Directory {


    //Define some constants
    private static final int CHAR_SIZE = 2; //Char = 2 bytes
    private static final int INT_SIZE = 4; //Int = 4 bytes
    private static int maxChars = 30; //Max file size name
    private int fsizes[];
    private char fnames[][];

    public Directory(int maxInumber) {
        
        //Create file sizes array and set all to 0
        fsizes = new int[maxInumber];
        for(int i = 0; i < maxInumber; i++){
            fsizes[i] = 0;
        }
        
        //File names array
        fnames = new char[maxInumber][maxChars];

        String root = "/";
        fsizes[0] = root.length();

        root.getChars(0, fsizes[0], fnames[0], 0);
    }

    // assumes data[] received directory information from disk
    // initializes the Directory instance with this data[]
    public void bytes2directory(byte data[]) {

        //Keep track of where we are
        int offset = 0;

		//Get all the file sizes
		for (int i = 0; i < fsizes.length; i++) {
			fsizes[i] = SysLib.bytes2int(data, offset);
			offset += 4;
		}

		//Store all the file names
		for (int i = 0; i < fnames.length; i++){
			String fileName = new String(data, offset, maxChars * 2);
            fileName.getChars(0, fsizes[i], fnames[i], 0);
			offset += maxChars * 2;
		}
    }

    // converts and return Directory information into a plain byte array
    // this byte array will be written back to disk
    // note: only meaningfull directory information should be converted
    // into bytes.
    public byte[] directory2bytes() {

        //Create a data buffer to return
        byte[] data = new byte[(fsizes.length * INT_SIZE) + (fnames.length * CHAR_SIZE * maxChars)];

        //Keep track of where we are at
        int offset = 0;

        //Write all the sizes to the data buffer
        for(int i = 0; i < fsizes.length; i++){
            SysLib.int2bytes(fsizes[i], data, offset);
            offset += INT_SIZE;
        }

        //Write all file names to the data buffer
        for(int i = 0; i < fnames.length; i++){
            String fileName = new String(fnames[i], 0, fsizes[i]);
            byte bytes[] = fileName.getBytes();
            System.arraycopy(bytes, 0, data, offset, bytes.length);
			offset += maxChars * CHAR_SIZE;
        }

        //Return the directory in byte form
        return data;
    }

    // filename is the one of a file to be created.
    // allocates a new inode number for this filename
    public short ialloc(String fileName){

        //Look for an entry in file sizes
        for(short i = 0; i < fsizes.length; i++) {

            //If this is an empty entry, use it
            if(fsizes[i] == 0){

                //Set the file size and place the name in fnames
                //File size is either the length or maxChars is greater than maxChars
                fsizes[i] = fileName.length() < maxChars ? (short)fileName.length() : maxChars;
                fileName.getChars(0, fsizes[i], fnames[i], 0);
                return i;
            }
        }

        return -1;
    }

    // deallocates this inumber (inode number)
    // the corresponding file will be deleted.
    public boolean ifree(short iNumber){

        //If there is nothing there it an invalid request
        if(fsizes[iNumber] == 0){
            return false;
        }

        //Deallocate it and return true
        fsizes[iNumber] = 0;
        return true;
    }

    // returns the inumber corresponding to this filename
    public short namei(String fName){

        //Iterate through the files to find it
        for(short i = 0; i < fsizes.length; i++) {

            //Get the file name, if it matches return its location
            String current = new String(fnames[i], 0, fsizes[i]);
            if(current.equals(fName)){
                return i;
            }
        }

        //Wasn't found, return -1
        return -1;
    }


}