package com.rts.datasource;

import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.rts.datasource.IDataSource.DataSourceType;

/**
 * Reusable poller Data source using a single thread
 * @author VSilva
 *
 */
public class BasePollerDataSource extends BaseDataSource {

	private static final Logger log = LogManager.getLogger(BasePollerDataSource.class);
	
	private void LOGD(String text) {
		log.debug(String.format("[%s] %s", name,  text));
	}
	
	/**
	 * Base Event Handler
	 * @author VSilva
	 *
	 */
	public static abstract class IPollEvents {
		protected long frequency;

		public IPollEvents(long frequency) {
			super();
			this.frequency = frequency;
		}

		public abstract void fetch () throws Exception;
	}
	
	/** Used to get messages in the background */
	protected Thread poller ;

	/** Polling events: fetch, edtc...*/
	protected IPollEvents events;
	
	public BasePollerDataSource(DataSourceType type, final String name, final String description, IPollEvents events ) { 
		super(type, name, description);
		this.events = events;
	}

	public BasePollerDataSource(JSONObject ds) throws JSONException {
		super(ds);
	}

	protected void pollerStart () {
		poller = new Thread(new Runnable() {
			public void run() {
				while ( true ) {
					try {
						events.fetch();
					} catch (Exception e1) {
						//e1.printStackTrace();
						log.error("Fetch events: " + e1.toString());
					}
					try {
						Thread.sleep(events.frequency);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
				
				LOGD("Poller loop terminated.");
			}
		}, "DS-POLLER-" + name);

		poller.start();
		LOGD("Started message poller for " + name );
	}
	
	protected void pollerStop () {
		if ( poller == null ) {
			return;
		}
		LOGD("Interrupting message poller.");
		poller.interrupt();
		try {
			poller.join(5000);
		} catch (InterruptedException e) {
		}
	}

}
