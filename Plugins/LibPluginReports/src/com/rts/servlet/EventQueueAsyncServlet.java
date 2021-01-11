package com.rts.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.cloud.console.HTTPServerTools;
import com.cloud.console.JSPLoggerTool;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;
import com.cloud.core.services.PluginSystem;
import com.rts.core.EventQueue;
import com.rts.service.RealTimeStatsService;

/**
 * Cloud reports asynchronous servlet.
 * 
 * <li>The container creates ONE instance of this servlet.
 * <li>doGet gets invoked many times for each dashboard.
 * 
 * @author VSilva
 * @version 1.0.0 - Initial implementation: pop operation only.
 *
 */
@WebServlet(description = "CloudReports asynch servlet", urlPatterns = { "/Amq" }, asyncSupported=true)
public class EventQueueAsyncServlet extends HttpServlet {

	private static final Logger log = LogManager.getLogger(EventQueueAsyncServlet.class);
	
	private static final long serialVersionUID = 9029065609277522335L;

	/** Used for poll reaping - kill the poll when the dash window is closed - to prevent mem leaks on container shutdown */
	private final List<PollJob> pollTracker = new CopyOnWriteArrayList<EventQueueAsyncServlet.PollJob>();
	
	/**
	 * Async event listener. It tracks the status of the async poll: failed, complete or timed out.
	 * @author VSilva
	 *
	 */
	static class PollListener implements AsyncListener {
		boolean failed;
		boolean complete;
		boolean timedOut;
		final PrintWriter pw;
		
		public PollListener(final PrintWriter pw) {
			this.pw = pw;
		}
		
		@Override
		public void onTimeout(AsyncEvent evt) throws IOException {
			timedOut = true;
			// if timedout must write an empty JSON response so the clienty won't give an error
			JSONObject empty = new JSONObject();
			HTTPServerTools.injectStatus(empty, 200, "OK");
			
			pw.print(empty);

			// Fix for long poll returning HTTP 500 on request timeout.
			evt.getAsyncContext().complete();
		}
		
		@Override
		public void onStartAsync(AsyncEvent evt) throws IOException {
			failed = complete = false;
			timedOut = false;
		}
		
		@Override
		public void onError(AsyncEvent evt) throws IOException {
			// fires when complete has not been called by the backend before the
			// timeout expires.
			failed = true;
		}
		
		@Override
		public void onComplete(AsyncEvent evt) throws IOException {
			complete = true;
		}
	}
	
	/**
	 * Actual Poll Job (runnable)
	 * @author VSilva
	 *
	 */
	/*static*/ class PollJob implements Runnable {
		final PollListener listener;
		final AsyncContext context;
		final String name;
		final PrintWriter pw;
		final String windowId;
		final String dash;
		private boolean killed;
		
		/**
		 * @param listener Event listener.
		 * @param context Request Async context.
		 * @param name Data source/queue
		 * @param pw Response writer.
		 * @param windowId Window (Client) id.
		 */
		public PollJob(final PollListener listener, final AsyncContext context, final String name, final PrintWriter pw, final String windowId, final String dash) {
			this.listener 	= listener;
			this.context 	= context;
			this.name 		= name;			// DS/queue
			this.pw 		= pw;
			this.windowId	= windowId;
			this.dash		= dash;			// optional dashboard
		}
		
		public void run() {
			try {
				boolean sent = false;
				while ( ! listener.timedOut && !listener.failed && !listener.complete && !killed) {
					
					JSONObject root = EventQueue.pop(name, "dash", dash);
					
					if ( root != null) {
						// inject an HTTP status & SEND
						HTTPServerTools.injectStatus(root, 200, "OK");
						pw.print(root);
						sent = true;
						break;
					}
					else {
						// wait
						try {
							Thread.sleep((long)100);
						} catch (InterruptedException e) {
							log.info("Poll  operation interrupted. Abort.");
							break;
						}
					}
				}
				//log.debug(name + " Long poll end failed:" + listener.failed + " timed out:" + listener.timedOut + " Complete:" + listener.complete + " killed=" + killed);
				
				// Added 8/26/2019 : Bug - killed = true on 2 concurrent polls? Don't know why
				if ( ! sent ) {
					JSONObject empty = new JSONObject();
					HTTPServerTools.injectStatus(empty, 200, "OK");
					pw.print(empty);
					
				}
				if ( listener.failed) {
					log.warn("DataSource: " + name + " long poll failed.");
				}
				/*
				if ( listener.timedOut) {
					log.warn("DataSource: " + name + " long poll timed out.");
				}*/
				// must reap the poll to prevent overflowing the list w/ polls for the same window id.
				doPollReap(windowId, false);
				
				// return from async servlet
				if (! listener.failed && !listener.timedOut) {
					context.complete();
				}
			} catch (Exception e) {
				log.error("Poll: " + name , e);
			}
		}
		
		@Override
		public String toString() {
			return String.format("%s %s",  windowId, name);
		}
		
		void kill () {
			killed = true;
		}
	}
	
	/**
	 * This method gets invoked multiple times for each dash board.
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		final String operation	= request.getParameter("op");		// operation
		final String name		= request.getParameter("name");		// data source/queue
		final String windowId 	= request.getParameter("windowId");	// client id
		final String dash 		= request.getParameter("dash");	// optional dashboard
		
		final PrintWriter pw 	= response.getWriter();
		JSONObject root 		= null;

		// response type
		response.setContentType(HTTPServerTools.CONTENT_TYPE_JSON);

		// Op, listener name are required.
		if ( operation == null ) {
			root = new JSONObject();
			HTTPServerTools.injectStatus(root, 500, "Request opration (op) is required.");
			pw.print(root);
			return;
		}
		/* Some ops require a queue name */
		if ( operation.equals("pop") && name == null) {
			root = new JSONObject();
			HTTPServerTools.injectStatus(root, 500, "A queue name (name) is required for operation " + operation);
			pw.print(root);
			return;
		} 
		
		// check if listener name (data source) exists for ops: pop
		if ( operation.equals("pop") && !EventQueue.containsQueue(name)) {
			root = new JSONObject();
			HTTPServerTools.injectStatus(root, 500, "Missing data source " + name);
			pw.print(root);
			return;
		}
		// Init dash.
		if ( operation.equals("init") ) {
			doInit(name, windowId);
			return;
		}
		// Shutdown dash-board
		if ( operation.equals("shutdown") ) {
			doShutdown(name, windowId);
			doPollReap(windowId, true);
			return;
		}
	
		// async poll @ a timeout of 30s.
		final AsyncContext context = request.startAsync(request, response);
		context.setTimeout(30000);

		final PollListener pl 	= new PollListener(pw);
		final PollJob job 		= new PollJob(pl, context, name, pw, windowId, dash);
		
		// save for poll reaping when window is closed
		pollTracker.add(job);
		//dumpPollTracker("ADD-" + windowId);
		
		context.addListener(pl);
		context.start(job);
	}
	
	@Override
	public void destroy() {
		super.destroy();
		for ( PollJob poll : pollTracker) {
			poll.kill();
		}
		pollTracker.clear();
	}
	
	void doPollReap (final String windowId, final boolean kill) {
		for ( PollJob poll : pollTracker) {
			if ( poll.windowId != null && poll.windowId.equals(windowId)) {
				if ( kill ) {
					poll.kill();
				}
				pollTracker.remove(poll);
			}
		}
		//dumpPollTracker("REAP-" + windowId);
	}
	
	void dumpPollTracker (String label) {
		StringBuffer buf = new StringBuffer("--- POLL TRACKER [" + label + "] ---\n");
		for ( PollJob poll : pollTracker) {
			buf.append(poll.toString() + "\n");
		}
		System.out.print(buf);
	}
	
	/**
	 * Initialize a dashboard. Sent by the browser when a dash window is opened.
	 * @param dataSource Data source name (required).
	 * @param windowId Window id (optional).
	 */
	private void doInit ( final String dataSource, final String windowId) {
		JSPLoggerTool.JSP_LOGD("INIT-DASH", "Initalizing dashboard for data soucre: " + dataSource + " Window Id:" + windowId);
		
		RealTimeStatsService service 	= (RealTimeStatsService)PluginSystem.findInstance(RealTimeStatsService.class.getName()).getInstance();
		service.clientTrackerInit(dataSource, windowId);
	}

	/**
	 * Shutdown a dashboard. Sent by the browser when a dash window is closed.
	 * @param dataSource Data source name (required).
	 * @param windowId Window id (optional).
	 */
	private void doShutdown ( final String dataSource, final String windowId) {
		JSPLoggerTool.JSP_LOGD("SHUTDOWN-DASH", "Shutting down dashboard for data soucre: " + dataSource + " Window Id:" + windowId);

		//RealTimeStatsService service 	= (RealTimeStatsService)CloudServices.findService(ServiceType.DAEMON);
		RealTimeStatsService service 	= (RealTimeStatsService)PluginSystem.findInstance(RealTimeStatsService.class.getName()).getInstance();
		service.clientTrackerDecrement(dataSource, windowId);
	}
	
}
