package com.cloud.core.cron;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Poor man implementation of a Cron-task. Currently, this task can only execute ONCE every day @ a given HH:mm.
 * 
 * <p>Eventually this task will provide full cron-like functionality.</p>
 * 
 * @author VSilva
 * @version 1.0.0 - A work in progress.
 */
public class CronTask {

	/** task id */
	private String id;
	private int desiredHr;
	private int desiredMin;
	private Date desiredTime;
	private Runnable runnable;
	//private Date completedTime;
	private long completedTime;
	
	/**
	 * Construct a DAILY task.
	 * @param id A unique ID.
	 * @param desiredHours Desired hours 0-24.
	 * @param desiredMinutes Desired minutes 0-60.
	 * @param runnable The task to execute.
	 * @throws ParseException
	 */
	public CronTask(String id, int desiredHours, int desiredMinutes, Runnable runnable) throws ParseException {
		this.id 		= id;
		this.runnable 	= runnable;
		this.desiredHr 	= desiredHours;
		this.desiredMin = desiredMinutes;
		this.desiredTime = getTimeOfDay(desiredHr, desiredMin);
	}
	
	void execute() {
		runnable.run();
		completedTime = System.currentTimeMillis(); // new Date();
	}
	
	boolean shouldExecute(Date now)  {
		boolean exec = now.after( desiredTime ) && !isExecuted(); 
		return exec;
	}
	
	boolean isExecuted () {
		return completedTime > 0; // != null; 
	}
	
	void reset() {
		completedTime = 0; // null;
	}
	
	@Override
	public String toString() {
		return "[id:" + id + " desired:" + desiredTime +  " completed:" + completedTime + "]"; 
	}
	
	public Date getDesiredTime() {
		return  new Date(desiredTime.getTime()); 
	}

	/**
	 * Format the current time for a given HH:mm
	 * @param hour Desired HH
	 * @param minute Desired mm
	 * @return Current Date for desired HH:mm
	 * @throws ParseException
	 */
	public static Date getTimeOfDay(int hour, int minute) throws ParseException {
		Calendar now = new GregorianCalendar();
		
		String year = String.valueOf(now.get(Calendar.YEAR)); 
		int month 	= now.get(Calendar.MONTH) + 1;
		int day 	= now.get(Calendar.DAY_OF_MONTH);
		int sec 	= now.get(Calendar.SECOND);
		
		String date = sec + " " + minute + " " + hour + " " + day + " " + month + " " + year;
		
		// vsilva 12/6/2016 24 H days
		return new SimpleDateFormat("ss mm HH dd MM yyyy").parse(date); 
	}

}
