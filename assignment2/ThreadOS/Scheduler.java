/*
Colton Sellers
CSS 430
Multilevel Feedback Queue Scheduler for ThreadOS

*/

import java.util.*;

public class Scheduler extends Thread
{
    //Create the three queues and quantum
    private ArrayList<Vector<TCB>> queuesArray = new ArrayList<Vector<TCB>>();
    private int timeSlice;
    private static final int quantumDefault = 500;

    // New data added to p161 
    private boolean[] tids; // Indicate which ids have been used
    private static final int DEFAULT_MAX_THREADS = 10000;

    // A new feature added to p161 
    // Allocate an ID array, each element indicating if that id has been used
    private int nextId = 0;
    private void initTid( int maxThreads ) {
        tids = new boolean[maxThreads];
        for ( int i = 0; i < maxThreads; i++ )
            tids[i] = false;
    }

    // A new feature added to p161 
    // Search an available thread ID and provide a new thread with this ID
    private int getNewTid( ) {
        for ( int i = 0; i < tids.length; i++ ) {
            int tentative = ( nextId + i ) % tids.length;
            if ( tids[tentative] == false ) {
                tids[tentative] = true;
                nextId = ( tentative + 1 ) % tids.length;
                return tentative;
            }
        }
        return -1;
    }

    // A new feature added to p161 
    // Return the thread ID and set the corresponding tids element to be unused
    private boolean returnTid( int tid ) {
        if ( tid >= 0 && tid < tids.length && tids[tid] == true ) {
            tids[tid] = false;
            return true;
        }
        return false;
    }

    // A new feature added to p161 
    // Retrieve the current thread's TCB from the queue pool
    public TCB getMyTcb( ) {
        Thread myThread = Thread.currentThread( ); // Get my thread object
        for(Vector<TCB> queue : queuesArray){ 
            synchronized( queue ) {
                for ( int j = 0; j < queue.size( ); j++ ) {
                    TCB tcb = ( TCB )queue.elementAt(j);
                    Thread thread = tcb.getThread( );
                    if ( thread == myThread ) // if this is my TCB, return it
                        return tcb;
                }
            }
        }
        return null;
    }


    // A new feature added to p161 
    // Return the maximal number of threads to be spawned in the system
    public int getMaxThreads( ) {
	    return tids.length;
    }

    public Scheduler( ) {
        timeSlice = quantumDefault;
        for (int i=0 ; i<3; i++){
            queuesArray.add(new Vector<TCB>( ));
        }
        initTid( DEFAULT_MAX_THREADS );
    }

    public Scheduler( int quantum ) {
        timeSlice = quantum;
        for (int i=0 ; i<3; i++){
            queuesArray.add(new Vector<TCB>( ));
        }
        initTid( DEFAULT_MAX_THREADS );
    }

    // A new feature added to p161 
    // A constructor to receive the max number of threads to be spawned
    public Scheduler( int quantum, int maxThreads ) {
        timeSlice = quantum;
        for (int i=0 ; i<3; i++){
            queuesArray.add(new Vector<TCB>( ));
        }
        initTid( maxThreads );
    }

    private void schedulerSleep(int quantum) {
        try {
            Thread.sleep(quantum);
        } catch ( InterruptedException e ) {
        }
    }

    // A modified addThread of p161 example
    public TCB addThread( Thread t ) {
        TCB parentTcb = getMyTcb( ); // get my TCB and find my TID
        int pid = ( parentTcb != null ) ? parentTcb.getTid( ) : -1;
        int tid = getNewTid( ); // get a new TID
        if ( tid == -1)
            return null;
        TCB tcb = new TCB( t, tid, pid ); // create a new TCB
        
        queuesArray.get(0).add( tcb ); //add to first queue
        return tcb;
    }

    // A new feature added to p161
    // Removing the TCB of a terminating thread
    public boolean deleteThread( ) {
        TCB tcb = getMyTcb( ); 
        if ( tcb!= null )
            return tcb.setTerminated( );
        else
            return false;
    }

    public void sleepThread( int milliseconds ) {
        try {
            sleep( milliseconds );
        } catch ( InterruptedException e ) { }
    }
    
    // A modified run of p161
    public void run( ) {
        while ( true ) {
            try {
                for(int queueNum = 0; queueNum < 3; queueNum++){
                    runQueue(queueNum);
                }
            } catch ( NullPointerException e3 ) { };
        }
    }

    // Algorithm for running queues, varies slightly per queue
    public void runQueue(int queueNum){
        Thread current = null;
        TCB currentTCB;
        Vector<TCB> queue = queuesArray.get(queueNum);

        while (queue.size( ) > 0){

            //Get the thread
            synchronized(queue){
                currentTCB = (TCB)queue.firstElement( );
            }
            
            //If thread is done remove it from queue
            if ( currentTCB.getTerminated( )) {
                queue.remove( currentTCB );
                returnTid( currentTCB.getTid( ) );
                continue;
            }

            //Get the thread, and get it running
            current = currentTCB.getThread( );
            if ( current != null ) {
                if ( current.isAlive( ) ) {
                    current.resume();
                } else {
                    current.start();
                }
            }
            
            //Queue dependent execution Algorithm
            switch(queueNum){

                //run till quantum
                case 0: 
                    schedulerSleep(timeSlice);
                    break;

                //run quantum, check for anything in queue 0
                //if so go run that queue first and then come back
                //and continue running for another quantum
                case 1: 
                    schedulerSleep(timeSlice);
                    if(queuesArray.get(0).size() > 0)
                        current.suspend();
                        runQueue(0);
                        current.resume();
                    schedulerSleep(timeSlice);
                    break;
                
                //run quantum, check for anything in queue 0 or 1
                //if so go run those queues first and then come back
                //do this till q2's full quantum (2000ms)
                case 2:
                    //Run three times to get to 1500ms quantum
                    for (int i = 0; i < 3; i++){
                        schedulerSleep(timeSlice);
                        if(queuesArray.get(0).size() > 0 || queuesArray.get(1).size() > 0)
                            current.suspend();
                            runQueue(0);
                            runQueue(1);
                            current.resume();
                    }
                    schedulerSleep(timeSlice);
                    break;
            }

            //Move the TCB to the next appropriate queue, unless already in Q2 just rotate.
            synchronized ( queue ) {
                if ( current != null && current.isAlive( ) ){
                    current.suspend();
                }
                
                switch(queueNum){
                  case 0:
                     queuesArray.get(0).remove( currentTCB );
                     queuesArray.get(1).add( currentTCB );
                     break;
                  case 1:
                     queuesArray.get(1).remove( currentTCB );
                     queuesArray.get(2).add( currentTCB );
                     break;
                  case 2:
                     queuesArray.get(2).remove( currentTCB );
                     queuesArray.get(2).add( currentTCB );
                     break;
                }

            }
        }



    }
}
