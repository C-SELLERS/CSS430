//TestThread3B
//Used by Test3 as a disk writing and reading thread.

class TestThread3B extends Thread{
	
	//A block of data to write to disk
	byte[] buffer = new byte[512];
	
	//write, read and exit
	public void run(){
		for(int i = 0; i < 1000; i++){
			SysLib.rawwrite(i,buffer);
			SysLib.rawread(i,buffer);
		}
		SysLib.cout("disk finished...\n");
		SysLib.exit();	
	}
}
