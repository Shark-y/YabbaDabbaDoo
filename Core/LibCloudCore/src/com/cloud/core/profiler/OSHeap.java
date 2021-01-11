package com.cloud.core.profiler;

import java.io.File;
import java.util.Date;

import com.c1as.profiler.HeapDumper;
import com.cloud.core.provider.IHTMLFragment;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.Snapshot;
import com.sun.tools.hat.internal.parser.HprofReader;

public class OSHeap {

	private static long timerStart() {
		return System.currentTimeMillis();
	}

	private static void timerEnd(long start, String label) {
		LOGD(label + " "	+ (System.currentTimeMillis() - start) + " ms");
	}

	private static void LOGD(String text) {
		System.out.println("[HEAP] " + text);
	}

	@SuppressWarnings("unused")
	private static void LOGE(String text) {
		System.err.println("[HEAP] " + text);
	}
	
	/**
	 * Dump the heap. 
	 * @param fileName Path name to use (will delete if existing).
	 * @throws Exception
	 */
	/*
	private static void heapDump(String fileName) throws Exception {
		File f = new File(fileName);

		// must delete existing...
		if (f.exists()) {
			LOGD(fileName + " exists. Trying to delete.");
			if ( !f.delete() ) {
				LOGE("Unable to delete " + fileName);
				//throw new IOException("Unable to delete " + fileName);
			}
		}
		long t = timerStart();
		HeapDumper.dumpHeap(fileName);
		timerEnd(t, "Dump time:");
	} */

	/**
	 * Get a Heap dump as an {@link IHTMLFragment}.
	 * @return Heap dump HTML fragment (as an HTML TABLE).
	 */
	public static IHTMLFragment getOSHeap() throws Exception {
		// generate a temp file name for the heap dump.
		final String tmpDir 	= System.getProperty("java.io.tmpdir");
		//final String fileName 	= tmpDir + "heap.hprof";
		final StringBuffer html = new StringBuffer(); 
		
		// dump it to the temp dir: java.io.tmpdir
		//heapDump(fileName);
		String fileName = HeapDumper.dumpHeap(new File(tmpDir), true); 

		LOGD("Heap Dump @ " + fileName);

		// read it
		long t = timerStart();
		Snapshot snap = HprofReader.readFile(fileName, true, 0);
		timerEnd(t, "Read time:");

		// must resolve
		t = timerStart();
		snap.resolve(true);
		timerEnd(t, "Resolve time:");

		JavaClass[] classes = snap.getClassesArray();

		LOGD("Total Classes: " + classes.length);

		// Display
		t = timerStart();
		int i = 0;
		
		html.append("\n<b>Date:</b> " + new Date());
		html.append("\n<b>Total Classes:</b> " + classes.length);
		html.append("\n<p/>");
		html.append("\n<table id=\"tblHeap\" class=\"table\">");
		html.append("\n<thead><tr><th>#</th><th>Class Name</th><th>Instances</th><th>Size (bytes)</th></tr></thead>");
		html.append("\n<tbody>");
		
		for (JavaClass jClass : classes) {
			final int count = jClass.getInstancesCount(true);
			final long size = jClass.getTotalInstanceSize();

			// only display classes whose # of instances & size > 0 (filter junk)
			if (count > 0 && size > 0) {
				html.append("\n<tr><td>" + (i++) + "</td>"
						+ "<td>" + jClass.getName() + "</td>" 
						// FIXME The JS table sorter can't sort comma formatted numbers :( 
						+ "<td>" + count + "</td>"	// formatNumber(count)
						+ "<td>" + size + "</td>" 	// formatNumber(size)
						+ "</tr>");
			}
		}
		timerEnd(t, "Display time:");
		
		html.append("\n</tbody>");
		html.append("\n</table>");

		return new IHTMLFragment() {
			@Override
			public String getInnerHTML() {
				return html.toString();
			}
		};
	}
	
	/**
	 * Format a long as x,xxxx
	 * @param n
	 * @return long formatted as x,xxx
	 */
	static String formatNumber(long n) {
		return String.format("%,d%n",  n);
	}
	
}
