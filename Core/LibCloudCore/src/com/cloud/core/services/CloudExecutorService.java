package com.cloud.core.services;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.cloud.core.services.CloudExecutorService;
import com.cloud.core.services.CloudServices;
import com.cloud.core.concurrent.ScheduledExecutor;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * A cloud based Pool service. Use this to spawn threads or create timers. This service has 2 executors:
 * <ul>
 * <li>A scheduler: to spawn repetitive tasks using a thread pool (default pool size is 20).
 * <li>A cached thread executor to run unlimited # of threads.
 * </ul>
 * 
 * The executors are managed started/stopped by the {@link CloudServices} class.
 * 
 * @author VSilva
 * @version 1.0.2
 */
public class CloudExecutorService {

	private static final Logger log = LogManager.getLogger(CloudExecutorService.class);
	
	// 11/29/16 FindBugs Incorrect lazy initialization of static field com.cloud.core.services.CloudExecutorService.scheduler
	/** An executor to spawn runnables at given intervals */
	private static volatile ScheduledExecutor scheduler;
	
	/** A thread executor to run unlimited number of threads */
	private static volatile ExecutorService executor;
	
	/** Default pool size for the scheduler. Note: the executor has no limit */
	private static int DEFAULT_POOL_SIZE = 20;
	
	/** Init param: Scheduler Pool size */
	public static final String PARAM_POOL_SIZE = "PARAM_POOL_SIZE";

	/**
	 * Initialize the Cloud Executor service. Params:
	 * <li> DEFAULT_POOL_SIZE (default 20)
	 * @param params Service parameters.
	 */
	public static void initalize (Properties params) {
		try {
			DEFAULT_POOL_SIZE = params.containsKey(PARAM_POOL_SIZE) ? Integer.parseInt(params.getProperty(PARAM_POOL_SIZE)) : 20;
		} catch (Exception e) {
		}
	}

	/**
	 * Start the inner services. This sub can be called multiple times.
	 */
	public static void start () {
		if ( scheduler == null)	{
			log.debug("Start: Creating scheduled executor w/ pool size " + DEFAULT_POOL_SIZE);
			scheduler = new ScheduledExecutor(DEFAULT_POOL_SIZE, "CloudExecutorService");
		}
		if ( executor == null)	{
			log.debug("Start: Creating cached thread pool executor.");
			executor = Executors.newCachedThreadPool();
		}
	}

	/**
	 * Initiates an orderly shutdown in which previously submitted tasks are executed, but no new tasks will be accepted. 
	 * Invocation has no additional effect if already shut down. 
	 * <ul>
	 * <li>Repetitive Tasks: Blocks for 5s until all tasks have completed execution after a shutdown request, or the timeout occurs, or the current thread is interrupted, whichever happens first.
	 * <li>Thread Executor: Attempts to stop all actively executing tasks, halts the processing of waiting tasks, and returns a list of the tasks that were awaiting execution.
	 * This method does not wait for actively executing tasks to terminate.
	 * </ul>
	 */
	public static void shutdown () {
		if ( scheduler != null) {
			log.debug("Shutting down Cloud Executor repetive task scheduler.");
			scheduler.shutdown();
			scheduler = null;
		}
		if ( executor != null) {
			log.debug("Shutting down Cloud Executor thread executor.");
			executor.shutdownNow();
			executor = null;
		}
	}

	/**
	 * Stop the {@link CloudExecutorService}. This method should be invoked on container shutdown.
	 */
	public static void destroy () {
		shutdown();
	}
	
	/*
	 * These are deprecated. Forgot to return ScheduledFuture<?> 
	 */
	
//	/**
//	 * Schedule a timer.
//	 * @deprecated This method should return ScheduledFuture&lt;?>. Use scheduleWithFixedDelayA instead.
//	 * @param command
//	 * @param initialDelay
//	 * @param intervalMillis
//	 */
//	static public void scheduleWithFixedDelay(Runnable command, long initialDelay, long intervalMillis) {
//		if ( scheduler != null ) {
//			scheduler.scheduleWithFixedDelay(command, initialDelay, intervalMillis, TimeUnit.MILLISECONDS);
//		}
//	}
//
//	/**
//	 * Schedule a timer.
//	 * @deprecated This method should return ScheduledFuture&lt;?>. Use scheduleWithFixedDelayA instead.
//	 * @param command
//	 * @param initialDelay
//	 * @param interval
//	 * @param timeunit
//	 */
//	static public void scheduleWithFixedDelay(Runnable command, long initialDelay, long interval, TimeUnit timeunit) {
//		if ( scheduler != null ) {
//			scheduler.scheduleWithFixedDelay(command, initialDelay, interval, timeunit);
//		}
//	}
//
//	/**
//	 * Schedule a timer.
//	 * @deprecated This method should return ScheduledFuture&lt;?>. Use scheduleAtFixedRateA instead.
//	 * @param command
//	 * @param initialDelay
//	 * @param interval
//	 * @param timeunit
//	 */
//	static public void scheduleAtFixedRate(Runnable command, long initialDelay, long interval, TimeUnit timeunit) {
//		if ( scheduler != null ) {
//			scheduler.scheduleAtFixedRate(command, initialDelay, interval, timeunit);
//		}
//	}
//
//	/**
//	 * Schedule a timer.
//	 * @deprecated This method should return ScheduledFuture&lt;?>. Use scheduleAtFixedRateA instead.
//	 * @param command
//	 * @param initialDelay
//	 * @param intervalMS
//	 */
//	static public void scheduleAtFixedRate(Runnable command, long initialDelay, long intervalMS) {
//		if ( scheduler != null ) {
//			scheduler.scheduleAtFixedRate(command, initialDelay, intervalMS, TimeUnit.MILLISECONDS);
//		}
//	}
//
//	/**
//	 * Schedule a command @ a given delay.
//	 * @deprecated This method should return ScheduledFuture&lt;?>. Use scheduleA instead.
//	 * @param command Runnable.
//	 * @param delay Delay.
//	 * @param unit See {@link TimeUnit}
//	 */
//	static public void schedule(Runnable command, long delay, TimeUnit unit) {
//		if ( scheduler != null ) {
//			scheduler.schedule(command, delay,  unit);
//		}
//	}

	/*
	 * New ones returning ScheduledFuture<?> 
	 */
	
	/**
	 * Creates and executes a periodic action that becomes enabled first after the given initial delay, and subsequently with the given delay between the termination of one execution and the commencement of the next. If any execution of the task encounters an exception, subsequent executions are suppressed. Otherwise, the task will only terminate via cancellation or termination of the executor.
	 * @param command - the task to execute
	 * @param initialDelay - the time to delay first execution
	 * @param intervalMillis - the delay between the termination of one execution and the commencement of the next in ms.
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation.
	 */
	static public ScheduledFuture<?> scheduleWithFixedDelay (Runnable command, long initialDelay, long intervalMillis) {
		if ( scheduler == null ) return null;
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
	static public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long interval, TimeUnit timeunit) {
		if ( scheduler == null ) return null;
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
	static public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long interval, TimeUnit timeunit) {
		if ( scheduler == null ) return null;
		return scheduler.scheduleAtFixedRate(command, initialDelay, interval, timeunit);
	}

	/**
	 * Creates and executes a periodic action that becomes enabled first after the given initial delay, and subsequently with the given period; that is executions will commence after initialDelay then initialDelay+period, then initialDelay + 2 * period, and so on. If any execution of the task encounters an exception, subsequent executions are suppressed. Otherwise, the task will only terminate via cancellation or termination of the executor. If any execution of this task takes longer than its period, then subsequent executions may start late, but will not concurrently execute.
	 * @param command - the task to execute
	 * @param initialDelay - the time to delay first execution
	 * @param intervalMS - the delay between the termination of one execution and the commencement of the next in ms.
	 * @return a ScheduledFuture representing pending completion of the task, and whose get() method will throw an exception upon cancellation.
	 */
	static public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long intervalMS) {
		if ( scheduler == null ) return null;
		return scheduler.scheduleAtFixedRate(command, initialDelay, intervalMS, TimeUnit.MILLISECONDS);
	}

	/**
	 * Creates and executes a one-shot action that becomes enabled after the given delay.
	 * @param command - the task to execute
	 * @param delay - the time from now to delay execution
	 * @param unit unit - the time unit of the delay parameter 
	 * @return a ScheduledFuture representing pending completion of the task and whose get() method will return null upon completion 
	 */
	static public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		if ( scheduler == null ) return null;
		return scheduler.schedule(command, delay,  unit);
	}
	
	/**
	 * Run a command.
	 * @param command A {@link Runnable}.
	 */
	static public void execute (Runnable command) {
		if ( executor != null ) {
			executor.execute(command);
		}
	}
	
}

