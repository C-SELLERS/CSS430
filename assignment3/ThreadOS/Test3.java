import java.util.Date;

class Test3 extends Thread{

	private int threadsToRun;
	
	//Argument for threadsToRun
	public Test3(String[] args){
		threadsToRun = Integer.parseInt(args[0]);
	} 
	

	public void run(){
		
		//Initiate compute and disk thread arguement array
		String[] testThread3A = SysLib.stringToArgs("TestThread3A");
		String[] testThread3B = SysLib.stringToArgs("TestThread3B");
		
		//Time 1 Starting
		long time1 = new Date().getTime(); 

		//run both threads the set amount
		for(int i = 0; i < threadsToRun; i++){
			SysLib.exec(testThread3A);
			SysLib.exec(testThread3B);
		}
		
		//wait for them to finish 
		for(int i = 0; i < 2*threadsToRun; i++){
			SysLib.join();
		}
		
		//Time 2 Ending
		long time2 = new Date().getTime(); // get running time
		
		//Print the elapsed time and exit
		SysLib.cout("Total time elapse: " + (time2 - time1) + "ms" + "\n");
		SysLib.exit(); 
	}
}