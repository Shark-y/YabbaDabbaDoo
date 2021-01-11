package com.rts.datasource;

import org.json.JSONException;

import com.rts.core.IBatchEventListener;
import com.rts.datasource.sock.TCPSockDataSource;

public interface IDataMatcher {

	public static final String LINE_FEED = "\n";

	/**
	 * Match a TCP socket raw data against a {@link DataFormat} & notify an {@link IBatchEventListener}.
	 * @param dataSourceName Name or Id of the {@link TCPSockDataSource} that listens for data.
	 * @param data Raw data from the server socket.
	 * @param format {@link DataFormat} used to describe the raw socket data.
	 * @param listener Listener that will receive the resulting notification as a JSON object.
	 * @throws JSONException
	 */
	void matchFormat (String dataSourceName, String data, DataFormat format, IBatchEventListener listener) throws JSONException;

	long getTotalBatches();
	
	long getTotalRecords();

	long getRemoteTotalBatches();

	void resetMetrics();
}
