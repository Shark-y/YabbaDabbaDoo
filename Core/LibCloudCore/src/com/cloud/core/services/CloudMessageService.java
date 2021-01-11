package com.cloud.core.services;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.cloud.core.services.CloudMessageService;
import com.cloud.core.types.OMEvent;
import com.cloud.core.types.OMResponse;

/**
 * A very basic thread safe messaging system.
 * It is used to send messages between all actors in this system: 
 * Chat service, Contact Center or Agent(through the Web Socket)<pre> 
 * Usage:
 *	// Server: Initialization..
 *	CloudMessageService.initialize();
 *
 *	// Server: Cleanup
 *	CloudMessageService.destroy();
 *
 * 	// Sender... (Chat service)
 * 	CloudMessageService.postEvent(new OMEvent(OMEvent.EventType.blindTransfer, event.getRaw()), CloudMessageService.Destination.Agent);
 * 
 *	// EventReceiver... (Agent) on initialization.
 *	CloudMessageService.addListener(Destination.Agent, new CloudMessageService.MessageEventListener() {
 *		public void onEvent(OMEvent event) {
 *			consumeExternalEvent(event);
 *		}
 *	});
 *
 * @author vsilva
 * @version 1.0.1
 */
public class CloudMessageService 
{
	private static final Logger log = Logger.getLogger(CloudMessageService.class);
	
	/** Interval @ which the {@link EventReceiver} will expire in milliseconds (default: 10 min) */
	private static final int EXPIRATION_INTERVAL = 600000;
	
	/**
	 * Event/destination wrapper.
	 * @author vsilva
	 *
	 */
	private static class EventReceiver {
		OMEvent event;				// The posted event
		//Destination destination;	// The destination (receiver)
		String destination;			// The destination (receiver)
		long creationTime;			// Creation time (used for expiration)
		
		public EventReceiver(OMEvent event , Destination dest) {
			this.event 			= event;
			this.destination	= dest.name();
			this.creationTime	= System.currentTimeMillis();
		}
		
		public EventReceiver(OMEvent event , String dest) {
			this.event 			= event;
			this.destination	= dest;
			this.creationTime	= System.currentTimeMillis();
		}
		
		@Override
		public String toString() {
			return destination + ": " + event + " Expired: " + isExpired();
		}
		
		boolean isExpired() {
			long delta = System.currentTimeMillis() - creationTime;
			return delta > EXPIRATION_INTERVAL;
		}
	}

	/**
	 * A thread safe queue used to store all messages 
	 */
	private static Queue<EventReceiver> queue = new ConcurrentLinkedQueue<EventReceiver>();
	
	// Message destination (receiver)
	public enum Destination {ChatService, ContactCenter, Agent ,CallCenter, BackendService, CloudService};
	
	/**
	 * Event listener interface
	 * @author vsilva
	 *
	 */
	public static interface MessageEventListener {
		void onEvent(OMEvent event);
	}
	
	// a timer used to read messages from the queue & despatch them to the destination listeners
// FIXME Removed on 12/6/2016 for Thread consolidation - private static volatile ScheduledExecutor messageTimer;
	
	// destination listeners
	private static Map</*Destination*/String, MessageEventListener> listeners = new ConcurrentHashMap<String /*Destination*/, MessageEventListener>();
	
	/**
	 * Invoked by a service to receive messages
	 * @param destination EventReceiver name {@link Destination}
	 * @param l message listener {@link MessageEventListener}
	 */
	public static void addListener(Destination destination, MessageEventListener l) {
		listeners.put(destination.name(), l);
	}

	/**
	 * Subscribe to receive some message via {@link OMEvent}. It is equivalent to addListener()
	 * @param destination The destination name. Any String is accepted.
	 * @param l The event listener. See {@link MessageEventListener}.
	 * @since 1.0.1
	 */
	public static void subscribe(String destination, MessageEventListener l) {
		listeners.put(destination, l);
	}

	/**
	 * Publish a message to be received by someone else. Equivalent to postEvent()
	 * @param destination The message destination.
	 * @param event The message {@link OMEvent}.
	 * @since 1.0.1
	 */
	static public void publish( String destination, OMEvent event) {
		queue.offer(new EventReceiver(event, destination));
	}

	/**
	 * True if there is a queue for a given {@link Destination}.
	 * @param destination See {@link Destination}.
	 * @return True if there is a queue for a given {@link Destination}.
	 */
	public static boolean hasListener(Destination destination) {
		return listeners.containsKey(destination.name());
	}

	/**
	 * Return True if there is a subscriber for destination.
	 * @param destination Subscriber name.
	 * @return True if there is a subscriber for destination.
	 */
	public static boolean hasSubscriber(String destination) {
		return listeners.containsKey(destination);
	}

	/**
	 * Read messages from the local event queue and dispatch them to their {@link MessageEventListener} receiver.
	 */
	static void run() {
		try {
			//Set<String /*Destination*/> keys = listeners.keySet();
			Set<Map.Entry<String, MessageEventListener>> entries = listeners.entrySet(); 
			
			//dump("Queue");
			
			for (Map.Entry<String, MessageEventListener> entry : entries) {
				String dest 	= entry.getKey();
				
				// pop an event for that destination
				OMEvent event 	= CloudMessageService.popEvent(dest);
				
				if ( event != null) {
					//System.out.println("**** NEW MESSAGE dest:" + dest + " payload:" + event);
					
					// dispatch to destination listener
					MessageEventListener listener =  entry.getValue();
					
					if ( listener != null) {
						listener.onEvent(event);
					}
				}
			}
			
			collectGarbage();
		} 
		catch (Exception e) {
			log.error("Cloud MessageService", e);
		}
	}

	private static void collectGarbage() {
		// clean expired events
		for (EventReceiver e : queue) {
			if ( e.isExpired()) {
				//System.out.println("Event " + e + " has expired.");
				queue.remove(e);
			}
		}
	}
	
	/**
	 * Initialize the messaging system
	 */
	public static void initialize() {
		/**
		 * A simple message queuing system is used to communicate with other services.
		 * This timer is used to pop events from the queue and consume them.
		 */
		/* FIXME Removed on 12/6/2016 for Thread consolidation - Delete soon.
		messageTimer = new ScheduledExecutor("CoreMessageManager");
		
		messageTimer.scheduleWithFixedDelay(new TimerTask() {
			@Override
			public void run() {
				try {
					Set<Destination> keys = listeners.keySet();
					
					//System.out.println("** DESTINATIONS=" + keys);
					//dump("Queue");
					
					for (Destination dest : keys) {
						// pop an event for that destination
						OMEvent event = CloudMessageService.popEvent(dest);
						
						if ( event != null) {
							//System.out.println("**** NEW MESSAGE dest:" + dest + " payload:" + event);
							
							// dispatch to destination listener
							MessageEventListener listener = listeners.get(dest);
							
							if ( listener != null) {
								listener.onEvent(event);
								//CloudMessageService.dump(dest.toString());
							}
						}
					}
					
					collectGarbage();
				} catch (Exception e) {
					//log.error("MessageQueue:" + e.toString());
				}
			}
			
			private void collectGarbage() {
				// clean expired events
				for (EventReceiver e : queue) {
					if ( e.isExpired()) {
						//System.out.println("Event " + e + " has expired.");
						queue.remove(e);
					}
				}
			}
		}, 0, 1000);
		*/
	}

	public static void destroy() {
		/* FIXME Removed on 12/6/2016 for Thread consolidation - Delete soon.
		if ( messageTimer != null) {
			log.debug("Shutting down core message manager.");
			messageTimer.shutdown(); 
			messageTimer = null;
		} */
		queue.clear();
	}
	
	/**
	 * Send an event to a specific destination
	 * @param event {@link OMEvent} object
	 * @param destination {@link Destination} event receiver: Agent, Chat Service or Contact Center
	 */
	static public void postEvent( OMEvent event, Destination destination) {
		queue.offer(new EventReceiver(event, destination));
	}
	
	/**
	 * Called by the event receiver to retrieve the first event for a given destination 
	 * @param destination Event receiver or {@link Destination}
	 * @return The first event for a given destination
	 */
	static private OMEvent popEvent(/*Destination*/String destination) {
		for (EventReceiver r : queue) {
			//if ( r.destination == destination){
			if ( r.destination. equals(destination) ){
				queue.remove(r);
				return r.event;
			}
		}
		return null;
	}
	
	static public void dump(String label) {
		System.out.println("-- Queue " + label + " START (" + queue.size() + ") --");
		
		for (EventReceiver e : queue) {
			System.out.println(e);
		}
		System.out.println("-- Queue " + label + " END --");
	}

	/**
	 * Create a basic event {@link JSONObject} with the session.
	 * @param session
	 * @return
	 * @throws JSONException
	 */
	private static JSONObject buildRawEvent(String session) throws JSONException {
		JSONObject event = new JSONObject();
		event.put(OMEvent.KEY_SESSION, session);
		event.put(OMEvent.KEY_WORKITEM_ID, session);
		return event;
	}
	
	/**
	 * Send a chat interaction release to the agent manager.
	 * @deprecated This method belong in the Cloud Adapter product.
	 * @param chatId Chat id.
	 */
	public static void agentManagerPostSessionRelease(long chatId) {
		
		try {
			JSONObject event = buildRawEvent(String.valueOf(chatId));
			
			CloudMessageService.postEvent(new OMEvent(OMEvent.EventType.sessionReleased, event), CloudMessageService.Destination.Agent);
		} catch (JSONException e) {
			log.error("ReleaseChat: " + chatId + " Failed to notify agent manager:" + e.getMessage()); 
		}
	}
	
	/**
	 * Post a status message to the agent manager.
	 * @deprecated This method belong in the Cloud Adapter product.
	 * @param session Session (chat) id the agent is handling.
	 * @param status HTTP status code.
	 * @param message Status message.
	 */
	public static void agentManagerPostStatusMessage(String session, int status,  String message) {
		JSONObject event =  OMResponse.createStatusResponse(status, message);
		try {
			// attach params: session (chatId)
			event.put(OMEvent.KEY_SESSION, session);
			
			CloudMessageService.postEvent(new OMEvent(OMEvent.EventType.statusResponse, event), CloudMessageService.Destination.Agent);
		} catch (JSONException e) {
			log.error("Send StatusMessage Error: " + e.toString() + " session:" + session + " Message:" + message);
		}
	}

}
