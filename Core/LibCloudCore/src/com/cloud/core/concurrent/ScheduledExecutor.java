package com.cloud.core.concurrent;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.cloud.core.concurrent.ThreadFactoryBuilder;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * A tool to run repeated tasks in the background. This class replaces the java Timer.
 * Timers must NOT be used. If the server dies/crashes or goes to sleep any running tasks won't
 * terminate gracefully. Some features:
 * 
 * <ul>
 * <li> This class will wait for any running tasks and then shutdown gracefully.
 * <li> Thread pools are named (very helpful when profiling)
 * <li> Use it instead of timers.
 * </ul>
 * 
 * <h2>Change Log</h2>
 * <ul>
 * <li> 2/24/2017 scheduler member changed to {@link ScheduledThreadPoolExecutor} + added an {@link RejectedExecutionHandler} that logs an {@link RejectedExecutionException} in case of failures.
 * </ul>
 * @version 1.0.1
 * @author vsilva
 *
 */
public class ScheduledExecutor {

	private static final Logger log = LogManager.getLogger(ScheduledExecutor.class);
	
	private /*ScheduledExecutorService*/ ScheduledThreadPoolExecutor scheduler;

	/**
	 * Creates a thread pool that can schedule commands to run after a given delay, or to execute periodically.
	 * @param namePrefix - Name prefix used to id threads in this pool. Very helpful for profiling.
	 */
	public ScheduledExecutor(String namePrefix) {
		this(1, namePrefix);
	}
	
	/**
	 * Creates a thread pool that can schedule commands to run after a given delay, or to execute periodically.
	 * @param poolSize - the number of threads to keep in the pool, even if they are idle.
	 * @param namePrefix - Name prefix used to id threads in this pool. Very helpful for profiling.
	 */
	public ScheduledExecutor(int poolSize, String namePrefix) {
		ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(namePrefix + "-%d").build();
		//scheduler = Executors.newScheduledThreadPool(poolSize, namedThreadFactory);
		scheduler = new ScheduledThreadPoolExecutor (poolSize, namedThreadFactory, new RejectedExecutionHandler() {
			
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				int active 		= executor.getActiveCount();
				int coreSize	= executor.getCorePoolSize();
				int poolSize	= executor.getPoolSize();
				boolean isDown	= executor.isShutdown();
				boolean isTerm	= executor.isTerminated();
				
				RejectedExecutionException ex = new RejectedExecutionException("Cloud Executor service cannot execute task. Active Count: " 
						+ active + " Core Pool Size: " + coreSize + " Pool Size: " + poolSize
						+ " IsShutDown: " + isDown + " IsTErminated: " + isTerm);
				log.error("Rejected Execution", ex);
			}
		});
	}
	
	/**
	 * Creates and executes a periodic action that becomes enabled first after the given initial delay, and subsequently with the given delay between the termination of one execution and the commencement of the next. If any execution of the task encounters an exception, subsequent executions are suppressed. Otherwise, the task will only terminate via cancellation or termination of the executor.
	 * @param command - the task to execute
	 * @param initialDelay - the time to delay first execution
	 * @param intervalMillis - the delay between the termination of one execution and the commencement of the next
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation.
	 */
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long intervalMillis) {
		return scheduler.scheduleWithFixedDelay(command, initialDelay, intervalMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * Creates and executes a periodic action that becomes enabled first after the given initial delay, and subsequently with the given delay between the termination of one execution and the commencement of the next. If any execution of the task encounters an exception, subsequent executions are suppressed. Otherwise, the task will only terminate via cancellation or termination of the executor.
	 * @param command - the task to execute
	 * @param initialDelay - the time to delay first execution
	 * @param interval - the delay between the termination of one execution and the commencement of the next
	 * @param timeunit - the time unit of the initialDelay and delay parameters 
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation.
	 */
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long interval, TimeUnit timeunit) {
		return scheduler.scheduleWithFixedDelay(command, initialDelay, interval, timeunit);
	}

	/**
	 * Creates and executes a periodic action that becomes enabled first after the given initial delay, and subsequently with the given period; that is executions will commence after initialDelay then initialDelay+period, then initialDelay + 2 * period, and so on. If any execution of the task encounters an exception, subsequent executions are suppressed. Otherwise, the task will only terminate via cancellation or termination of the executor. If any execution of this task takes longer than its period, then subsequent executions may start late, but will not concurrently execute.
	 * @param command - the task to execute
	 * @param initialDelay - the time to delay first execution
	 * @param interval - the delay between the termination of one execution and the commencement of the next
	 * @param timeunit - the time unit of the initialDelay and delay parameters 
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation.
	 */
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long interval, TimeUnit timeunit) {
		return scheduler.scheduleAtFixedRate(command, initialDelay, interval, timeunit);
	}

	/**
	 * Creates and executes a periodic action that becomes enabled first after the given initial delay, and subsequently with the given period; that is executions will commence after initialDelay then initialDelay+period, then initialDelay + 2 * period, and so on. If any execution of the task encounters an exception, subsequent executions are suppressed. Otherwise, the task will only terminate via cancellation or termination of the executor. If any execution of this task takes longer than its period, then subsequent executions may start late, but will not concurrently execute.
	 * @param command - the task to execute
	 * @param initialDelay - the time to delay first execution
	 * @param intervalMS - the delay between the termination of one execution and the commencement of the next
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation.
	 */
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long intervalMS) {
		return scheduler.scheduleAtFixedRate(command, initialDelay, intervalMS, TimeUnit.MILLISECONDS);
	}

	/**
	 * Creates and executes a one-shot action that becomes enabled after the given delay.
	 * @param command - the task to execute
	 * @param delay - the time from now to delay execution
	 * @param unit unit - the time unit of the delay parameter 
	 * @return a ScheduledFuture representing pending completion of the task and whose get() method will return null upon completion 
	 */
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		return scheduler.schedule(command, delay,  unit);
	}

	/**
	 * Attempts to stop all actively executing tasks, halts the processing of waiting tasks, and returns a list of the tasks that were awaiting execution. 
	 * This method does not wait for actively executing tasks to terminate. Use awaitTermination to do that. 
	 * There are no guarantees beyond best-effort attempts to stop processing actively executing tasks. For example, typical implementations will cancel via Thread.interrupt(), so any task that fails to respond to interrupts may never terminate.
	 * @return list of tasks that never commenced execution 
	 */
	public List<Runnable> shutdownNow () {
		return scheduler.shutdownNow();
	}
	
	public void execute (Runnable command) {
		scheduler.execute(command);
	}
	
	/**
	 * Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted. 
	 * Invocation has no additional effect if already shut down. 
	 * Blocks for 5s until all tasks have completed execution after a shutdown request, or the timeout occurs, or the current thread is interrupted, whichever happens first.
	 */
	public void shutdown () {
		shutdown(5000);
	}
	
	/**
	 * Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted. 
	 * Invocation has no additional effect if already shut down. 
	 * Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs, or the current thread is interrupted, whichever happens first.
	 * @param timeout Time to wait in milli seconds for tasks to terminate.
	 */
	public void shutdown (long timeout) {
		try {
			// graceful shutdown. It'll wait for any running tasks
			/**
			 * java.lang.NullPointerException
			 * 	at com.cloud.core.concurrent.ScheduledExecutor.shutdown(ScheduledExecutor.java:141)
			 */
			if  ( scheduler != null) {
				scheduler.shutdown();
				scheduler.awaitTermination(timeout, TimeUnit.MILLISECONDS);
				scheduler.shutdownNow();
			}
			scheduler = null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("Shutdown with timeout " + timeout, e);
		}
	}
}
