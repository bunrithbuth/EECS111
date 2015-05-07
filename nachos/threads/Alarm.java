package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;
/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    private static LinkedList Queue;
    private static long[] wakeTime = new long[10];
    private static int count1 = 0;
    private static int count2 = 0;
    private static int head = 0;
    
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
        while(count2 > 0 && wakeTime[head] >= Machine.timer().getTime())
        {
            boolean status = Machine.interrupt().disable();
            ((KThread) Queue.removeFirst()).ready();
            Machine.interrupt().restore(status);
            count2--;
            head = head++ % 10;
        }
        KThread.currentThread().yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		//long wakeTime = Machine.timer().getTime() + x;
		//while (wakeTime > Machine.timer().getTime())
		//	KThread.yield();
        
	   if(x ==0)
	   {
	       return;
	    }
	    if(count2 >= 10)
	    {
	        return;
	    }
        
	 wakeTime[count1] = Machine.timer().getTime() + x;
	 count1 = count1++ %10;
        count2++;
        Queue.add(KThread.currentThread());
        Machine.interrupt().disable();
        KThread.currentThread().sleep();
	}
    
}
