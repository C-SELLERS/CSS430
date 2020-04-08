import java.util.Vector;

class QueueNode {
    private Vector<Integer> vector;

    //Create the QueueNode
    public QueueNode() {
        vector = new Vector<Integer>(); //Contains the waiting threads
    }

    //Put the thread to sleep
    public synchronized int sleep() {
        if (vector.size() == 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("Thread unable to sleep");
            }
            //return the thread id when removing it from the queue
            return vector.remove(0);
        }

        return -1;
    }

    //Wake the thread
    public synchronized void wakeup(int tid) {
        vector.add(tid);    //add it to the vector
        notifyAll();        //wake up the parent
    }
}