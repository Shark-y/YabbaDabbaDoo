package com.cloud.cluster.zeroconf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.cloud.cluster.multicast.ZeroConfDiscovery;
import com.cloud.cluster.multicast.ZeroDescriptorObject;
import com.cloud.cluster.multicast.ZeroConfDiscovery.MessageType;
import com.cloud.cluster.multicast.ZeroDescriptorObject.UpdateType;
import com.cloud.cluster.zeroconf.ZIO;
import com.cloud.core.io.ObjectCache;
import com.cloud.core.io.ObjectCache.ObjectType;
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
public class ZList<E> implements List<E> {

	static final Logger log 					= LogManager.getLogger(ZList.class);
	
	private String 		name;
	
	private ZeroConfDiscovery ds;
//	private ObjectCache cache;
	private List<E> list;
	
	/**
	 * Construct
	 * @param name Cluster list name.
	 * @param nodeGroup Cluster group name.
	 */
	public ZList(ObjectCache cache, ZeroConfDiscovery ds, String name/*, String nodeGroup*/) {
		super();
		this.ds 	= ds;
		this.name	= name;
//		this.cache	= cache;
		
		log.debug("Construct List Name: " + name);

		if ( cache.containsKey(name)) {
			list = (List<E>)cache.get(name).getObject();
		}
		else {
			list = new ArrayList<E>();
			cache.add(name, ObjectType.T_LIST, list);
			try {
				// replicate
				ds.sendAndQueue(ZeroDescriptorObject.create(MessageType.OBJECT_NEW, ObjectType.T_LIST, name));
			} catch (IOException e) {
				log.error("List.New(" + name + ")", e);
			}
		}		
	}

	@Override
	public boolean add(E e) {
		try {
			// replicate. This will add the element via UDP to all nodes including the sender.
			String b64 			= ZIO.encodeObject(e, true);
			ds.sendAndQueue(ZeroDescriptorObject.create(MessageType.OBJECT_UPDATE, UpdateType.ADD, name, null, b64));//e));
		} catch (IOException ex) {
			log.error("List.Add(" + name + ")", ex);
		}
		// Avoid duplicate additions return list.add(e);
		return true;
	}

	@Override
	public void add(int index, E element) {
		throw new RuntimeException("CLUSTER: Method add(int index, E element) not implemented.");
		/*try {
			// replicate
			String b64 			= ZIO.encodeObject(element, true);
			ds.sendAndQueue(ZeroDescriptorObject.create(MessageType.OBJECT_UPDATE, UpdateType.ADD, name, null, b64));
		} catch (IOException ex) {
			log.error("List.Add(" + name + ")", ex);
		}		
		list.add(index, element);*/
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
		try {
			// replicate
			ds.sendAndQueue(ZeroDescriptorObject.create(MessageType.OBJECT_UPDATE, UpdateType.CLEAR, name)); 
		} catch (IOException e) {
			log.error("List.Clear(" + name + ")", e);
		}
		list.clear();
	}

	@Override
	public boolean contains(Object o) {
		return list.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new RuntimeException("Method containsAll(Collection<?> c) not implemented.");
	}

	@Override
	public E get(int index) {
		E v = list.get(index);
		if ( v != null ) {
			try {
				// Unzip/Unserialize
				String ostr = ((Object)v).toString();
				if (ostr.startsWith(ZIO.ENC_OBJ_PREFIX) ) {
					return (E)ZIO.decodeObject(ostr, true);
				}
			} catch (Exception e) {
				log.error("List." + name + ".get(" + index + ")", e);
			}
		}
		return v;
	}

	@Override
	public int indexOf(Object o) {
		throw new RuntimeException("Method indexOf(Object o) not implemented.");
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}
	
	@Override
	public Iterator<E> iterator() {
		return list.iterator();
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
			// replicate
			ds.sendAndQueue(ZeroDescriptorObject.create(MessageType.OBJECT_UPDATE, UpdateType.REMOVE, name, null, o));
		} catch (IOException e) {
			log.error("Map.Remove(" + name + ")", e);
		}
		return list.remove(o);
	}

	@Override
	public E remove(int index) {
		log.error("CLUSTER: List.remove(int index) not implemented.");
		return list.remove(index);
	}

	
	@Override
	public boolean removeAll(Collection<?> c) {
		throw new RuntimeException("Method removeAll(Collection<?> c) not implemented.");	
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new RuntimeException("Method retainAll(Collection<?> c) not implemented.");
	}

	/**
	 * Replaces the element at the specified position in this list with the specified element (optional operation).
	 * Parameters:
	 * index - index of the element to replace
	 * element - element to be stored at the specified position 
	 * Returns:  the element previously at the specified position 
	 */
	@Override
	public E set(int index, E element) {
		log.error("CLUSTER: List.set(int index, E element) not implemented.");
		return list.set(index, element);
	}

	@Override
	public int size() {
		return list.size();
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

	@Override
	public String toString() {
		//return name + " = " + list;
		StringBuffer buf 	= new StringBuffer("[");
		boolean comma 		= false;
		for ( E elem : list) {
			if ( comma ) {
				buf.append(" , ");
			}
			if ( elem.toString().startsWith(ZIO.ENC_OBJ_PREFIX) ) {
				try {
					buf.append(ZIO.decodeObject(elem.toString()).toString());
				} catch (Exception e) {
					buf.append(elem.toString());
				}
			}
			else {
				buf.append(elem.toString());
			}
			comma = true;
		}
		buf.append("]");
		return buf.toString(); 
	}
}
