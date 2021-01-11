package com.cloud.core.io;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

import com.cloud.core.io.IOTools;
import com.cloud.core.types.CoreTypes;

/**
 * ScriptExecutor using the default java scripting engine: <pre>
 *  ScriptExecutor s = new ScriptExecutor();
 *  String json = s.invokeFunction("foo.js", "main_method", "arg1", "arg2", "arg3");
 * </pre>
 * @author Administrator
 *
 */
public class JSScriptExecutor {

	private static final Logger log = Logger.getLogger(JSScriptExecutor.class);
	
	private ScriptEngine javascriptEngine;
	
	/**
	 * Initialize the JS script engine.
	 */
	public JSScriptExecutor() {
		ScriptEngineManager manager = new ScriptEngineManager();
		javascriptEngine 			= manager.getEngineByExtension("js");
	}
	
	/**
	 * Get the file name and function name and invoke it.
	 * @param fileName JS script full path.
	 * @param methodName Function to invoke within the script.
	 * @param args Comma separated array of arguments to pass to the script function.
	 * @return Function result.
	 * @throws FileNotFoundException 
	 * @throws ScriptException 
	 * @throws NoSuchMethodException 
	 */
	public Object invokeFunction(String fileName, String methodName, Object ... args) throws FileNotFoundException, ScriptException, NoSuchMethodException
	{
		log.debug("Invoking " + fileName + "@" + methodName);
		InputStream is = null;
		try {
			is = new FileInputStream(fileName);
			return invokeFunction(is, methodName, args);
		} finally {
			IOTools.closeStream(is);
		}
	}
	
	public Object invokeFunction(InputStream in, String methodName, Object ... args) throws FileNotFoundException, ScriptException, NoSuchMethodException
	{
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(in, CoreTypes.CHARSET_UTF8));
			javascriptEngine.eval(reader);
			Invocable invocableEngine = (Invocable)javascriptEngine;
			return invocableEngine.invokeFunction(methodName, args);
		}
		finally {
			IOTools.closeStream(reader);
		}
	}
	
}
