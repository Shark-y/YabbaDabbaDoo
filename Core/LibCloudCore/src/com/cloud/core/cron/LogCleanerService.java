package com.cloud.core.cron;

import java.io.File;
import java.util.Properties;

import com.cloud.core.cron.AutoUpdateUtils;
import com.cloud.core.io.FileTool;
import com.cloud.core.logging.Container;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.CloudServicesMasterTask;
import com.cloud.core.services.NodeConfiguration;

/**
 * A simple log cleaner service that runs 1 per week to keep logs clean.
 * <ul>
 * <li>It uses a {@link CleanPolicy} stores in the {@link NodeConfiguration}
 * <li>Deletes files by last update time older than 2 weeks, 1 month or
 * <li>By size: larger than 200, 500MB.
 * <li>This class in invoked by the {@link CloudServicesMasterTask}.
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.0 2/2/2019
 * 
 */
public class LogCleanerService {

	private static final Logger log 	= LogManager.getLogger(LogCleanerService.class);
	
	private static boolean executed;
	
	// Tick count
	private static long tickCount;

	/**
	 * Clean policy type/action: By last update or file size.
	 * @author VSilva
	 *
	 */
	public enum CleanPolicy {
		DISABLED,
		REMOVE_OLDER_THAN_2WEEKS,
		REMOVE_OLDER_THAN_1MONTH,
		//REMOVE_OLDER_THAN_6MONTHS,
		REMOVE_BIGGER_THAN_200MB,
		REMOVE_BIGGER_THAN_500MB
	}
	
	/**
	 * To be executed from the {@link CloudServicesMasterTask}.
	 */
	public static void run (Properties config)  {
		// about once/5 min (300)
		if ( (tickCount++ % 300) != 0 || tickCount <= 2) {
			return;
		}
		try {
			tick(config);
		} catch (Exception e) {
			log.error("Error Notification System:", e);
		}
	}

	/**
	 * This service runs 1/month.
	 * @param config {@link NodeConfiguration} containing the cleaner policy.
	 * @throws Exception on any kind of error I/O, NPEs, etc.
	 */
	private static void tick (Properties config) throws Exception {
		AutoUpdateUtils.Frequency freq 		= AutoUpdateUtils.Frequency.MONTHLY; // 2/9/2019 WEEKLY;
		boolean shouldExecute			 	= AutoUpdateUtils.checkFrequency(freq);
		
		if ( shouldExecute && ! executed ) {
			clean(config);
			executed = true;
		}
		// reset?
		if ( !shouldExecute ) {
			executed = false;
		}
	}

	/*
	 * Invoked by tick()
	 */
	private static void clean(Properties config) {
		final CleanPolicy policy	= CleanPolicy.valueOf( config.getProperty(NodeConfiguration.KEY_LOG_CLEANER_POL, CleanPolicy.REMOVE_OLDER_THAN_2WEEKS.name()));
		clean (policy);
	}

	/**
	 * This method loops thru the log folder & removed log files depending on the node CleanPolicy.
	 * Note: this method is public so it can be invoked synchronously from the cloud console (it should take long)
	 * @param policy A {@link CleanPolicy}. Files that match this policy will be removed.
	 * @return A message indicating the action taken: 'Cleaned X files i Y ms, Service is disabled or Cleaned ZERO files.'
	 */
	public static String clean(final CleanPolicy policy) {
		final String path 			= Container.getDefautContainerLogFolder();
		//final CleanPolicy policy	= CleanPolicy.valueOf( config.getProperty(NodeConfiguration.KEY_LOG_CLEANER_POL, CleanPolicy.REMOVE_OLDER_THAN_2WEEKS.name()));

		final long TWO_WEEKS_IN_MS		= 1209600000;
		final double ONE_MONTH_IN_MS	= 2628002880d;
		
//		System.out.println(config);		
//		System.out.println(policy);
		
		if ( policy == CleanPolicy.DISABLED) {
			return "The LogCleaner service is disabled.";
		}
		String response				= "Cleaned ZERO files.";
		
		// Log file extension filters (.log)
		final String[] exts 		= new String[] {"log"};

		// List container log files
		File[] files 				= FileTool.listFiles(path, exts, null); //filters);

		long t0 					= System.currentTimeMillis();
		long count					= 0;
		
		for ( File f : files) {
			long date1 		= f.lastModified();
			long now		= System.currentTimeMillis();
			long size		= f.length();
			long deltaT		= now - date1;
			boolean delete	= false;
			
			switch (policy) {
			case REMOVE_OLDER_THAN_2WEEKS:
				if ( deltaT >= TWO_WEEKS_IN_MS) {
					delete = true;
				}
				break;
			case REMOVE_OLDER_THAN_1MONTH:
				if ( deltaT >= ONE_MONTH_IN_MS) {
					delete = true;
				}
				break;
			case REMOVE_BIGGER_THAN_200MB:
				if ( size >= 200 * 1024 * 1024) {
					delete = true;
				}
				break;
			case REMOVE_BIGGER_THAN_500MB:
				if ( size >= 500 * 1024 * 1024) {
					delete = true;
				}
				break;
			default:
				break;
			}
			if ( delete) {
				//System.out.println(policy + " " + f);
				if ( f.delete() ) {
					count++;
				}
			}
		}
		long delta 	= System.currentTimeMillis() - t0;
		if ( count > 0 ) {
			log.info( policy + ": Cleaned " + count + " files in " + delta + " ms from " + path);
			response = "Cleaned " + count + " files in " + delta + " ms from " + path + " (" + policy + ")";
		}
		return response;
	}
}
