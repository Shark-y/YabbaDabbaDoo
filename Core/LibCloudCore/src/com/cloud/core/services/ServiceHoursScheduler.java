package com.cloud.core.services;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;


/**
 * Simple Service Hours Scheduler. Use it to  either start or stop the current node based on a daily schedule.
 * <pre>
 *  ServiceHoursScheduler sched = new ServiceHoursScheduler();
 *  Trigger start = new Trigger("t-start", 14, 0, new Runnable() {
 *    public void run() {
 *      System.out.println("Start");
 *    }
 *  });
 *  Trigger stop = new Trigger("t-stop", 17, 57, new Runnable() {
 *    public void run() {
 *      System.out.println("Stop");
 *    }
 *  });
 *  WeekDay[] days = new WeekDay[] { WeekDay.SAT, WeekDay.SUN};
 *
 *  sched.schedule(new TriggerTuple(start, stop, days));
 *  sched.start();
 *
 *  Thread.sleep(300000);
 *
 *  sched.stop();
 * </pre>
 * <h2>Change Log</h2>
 * <ul>
 * <li> 4/3/2017 This code has been deprecated and will be removed soon.
 * </ul>
 * @deprecated 4/3/2017 This code never worked properly. All references to this class should be removed.
 * @author sharky
 *
 */
public class ServiceHoursScheduler {
	private static final Logger log = LogManager.getLogger(ServiceHoursScheduler.class);
	
	static final String TAG = "[SERVICE-HRS] ";
	
	public static final WeekDay[] WEEK_BUSINESS = new WeekDay[] { WeekDay.MON, WeekDay.TUE, WeekDay.WED, WeekDay.THU, WeekDay.FRI };
	public static final WeekDay[] WEEK_END = new WeekDay[] { WeekDay.SAT, WeekDay.SUN};
	
	public enum WeekDay {MON, TUE, WED, THU, FRI, SAT, SUN };
	
	private static void LOGD(String text) {
		log.debug(TAG + text);
	}
	private static void LOGE(String text) {
		log.error(TAG + text);
	}
	
	/**
	 * Scheduler trigger
	 * @author sharky
	 *
	 */
	public static class Trigger {
		private Date desiredTime;
		private Runnable runnable;
		private String id;
		private Date executedTime;
		private int desiredHr;
		private int desiredMin;
	
		public Trigger(String id, int hoursOfDay, int minutesOfDay, Runnable runnable) {
			this.id 		= id;
			this.runnable 	= runnable;
			this.desiredHr 	= hoursOfDay;
			this.desiredMin = minutesOfDay;
		}
		void execute() {
			runnable.run();
			executedTime = new Date();
		}
		
		boolean shouldExecute(Date now) throws ParseException {
			boolean exec = now.after(computeDesiredTime()) && !isExecuted(); 
			return exec;
		}
		
		Date computeDesiredTime() throws ParseException {
			desiredTime = getTimeOfDay(desiredHr, desiredMin);
			return desiredTime;
		}
		
		boolean isExecuted () {
			return executedTime != null; 
		}
		
		void reset() {
			executedTime = null;
		}
		
		@Override
		public String toString() {
			try {
				computeDesiredTime();
			} catch (Exception e) {
			}
			return "[id:" + id + " desired:" + desiredTime +  " complete:" + executedTime + "]"; 
		}
		
		public Date getDesiredTime() {
			try {
				if ( desiredTime == null)
					computeDesiredTime();
			} catch (Exception e) {
			}
			return  new Date(desiredTime.getTime()); // Findbugs
		}
	}
	
	/**
	 * A trigger tuple used to declare a start/stop service descriptor
	 * @author sharky
	 *
	 */
	public static class TriggerTuple {
		Trigger start;
		Trigger stop;
		WeekDay[] days;
		
		public TriggerTuple(Trigger start, Trigger stop, WeekDay[] days) {
			this.start = start;
			this.stop = stop;
			// Findbugs: ServiceHoursScheduler.java:134 new com.cloud.core.services.ServiceHoursScheduler$TriggerTuple(ServiceHoursScheduler$Trigger, ServiceHoursScheduler$Trigger, ServiceHoursScheduler$WeekDay[]) may expose internal representation by storing an externally mutable object into ServiceHoursScheduler$TriggerTuple.days [Of Concern(18), Normal confidence]
			this.days = Arrays.copyOf(days, days.length);
		}
		
		void reset() {
			start.reset();
			stop.reset();
		}
		
		boolean daysInRange(Calendar date) {
			WeekDay dow = fromCalendarDay(date);
			for (int i = 0; i < days.length; i++) {
				if ( days[i] == dow) {
					return true;
				}
			}
			return false;
		}
		
		@Override
		public String toString() {
			StringBuffer s = new StringBuffer();
			if ( days != null) { 	// findbugs
				for (int i = 0; i < days.length; i++) {
					s.append(days[i] + " ");
				}
			}
			return "[" + start + " " + stop  + (days != null ? " Days:" + s : "") + "]";
		}
	}
	
// FIXME Removed on 12/6/2016 for Thread consolidation - private Timer master;
	private final int masterInterval = 5000;
	private List<TriggerTuple> triggers = new CopyOnWriteArrayList<TriggerTuple>();
	
	/**
	 * Create a scheduler
	 */
	public ServiceHoursScheduler() {
// FIXME Removed on 12/6/2016 for Thread consolidation - master = new Timer("CloudServicesMasterScheduler");
	}
	
	public void schedule (TriggerTuple trigger) {
		LOGD("Adding tuple " + trigger);
		triggers.add(trigger);
	}


	private void processTrigger (Trigger t, Date now) throws ParseException {
		if ( t.shouldExecute(now)) {
			t.execute();
		}
		/*
		else {
			if ( ! t.isExecuted()) {
				LOGD("Trigger " + t.id + " defrred @ " + t.desiredTime);
			}
		}*/
	}
	
	/**
	 * Is the schedule within service hours?
	 * @return
	 */
	public boolean isOutsideServiceHours() {
		try {
			return internalOutsideServiceHours();
		} catch (Exception e) {
			LOGE(e.toString());
			return false;
		}
	}
	
	/*
	 * Internal outside service hours.
	 */
	private boolean internalOutsideServiceHours() throws ParseException {
		Date now 		= new Date();
		Calendar cal 	= new GregorianCalendar();
		
		TriggerTuple tt = triggers.get(0);
		
		// reset range & begining of day or the trigger will NOT fire.
		if ( isBeginingOfDay(now)) {
			tt.reset();
		}

		// Check if the days are in range
		if ( !tt.daysInRange(cal)) {
			return true;
		}
		
		// check if the date HOURS are out of range
		cal.setTime(tt.stop.computeDesiredTime());
		cal.add(Calendar.SECOND, masterInterval/1000);

		//System.out.println("IS Outside serv hrs now=" + now + " STOP DESIRED=" + tt.stop.desiredTime + " STOP AT=" + cal.getTime()); 
		if ( now.after( cal.getTime())) {
			return true;
		} 
		
		cal.setTime(tt.start.computeDesiredTime());
		
		if ( now.before(cal.getTime())) {
			return true;
		}
		return false;
	}
	
	private void processRanges () throws ParseException {
		Date now 		= new Date();
		Calendar cal 	= new GregorianCalendar();
		
		for (TriggerTuple r : triggers) {
			// reset range & begining of day or the trigger will NOT fire.
			if ( isBeginingOfDay(now)) {
				r.reset();
			}

			// Check if the days are in range
			if ( !r.daysInRange(cal)) {
				continue;
			}
			// check if the date HOURS are out of range
			cal.setTime(r.stop.computeDesiredTime());
			cal.add(Calendar.SECOND, masterInterval/1000);

			//System.out.println("now=" + now + " STOP DESIRED=" + r.stop.desiredTime + " STOP TIME=" + cal.getTime()); 
			if ( now.after( cal.getTime())) {
				continue;
			}
			
			processTrigger(r.start, now);
			processTrigger(r.stop, now);
		}
	}
	
	private boolean isBeginingOfDay(Date date) {
		try {
			Date d1 = getTimeOfDay(0, 0);
			Date d2 = getTimeOfDay(0, 30);
			
			//System.out.println("BOD d=" + date + " c1=" + d1 + " c2=" + d2);
			return date.after(d1) && date.before(d2);
		} catch (Exception e) {
			LOGE("isBeginingOfDay " + e.toString());
			return false;
		}
	}

	void run() {
		try {
			processRanges();
		} catch (Exception e) {
			LOGE(e.toString());
		}
	}
	
	/* FIXME Removed on 12/6/2016 for Thread consolidation - Delete soon.
	public void start() {
		LOGD("Starting service scheduler with master interval (ms) " + masterInterval);
		
		master.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					processRanges();
				} catch (Exception e) {
					LOGE(e.toString());
				}
			}
		}, masterInterval, masterInterval); 
	} */
	
	public void stop () {
		/* FIXME Removed on 12/6/2016 for Thread consolidation - Remove soon
		if ( master != null) {
			LOGD("Destroy: Canceling master interval");
			master.cancel();
		} */
	}
	
	public void clear () {
		triggers.clear();
	}
	
	public static Date getTimeOfDay(int hour, int minute) throws ParseException {
		Calendar now = new GregorianCalendar();
		
		String year = String.valueOf(now.get(Calendar.YEAR)); 
		int month 	= now.get(Calendar.MONTH) + 1;
		int day 	= now.get(Calendar.DAY_OF_MONTH);
		int sec 	= now.get(Calendar.SECOND);
		
		String date = sec + " " + minute + " " + hour + " " + day + " " + month + " " + year;
		
		// Bug 12h day :( return new SimpleDateFormat("ss mm hh dd MM yyyy").parse(date);
		return new SimpleDateFormat("ss mm HH dd MM yyyy").parse(date); // vsilva 12/6/2016 24 H days
	}
	
	static WeekDay fromCalendarDay(Calendar date) {
		int dow = date.get(Calendar.DAY_OF_WEEK);
		switch (dow) {
		case Calendar.MONDAY:
			return WeekDay.MON;
		case Calendar.TUESDAY:
			return WeekDay.TUE;
		case Calendar.WEDNESDAY:
			return WeekDay.WED;
		case Calendar.THURSDAY:
			return WeekDay.THU;
		case Calendar.FRIDAY:
			return WeekDay.FRI;
		case Calendar.SATURDAY:
			return WeekDay.SAT;
		}
		return WeekDay.SUN;
	}
	
	/**
	 * Parse a day range string of the form DAY1,DAY2,... (MON,TUE,...)
	 * Currently only a comma separated string is supported
	 * @param daysStr String of the form DAY1,DAY2,...
	 * @return {@link WeekDay} array
	 */
	public static WeekDay[] parseDayString (String daysStr) {
		String[] vals 	= daysStr.split(",");
		WeekDay[] days 	= new WeekDay[vals.length];
		
		for (int i = 0; i < vals.length; i++) {
			days[i] = WeekDay.valueOf(vals[i]);
		}
		return days;
	}
	
}
