/*
Colton Sellers
CSS 430 Assignment 5
*/

public class SuperBlock {
    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList;    // the block number of the free list's head
    
    public SuperBlock(int diskSize) {

        //Byte array to read the first disk block
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread(0, superBlock);

        //Set our three variables from the block
        totalBlocks = SysLib.bytes2int(superBlock, 0);
        totalInodes = SysLib.bytes2int(superBlock, 4);
        freeList = SysLib.bytes2int(superBlock,8);

        //Check to see if we need to format the disk
        if(totalBlocks != diskSize && totalInodes < 1 && freeList < 2){
            totalBlocks = diskSize;
            format(64);
        }
        
        
    }

    //Format the disk to hold files
    public void format(int files) {

        //Inodes now equals the max number of files
        totalInodes = files;
        
        //Loop through and intialize the inodes
        for (short i = 0; i < totalInodes; i++) {
            Inode newInode = new Inode(); 
            newInode.toDisk(i);    
        }
        
        //Determine freelist size
        freeList = 2 + totalInodes * 32 / Disk.blockSize;
        

        //For each block in the freelist create a block and empty the block
        for (int i = freeList; i < totalBlocks; i++) {
            byte[] block = new byte[Disk.blockSize];
            
            for (int j = 0; j < Disk.blockSize; j++){
                block[j] = 0;
            }
            
            SysLib.int2bytes(i + 1, block, 0);
            SysLib.rawwrite(i, block);
        }
        
        //Sync now that format is complete
        sync();
    }

    //Sync the modified superblock
    public void sync(){
        byte[] block = new byte[512];
        SysLib.int2bytes(totalBlocks, block, 0);
        SysLib.int2bytes(totalInodes, block, 4);
        SysLib.int2bytes(freeList, block, 8);
        SysLib.rawwrite(0, block);
    }
    

    //Method to find a free block for data
    public int getFreeBlock(){
        
        if(freeList == -1){
            return -1;
        }

        //Retrieve the free block 
        byte[] data = new byte[512];
        SysLib.rawread(freeList, data);

        
        //Update the freelist
        int freeBlock = freeList;
        freeList = SysLib.bytes2int(data, 0);
        SysLib.int2bytes(0,data,0);
        SysLib.rawwrite(freeBlock,data);

        return freeBlock;
    }
    
    //Give a block back and empty it
    public boolean returnBlock(int blockNumber){

        //Not a block that can be returned
        if (blockNumber < 0){
            return false;
        }
        
        //Create an empty data block
        byte data[] = new byte[512];
        for (int i = 0; i < 512; i++){
            data[i] = 0;
        }
        
        //Write empty data to that block and then update freelist
        SysLib.rawwrite(blockNumber, data);
        SysLib.int2bytes(freeList, data, 0);
        freeList = blockNumber;
        
        return true;
    }
 }