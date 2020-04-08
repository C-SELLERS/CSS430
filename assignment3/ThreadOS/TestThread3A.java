//TestThread3B
//Used by Test3 as a computation thread

class TestThread3A extends Thread{
	
	//Run the computation and exit
	public void run(){
		 computation(5);
		 SysLib.cout("comp finished...\n");
		 SysLib.exit();
	}
	
	//Recursive computation, always O(n!)
	public void computation(int n){
		if (n <= 0){
			return;
		}
		for(int i = 0; i < n; i++){
			computation(n - 1);
		}
	}
}
