/*
Colton Sellers
CSS 430 Assignment 1
Part 2: Shell.java
1/23/2020

A shell program to used by ThreadOS

*/

class Shell extends Thread{

    public void run(){
        boolean running = true;
        int shellCmdCount=1;
      
        //Start the shell loop
        while(running){
            SysLib.cout("shell[" + shellCmdCount + "]% ");

            StringBuffer buffer = new StringBuffer();
            SysLib.cin(buffer);

            String input = "";
            input = buffer.toString();

            if(input.length() != 0){
                shellCmdCount++;
                running = processInput(input);
            } 
        } 

        SysLib.exit();
    }
    
    //Takes input and processes it. Returns true if shell is still live.
    private boolean processInput(String input){
    
        //Split up the input and track where the current command begins and ends
        String[] argArray = SysLib.stringToArgs(input);
        int cmdStart = 0;
        int cmdEnd; 

        //Loop through the args to determine the next command
        for (int argIndex = 0; argIndex < argArray.length; argIndex++) {

            //If the current argument is the last argument in the array or signals execution then execute the command. 
            if (argIndex == argArray.length - 1 || argArray[argIndex].equals(";") || argArray[argIndex].equals("&")) {
                    
                    //Determine where the end of the command is.
                    if(argArray[argIndex].equals(";") || argArray[argIndex].equals("&")){
                        cmdEnd = argIndex - 1;
                    } else {
                        cmdEnd = argIndex;
                    }

                    //Get us our command
                    String[] commandArray = buildCommand(argArray, cmdStart, cmdEnd);

                    //If its an exit we need to get out of the shell
                    if (commandArray[0].equals("exit")) {
                        return false;
                    } 
                
                    //Execute and collect the thread ID.
                    int tid = SysLib.exec(commandArray);

                    //If the thread hasn't failed and the job isn't background wait for it to complete
                    if (tid != -1 && !argArray[argIndex].equals("&")){
                        while (SysLib.join() != tid); 
                    }
                
                //Update counter to the beginning of the next command
                cmdStart = argIndex + 1;
            } 
        }
        
        //Continue to take inputs
        return true;
    }
    
    //Builds the commands for execution from array. Returns the command in a string array
    private String[] buildCommand(String[] argArray, int commandStart, int commandFinish) {

        String[] command = new String[commandFinish - commandStart + 1];
        
        for (int index = commandStart; index <= commandFinish; ++index) {
            command[index - commandStart] = argArray[index];
        }

        return command;
    }

}