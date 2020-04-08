public class SyncQueue {

    //Array  of QueueNodes
    private QueueNode [] queue;

    //Default constructor, load the queue array with 10 QueueNodes
    public SyncQueue(){
        queue = new QueueNode[10];
        for (int i = 0 ; i < 10 ; i++){
            queue[i] = new QueueNode();
        }
    }
        
    //Constructor, load the queue array with given number QueueNodes
    public SyncQueue(int max){
        queue = new QueueNode[max];
        for (int i = 0 ; i < max ; i++){
            queue[i] = new QueueNode();
        }
    }
        
    // Enqueue the thread until its condition is met, return the tid
    public int enqueueAndSleep(int cond) {
        int tid = -1; //default if it is a bad call to this function
        if (cond > -1 && cond < queue.length) {
            tid = queue[cond].sleep();
        }
        return tid;

    }
        
    // Dequeue and wake up the default thread (0)
    public void dequeueAndWakeup(int cond){
        if (cond > -1 && cond < queue.length){
            queue[cond].wakeup(0);
        }
    }
        
    // Dequeue and wake up the specific thread
    public void dequeueAndWakeup(int cond, int tid){
        if (cond > -1 && cond < queue.length){
            queue[cond].wakeup(tid);
        }

    }
}
