/*
Colton Sellers
CSS 430 Assignment 1
Part 1: processes.cpp
1/20/2020

A program meant to demonstrate system calls and piping through C++

*/


#include <iostream>    //for cout, endl
#include <unistd.h>    //for fork, pipe
#include <stdlib.h>    //for exit
#include <sys/wait.h>  //for wait

using namespace std;

int main(int argc, char* argv[]) {
    enum {Read, Write}; //Give file descriptors values, Read=0, Write=1
    int filedes1[2], filedes2[2]; //Pipe file descriptors

    if(pipe(filedes1) < 0 || pipe(filedes2) < 0){ //create the pipes
        cerr << "Pipe Error" << endl;
    }

    switch(fork()){
        case 0: //this is the child -> wc-l
            close(filedes2[Write]);
            close(filedes1[Write]);

            //read from pipe 2
            dup2(filedes2[Read],0); 

            close(filedes2[Read]);

            execlp("wc", "wc", "-l", NULL);
            break;
        

        default:{ //parent process
            switch(fork()){
                case 0:  //this is the grandchild -> grep
                    close(filedes2[Read]);
                    close(filedes1[Write]);

                    //read from pipe 1 write to pipe 2
                    dup2(filedes1[Read],0);
                    dup2(filedes2[Write],1);

                    close(filedes1[Read]);
                    close(filedes2[Write]);

                    execlp("grep", "grep", argv[1], NULL);
                    break;
                

                default: //parent process
                    switch(fork()){
                        case 0: //this is the great grandchild -> ps -A
                            close(filedes1[Read]);
                            close(filedes2[Write]);

                            //write to pipe 1
                            dup2(filedes1[Write],1);

                            close(filedes1[Write]);

                            execlp("ps", "ps", "-A", NULL);
                            break;
                        
                        default: //parent process
                            wait(NULL);
                            exit(EXIT_SUCCESS);
                            break;
                    }
                    break;
            }
            break;
        }
    }

}