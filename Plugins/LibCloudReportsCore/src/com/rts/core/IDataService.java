package com.rts.core;

import com.rts.datasource.DataSourceManager;
import com.rts.datasource.IDataSource;

/**
 * An interface for 2+ implementations of a reports service. Currently used by:
 * <ul>
 * <li> The cloud reports deamon service (data sources).
 * <li> The cloud storage micro service (data stores).
 * </ul>
 * 
 * @author VSilva
 * @version 1.0.0 - 9/30/2017 Initial implementation
 *
 */
public interface IDataService {

	public enum DataServiceType {
		DATASOURCE,
		DATASTORE
	};
	
	/**
	 * Get the service {@link DataSourceManager} which tracks data sources or data stores.
	 * @return See {@link DataSourceManager}.
	 */
	public DataSourceManager getDataSourceManager();
	
	/**
	 * Get a data source or data store by name.
	 * @param name Data source/store name.
	 * @return An {@link IDataSource}.
	 */
	public IDataSource getDataSource (final String name);
	
	/**
	 * @return The type of data service: DATASTORE or DATASOURCE.
	 */
	public DataServiceType getDataServiceType ();
	
	/**
	 * For data sources that produce data batches such as Socket.
	 * @return The {@link IBatchEventListener} that consumes the batches generated by the data source
	 */
	public IBatchEventListener getEventListener();
}