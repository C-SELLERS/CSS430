public class Inode {
    private final static int iNodeSize = 32;       // fix to 32 bytes
    private final static int directSize = 11;      // # direct pointers
 
    public int length;                             // file size in bytes
    public short count;                            // # file-table entries pointing to this
    public short flag;                             // 0 = unused, 1 = used, 2 = read, 3 = write
    public short direct[] = new short[directSize]; // direct pointers
    public short indirect;                         // a indirect pointer
 
    //Default constructor
    Inode( ) {                                     
       length = 0;
       count = 0;
       flag = 1;
       for ( int i = 0; i < directSize; i++ )
          direct[i] = -1;
       indirect = -1;
    }
 
    //Inode from the disk 
    Inode( short iNumber ) {   

        //Determine block number
        int blockNumber = 1 + iNumber / 16;

        //Load in the block
        byte data[] = new byte[512];						
        SysLib.rawread(blockNumber, data);

        //Determine offset in block
        int offset = (iNumber % 16) * iNodeSize;

        //Get information from block
        length = SysLib.bytes2int(data, offset);
        count = SysLib.bytes2short(data, offset += 4);												
        flag = SysLib.bytes2short(data, offset += 2);

        for(int i =0; i < directSize; i++) {
            direct[i] = SysLib.bytes2short(data, offset += 2);
        }

        indirect = SysLib.bytes2short(data, offset);
    }
 
    //Write the inode to the disk
    void toDisk( short iNumber ) {

        //Determine block number
        int blockNumber = (iNumber / 16) + 1;

        //Load in the block
        byte[] block = new byte[Disk.blockSize];
        SysLib.rawread(blockNumber,block);

        //Determine offset
        int offset = (iNumber % 16) * 32;

        //Write the information to the block
        SysLib.int2bytes( length, block, offset );
        SysLib.short2bytes( count, block, offset += 4 );
        SysLib.short2bytes( flag, block, offset += 2);

        for( int i = 0; i < directSize; i++ ) {
            SysLib.short2bytes( direct[i], block, offset += 2);
            
        }

        SysLib.short2bytes( indirect, block, offset += 2);

        //Write the updated block to the disk
        SysLib.rawwrite( blockNumber, block );
    }

    //Get the index block
    public short getIndexBlock() {
        return indirect;
    }

    //Set an index block***
    boolean setIndexBlock(short indexBlockNumber){

        //Check for invalid request with direct links
        for (int i = 0; i < directSize; ++i) {
            if (direct[i] == -1) {
                return false;
            }
        }

        //If there is not an index, set it with the given number
        if(indirect == -1) {
            indirect = indexBlockNumber;
            return true;
        } 

        //Otherwise return false
        return false;
     
    }

    //Free the index block up
    public byte[] freeIndexBlock( ) {

        //if indirect exits
        if(indirect >= 0) {

            //Read the data, set indirect to -1 and return the data
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread( indirect, data );
            indirect = -1;
            return data;

        } else {

            //Otherwise return nothing
            return null;
        }     
    }

    //Set the target block
    int setTargetBlock(int offset, short block){
        int blockNum = offset / 512;

   		//Check if its in the direct block
   		if (blockNum < 11){

   			//Block is already registered
   			if(direct[blockNum] != -1){
                return -1;
            }

            //Previous block is unused
            if (blockNum > 0 && direct[blockNum -1] == -1){
                return -2;
            }

            //It passed both checks so we can set the direct pointer
			direct[blockNum] = block;
			return blockNum;
		}
		// Check to see if its a indirect
		else if (indirect >= 0) {

            //Read the index block
			byte[] data = new byte[512];
            SysLib.rawread(indirect, data);
            
			//Write the indirect pointer
			int indirectNum = blockNum - 11;
			SysLib.short2bytes(block, data, indirectNum * 2);
            SysLib.rawwrite(indirect, data);
            
            //Success
			return blockNum;
        }
        
		//Indirect is null
		return -3;
    }


    public short findTargetBlock(int seekPtr) {

        //Determine block
        int block = seekPtr / Disk.blockSize;

        //If its direct, return the direct block pointer
        if(block < directSize){
            return direct[block];
        }

        //It must be an indirect
        //If indirect has a block
        if(indirect >= 0){

            //Load the block
            byte[] data = new byte[Disk.blockSize];
            SysLib.rawread(indirect, data);

            //Determine offset
            int offset = block - directSize;

            //Return the target
            return SysLib.bytes2short(data, offset * 2);
        } 

        //No block exists
        return -1;
    }
 }