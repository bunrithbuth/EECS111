package nachos.threads;

import nachos.machine.*;

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
			thread.ready();
			if(D) System.out.println(thread.getName() + " just woke up");
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
		do
		{
			thread = waitQueue.nextThread();
			if(thread != null)
			{
				thread.ready();
				if(D) System.out.println(thread.getName() + "just woke up");
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

	private static class Test1 implements Runnable
	{
		public Test1(Lock conditionLock, Condition2 cv)
		{
			this.conditionLock = conditionLock;
			this.cv = cv;
		}
		public void run()
		{
			if(D) System.out.println("Child is executing. Name = " + KThread.currentThread().getName());	
			conditionLock.acquire();
			done = true;
			cv.wake();
			conditionLock.release();
		}	
		private Lock conditionLock;
		private Condition2 cv;
	}

	public static void selfTest()
	{
		if(D) System.out.println("Condition 2 Self Test ---------------------");
		Lock test1Lock = new Lock();
		Condition2 cv = new Condition2(test1Lock);
		new KThread(new Test1(test1Lock, cv)).setName("Condition2 Test1").fork();
		test1Lock.acquire();
		while(!done)
		{
			cv.sleep();
		}
		test1Lock.release();
	}	

	private Lock conditionLock;
	private ThreadQueue waitQueue = ThreadedKernel.scheduler
			.newThreadQueue(false);
	private static final boolean D = true;
	private static boolean done = false;
}
