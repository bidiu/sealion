package io.sunhe.sealion.util;

/**
 * Log helper utility class.
 * All log entries are logged to StdOut.
 * 
 * @author sunhe
 * @date 2015年3月14日 下午4:42:46
 */
public class Logger {

	/**
	 * 
	 * @param label
	 * @param detail
	 * @author sunhe
	 * @date 2015年3月14日 下午4:52:09
	 */
	public static void log(String label, String detail) {
		System.out.println("[" + label + "] " + detail);
	}
	
}
