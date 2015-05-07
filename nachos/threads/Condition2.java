package nachos.threads;

import nachos.machine.*;
import java.util.Random;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock the lock associated with this condition variable.
	 * The current thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
		waitQueue = ThreadedKernel.scheduler.newThreadQueue(false);
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean intStatus = Machine.interrupt().disable();
		
		conditionLock.release();
		if(D) System.out.println(KThread.currentThread().getName() + " is about to sleep");
		waitQueue.waitForAccess(KThread.currentThread()); //wait for access to this conditional variable
		KThread.sleep(); //relenquish CPU	
		if(D) System.out.println(KThread.currentThread().getName() + " is executing");

		conditionLock.acquire();
		Machine.interrupt().restore(intStatus); // enable interrupts
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean intStatus = Machine.interrupt().disable();
	        	
		KThread thread = waitQueue.nextThread();
		if (thread != null) {
			thread.ready();	//put sleeping thread on ready queue
			if(D) System.out.println(thread.getName() + " was just woken up by " + KThread.currentThread().getName() + " in " + getName());
		}
		else
		{
			//do nothing
		}
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		boolean intStatus = Machine.interrupt().disable();
		KThread thread = null;
		//put all sleeping threads on ready queue
		do
		{
			thread = waitQueue.nextThread();
			if(thread != null)
			{
				thread.ready();
				if(D) System.out.println(thread.getName() + " was just woken up by " + KThread.currentThread().getName());
			}
			else
			{
				//no more threads in thread queue
				//Therefore all threads sleeping on this condition have been woken
				break;
			}
		}
		while(thread != null);
	}

	//Simple join test using CV
	private static class Test1 implements Runnable
	{
		public Test1(Lock conditionLock, Condition2 cv)
		{
			this.conditionLock = conditionLock;
			this.cv = cv;
		}
		public void run()
		{
			System.out.println("Child is executing. Name = " + KThread.currentThread().getName());	
			conditionLock.acquire();
			done = true;		
			cv.wake();		//notify parent that child is done
			conditionLock.release();
		}	
		private Lock conditionLock;
		private Condition2 cv;
	}

	//Sleeper Thread - Used to sleep multiple threads on the same condition variable
	private static class Sleeper implements Runnable
	{
		public Sleeper(Lock conditionLock, Condition2 cv, Condition2 cv2)
		{
			this.conditionLock = conditionLock;
			this.cv = cv;
			this.cv2 = cv2;
		}

		public void run()
		{
			Random rand = new Random();
			if(rand.nextInt(10) > 4)
			{
				KThread.yield();	//add some randomness to thread execution
			}
			conditionLock.acquire();
			System.out.println("Entering thread " + KThread.currentThread().getName());
			numSleeping++;
			cv2.wake();	//notify parent that numSleeping has incremented
			cv.sleep();	//wait for wakeup
			numSleeping--;
			cv2.wake();	//notify parent that numSleeping has decremented
			System.out.println("Exiting thread " + KThread.currentThread().getName());
			conditionLock.release();
		}
		
		private Lock conditionLock;
		private Condition2 cv;
		private Condition2 cv2;
	}

	//Self test to see if this module is working correctly
	public static void selfTest()
	{
		System.out.println("Condition 2 Self Test ---------------------\n");
		unitTest1();
		unitTest2();
		System.out.println("End Condition 2 Self Test ---------------------\n");
	}

	//1 thread sleeps on CV and 1 thread wakes a CV
	private static void unitTest1()
	{
		System.out.println("unit test 1\n");
		Lock test1Lock = new Lock();
		Condition2 cv = new Condition2(test1Lock);
		new KThread(new Test1(test1Lock, cv)).setName("Condition2 Test1").fork();
		test1Lock.acquire();
		while(!done)
		{
			cv.sleep();
		}
		test1Lock.release();
		done = false;
		System.out.println("Ending unit test 1\n");
	}
	//Multiple thread sleeps on CV and 1 thread wakes all 
	private static void unitTest2()
	{	
		System.out.println("unit test 2\n");
		Lock test2Lock = new Lock();
		Condition2 cv = new Condition2(test2Lock);
		Condition2 cv2 = new Condition2(test2Lock);
		cv2.setName("cv2");
		int numThreads = 4;
		test2Lock.acquire();
		for(int i = 0; i < numThreads; i++)
		{
			new KThread(new Sleeper(test2Lock, cv, cv2)).setName("Sleeper" + i).fork();	//create threads
		}
		while(numSleeping != numThreads)
		{
			cv2.sleep();	//wait for all created threads to go to sleep
		}
		System.out.println("Waking all sleepers");
		cv.wakeAll();	//wake all sleepers
		while(numSleeping != 0)
		{
			cv2.sleep();	//wait for all sleepers to finish
		}
		test2Lock.release();
		numSleeping = 0;
		System.out.println("Ending unit test 2\n");
	}

	private String getName()
	{
		return name;
	}
	private void setName(String name)
	{
		this.name = name;
	}

	private Lock conditionLock;
	private ThreadQueue waitQueue;
	private static final boolean D = false;	//debug flag
	private static boolean done = false;	//shared variable used for testing
	private static int numSleeping = 0;	//shared variable used for testing
	private String name = "cv";	//used for debugging
}
