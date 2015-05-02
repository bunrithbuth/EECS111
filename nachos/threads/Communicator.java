package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		lockListen = new Lock();
		lockSpeak = new Lock();
		lockMsg = new Lock();
		hasRead = false;
		hasSpoken = false;
		cvHasRead = new Condition(lockMsg);
		cvHasSpoken = new Condition(lockMsg);
		message = -1;
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word the integer to transfer.
	 */
	public void speak(int word) {
		//Acquire speaker lock and message lock
		lockSpeak.acquire();	//mutual exclusion: only 1 speaker can be speaking
		lockMsg.acquire();	//mutual exclusion: You can read or write to message but not both at the same time
		if(D) System.out.println("Inside speak(). About to modify message to " + word);
		//modify message and notify listeners
		message = word;
		hasSpoken = true;
		cvHasSpoken.wake();
		//wait for acknowledgement
		while(!hasRead)
		{
			if(D) System.out.println("Waiting for acknowledgement");
			cvHasRead.sleep();
		}
		//release locks and reset acknowledgement flag
		hasRead = false;
		lockMsg.release();
		lockSpeak.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		lockListen.acquire();	//mutual exclusion: only 1 listener can be listening
		lockMsg.acquire();	//mututal exclusion: You can read or write a message but not both at the same time
		//wait for speaker to speak;
		while(!hasSpoken)
		{
			cvHasSpoken.sleep();
		}
		int word = message;	//read message
		hasSpoken = false;	//reset flag
		//notify speakers that message has been read
		hasRead = true;		
		cvHasRead.wake();
		//release locks
		lockMsg.release();
		lockListen.release();
		return word;	
	}

	private static class Listener implements Runnable
	{
		public Listener(Communicator comm, int id)
		{
			this.comm = comm;
			this.id = id;
		}

		public void run()
		{
			int msg = comm.listen();
			System.out.printf("Listener %d received the number %d\n", id, msg);
		}
		private Communicator comm;
		private int id;
	}	
	
	private static class Speaker implements Runnable
	{
		public Speaker(Communicator comm, int id, int msg)
		{
			this.comm = comm;
			this.id = id;
			this.msg = msg;
		}

		public void run()
		{
			System.out.printf("Speaker %d sending the number %d\n", id, msg);
			comm.speak(msg);
		}
		private Communicator comm;
		private int id;
		private int msg;
	}	

	public static void selfTest()
	{
		if(D)	System.out.println("Communicator Self Test -------------- ");
		Communicator comm = new Communicator();	
		for(int i = 1; i < 3; i++)
		{
			if(D) System.out.println("creating speakers......");
			new KThread(new Speaker(comm, i, i)).setName("Speaker " + (i)).fork();	
		}
		for(int i = 1; i < 3; i++)
		{
			System.out.println("Creating listeners.......");
			new KThread(new Listener(comm, i)).setName("Listener " + (i)).fork();	
		}
		for(int i = 0; i < 10; i++)
		{
			KThread.yield();
		}		
	}


	private Lock lockListen;		// Lock for listeners
	private Lock lockSpeak;			// Lock for Speakers
	private Lock lockMsg;			// Lock to read or modify the message
	private int message;
	private boolean hasRead;		//Acknowledgement - flag to notify speaker that message has been read
	private Condition cvHasRead;		//C.V. for boolean condition hasRead
	private Condition cvHasSpoken;	 	//C.V. for boolean condition hasSpoken
	private boolean hasSpoken;		//determines whether a speaker in this communicator has spoken
	private static final boolean D = true;  //debug flag	
}
