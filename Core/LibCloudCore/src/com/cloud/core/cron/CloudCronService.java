package com.cloud.core.cron;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.cloud.core.cron.CronTask;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.profiler.LicenseMetrics;

/**
 * A work in progress Cron-service. Capable of:
 * <ul>
 * <li> Daily {@link Runnable} tasks @ a given HH:mm.
 * </ul>
 * 
 * In the future this service will provide full cron-like functionality.
 * 
 * @author VSilva
 * @version 1.0.0 - Initial implementation. A work in progress.
 *
 */
public class CloudCronService {

	private static final Logger log 			= LogManager.getLogger(CloudCronService.class);
	
	private static final List<CronTask> tasks 	= new ArrayList<CronTask>();
	
	/**
	 * Initialize. Invoke once when the container initializes.
	 * By default it creates a default {@link LicenseMetrics} disk save {@link CronTask} that runs @ EOB 18:00 every day.
	 */
	public static synchronized void initialize () {
		try {
			// daily @ EOB 18:00
			tasks.add(new CronTask("LICENSE-METRICS-SAVE-TASK", 18, 00, LicenseMetrics.getInstance().SAVE_TO_DISK_TASK));
		} catch (ParseException e) {
			log.error("Cloud CronService initialize", e);		
		}
	}
	
	/**
	 * Destroy. Invoke once when the container shuts down.
	 */
	public static synchronized void destroy () {
		tasks.clear();
	}
	
	/**
	 * Execute a tick in the service. Invoke @ a regular interval ~1/second.
	 */
	public static synchronized void run () {
		Date now = new Date();

		// 2/6/2020 Reset all tasks if BOD
		if ( isBeginingOfDay(now)) {
			for (CronTask task : tasks) {
				task.reset();
			}
		}
		
		for (CronTask task : tasks) {
			if ( task.shouldExecute(now)) {
				task.execute();
			}
		}
	}
	
	public static synchronized void addTask (CronTask task) {
		tasks.add(task);
	}
	
	public static boolean isBeginingOfDay() {
		return isBeginingOfDay(new Date());
	}
			
	public static boolean isBeginingOfDay(Date date) {
		try {
			Date d1 = CronTask.getTimeOfDay(0, 0);
			Date d2 = CronTask.getTimeOfDay(0, 10);
			
			return date.after(d1) && date.before(d2);
		} catch (Exception e) {
			log.error("isBeginingOfDay " + e.toString());
			return false;
		}
	}

}
