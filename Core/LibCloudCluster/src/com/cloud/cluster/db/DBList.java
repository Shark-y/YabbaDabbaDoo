package com.cloud.cluster.db;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.json.JSONArray;

import com.cloud.core.db.Database;
import com.cloud.core.db.JDBCResultSet;
import com.cloud.core.io.ObjectIO;
import com.cloud.core.logging.LogManager;
import com.cloud.core.logging.Logger;

/**
 * Cluster implementation of {@link List} using a Database.
 * 
 * <pre>
 * List<String> list1 = cluster.getClusterInstance().getList("list1");
 * list1.add("Foo1");
 * list1.clear();
 * </pre>
 * 
 * @author VSilva
 * @version 1.0.0
 *
 * @param <E> list element type.
 */
public class DBList<E> implements List<E> {

	static final Logger log 					= LogManager.getLogger(DBList.class);
	
	private static final String DB_TABLENAME 	= "ARRAYLISTS";
	
	private Database 	db 						= Database.getInstance();
	private String 		name;
	private String 		nodeGroup;
	private int 		index;
	
	/**
	 * Construct
	 * @param name Cluster list name.
	 * @param nodeGroup Cluster group name.
	 */
	public DBList(String name, String nodeGroup) {
		super();
		this.name 		= name;
		this.nodeGroup 	= nodeGroup;
		this.index		= getMaxIndex();	// in case there are leftovers in the DB
		log.debug("Construct List Name: " + name + " Group: " + nodeGroup + " Start Index:" + index);
		//clear();
	}

	/*
	 * Get the max index value from the DB (in case there are left over items). Return 0 if empty.
	 */
	private int getMaxIndex() {
		String SQL = String.format ("select max(listIndex) from %s WHERE %s", DB_TABLENAME, getDefaultSQLCondition()); 
		Object obj = dbQuery("getMaxIndex(", SQL, 0, 0);
		return obj != null ? Integer.parseInt(obj.toString()) : 0;
	}
	
	/**
	 * @return name = '" + mapName + "' AND nodeGroup = '" + nodeGroup + "'";
	 */
	private String getDefaultSQLCondition () {
		return String.format("name = '%s' AND nodeGroup = '%s'", name, nodeGroup);
	}

	private boolean dbUpdate (String label, String SQL, boolean throwExceptionOnZeroResults ) {
		try {
			int rows = db.update(SQL);
			if ( rows == 0 && throwExceptionOnZeroResults) {
				throw new IOException("Update returned 0 record size.");
			}
			return true;
		} catch (Exception e) {
			log.error(label + " DB Update " + SQL, e);
			return false;
		}
	}

	private Object dbQuery (String label, String SQL, int row, int col ) {
		try {
			JDBCResultSet rs = db.query(SQL);
			JSONArray rows	 = rs.getResultSet();
			
			if ( rows.length() == 0 ) {
				log.warn("No data for SQL " + SQL);
				return null;
			}
			return rows.optJSONArray(row).opt(col);
		} catch (Exception e) {
			log.error(label + " with Query " + SQL, e);
			return null;
		}
	}

	@Override
	public boolean add(E e) {
		try {
			long time 	= System.currentTimeMillis();
			String b64 	= ObjectIO.encodeObjectAsB64(e);
			String SQL 	= String.format("INSERT INTO %s VALUES ( '%s' , '%s' , %d , %d , '%s')", DB_TABLENAME, name, nodeGroup, time, index , b64);
			boolean ret = dbUpdate("Add", SQL, true);
			if ( ret ) 	index++;
			return ret;
		} catch (Exception e2) {
			log.error("Add " + name, e2);
			return false;
		}
	}

	@Override
	public void add(int index, E element) {
		throw new RuntimeException("Method add(int index, E element) not implemented.");
		/*
		try {
			boolean found 	= db.exists(DB_TABLENAME, String.format("%s AND listIndex = %d", getDefaultSQLCondition(), index));
			String b64 		= ObjectIO.encodeObjectAsB64(element);
			if ( found) {
				dbUpdate("add@" + index, String.format("UPDATE %s SET listValue = '%s'", DB_TABLENAME, b64));
				// Must shift elements to the right :(
			}
			else {
				long time 	= System.currentTimeMillis();
				dbUpdate("add@" + index, String.format("INSERT INTO %s VALUES ( '%s' , '%s' , %d , %d , '%s')", DB_TABLENAME, name, nodeGroup, time, index , b64));
			}
		} catch (Exception e) {
		} */
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		for (E e : c) {
			add(e);
		}
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		throw new RuntimeException("Method addAll(int index, Collection<? extends E> c) not implemented.");
	}

	@Override
	public void clear() {
		dbUpdate("clear()", String.format("DELETE FROM ARRAYLISTS WHERE %s", getDefaultSQLCondition()), false);
	}

	@Override
	public boolean contains(Object o) {
		try {
			String b64 		= ObjectIO.encodeObjectAsB64(o);
			String SQL		= String.format("SELECT listValue FROM %s WHERE %s AND listValue = '%s'", DB_TABLENAME, getDefaultSQLCondition(), b64);
			return dbQuery("contains", SQL, 0, 0) != null;
		} catch (Exception e) {
			log.error("Contains " + o, e);
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new RuntimeException("Method containsAll(Collection<?> c) not implemented.");
	}

	@Override
	public E get(int index) {
		String SQL = String.format("SELECT listValue FROM %s WHERE %s AND listIndex = %d", DB_TABLENAME, getDefaultSQLCondition(), index);
		Object b64 = dbQuery("Get@" + index, SQL, 0, 0);
		try {
			return b64 != null ? (E)ObjectIO.decodeObjectFromB64(b64.toString()) : null;
		} catch (Exception e) {
			log.error("get@" + index, e);
			return null;
		}
	}

	@Override
	public int indexOf(Object o) {
		throw new RuntimeException("Method indexOf(Object o) not implemented.");
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Implements a basic {@link Iterator} for {@link DBList}.
	 * @author VSilva
	 *
	 * @param <T> List element type.
	 */
	class DBListIterator<T> implements Iterator<T> {
		JSONArray rows;
		int index;
		
		public DBListIterator() {
			String SQL = String.format("SELECT listValue FROM %s WHERE %s", DB_TABLENAME, getDefaultSQLCondition());
			try {
				JDBCResultSet rs  	= db.query(SQL);
				rows 				= rs.getResultSet();
			} catch (Exception e) {
				log.error("DBListIterator()", e);
			}
		}
		
		@Override
		public boolean hasNext() {
			return index < rows.length(); 
		}

		@Override
		public T next() {
			try {
				if ( index > rows.length() ) {
					throw new NoSuchElementException(String.format("%d", index));
				}
				
				JSONArray row 	= rows.getJSONArray(index);
				String b64 		= row.getString(0);
				T element		= (T)ObjectIO.decodeObjectFromB64(b64);
				index++;
				return b64 != null ? element : null ;
			} catch (Exception e) {
				e.printStackTrace();
				throw new NoSuchElementException(String.format("%d", index));
			}
		}

		@Override
		public void remove() {
			rows.remove(index);
		}
	}
	
	@Override
	public Iterator<E> iterator() {
		return new DBListIterator<E>();
	}

	@Override
	public int lastIndexOf(Object o) {
		throw new RuntimeException("Method lastIndexOf(Object o) not implemented.");
	}

	@Override
	public ListIterator<E> listIterator() {
		throw new RuntimeException("Method ListIterator<E> listIterator() not implemented.");
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		throw new RuntimeException("Method ListIterator<E> listIterator(int index) not implemented.");
	}

	@Override
	public boolean remove(Object o) {
		try {
			String b64 	= ObjectIO.encodeObjectAsB64(o);
			String SQL 	= String.format("SELECT listIndex FROM %s WHERE %s AND listValue = '%s'", DB_TABLENAME, getDefaultSQLCondition(), b64);
			Object idx	= dbQuery("remove()", SQL, 0, 0);
			if ( idx == null) {
				return false;
			}
			String SQL1 = String.format("DELETE FROM %s WHERE %s AND listValue = '%s'", DB_TABLENAME, getDefaultSQLCondition(), b64);
			boolean ret = dbUpdate("remove", SQL1, false);

			// shift subsequent elements?
			shiftSubsequentItems(Integer.parseInt(idx.toString()));
			return ret;
		} catch (Exception e) {
			log.error("remove()", e);
		}
		return false;
	}

	@Override
	public E remove(int index) {
		E elem 		= get(index);
		String SQL 	= String.format("DELETE FROM %s WHERE %s AND listIndex = %d", DB_TABLENAME, getDefaultSQLCondition(), index);
		dbUpdate("remove()", SQL, false);
		shiftSubsequentItems(index);
		return elem;
	}

	/**
	 * Shift any subsequent elements to the left (subtracts one from their indices).
	 * @param index Start index.
	 */
	private void shiftSubsequentItems (int index) {
		String SQL1 = String.format("UPDATE %s SET listIndex = listIndex - 1 WHERE %s AND listIndex > %d", DB_TABLENAME, getDefaultSQLCondition(), index);
		dbUpdate("remove()", SQL1, false);
	}
	
	@Override
	public boolean removeAll(Collection<?> c) {
		throw new RuntimeException("Method removeAll(Collection<?> c) not implemented.");	
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new RuntimeException("Method retainAll(Collection<?> c) not implemented.");
	}

	@Override
	public E set(int index, E element) {
		/**
		 * Replaces the element at the specified position in this list with the specified element (optional operation).
		 * Parameters:
		 * index - index of the element to replace
		 * element - element to be stored at the specified position 
		 * Returns:  the element previously at the specified position 
		 */
		try {
			E old 		= get(index);
			String b64 	= ObjectIO.encodeObjectAsB64(element);
			String SQL 	= String.format("UPDATE %s SET listValue = '%s' WHERE listIndex = %d", DB_TABLENAME, b64, index);
			dbUpdate("set(index,element) @ " + index, SQL, true);
			return old;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int size() {
		String SQL = String.format("SELECT listValue FROM %s WHERE %s", DB_TABLENAME, getDefaultSQLCondition());
		try {
			JDBCResultSet rs = db.query(SQL);
			JSONArray rows	 = rs.getResultSet();
			return rows.length();
		} 
		catch (Exception e) {
			log.error("Size() " + name + "/" + nodeGroup , e);
		}
		return 0;
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		throw new RuntimeException("Method List<E> subList(int fromIndex, int toIndex) not implemented.");
	}

	@Override
	public Object[] toArray() {
		throw new RuntimeException("Method Object[] toArray() not implemented.");
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new RuntimeException("Method <T> T[] toArray(T[] a) not implemented.");
	}

}
