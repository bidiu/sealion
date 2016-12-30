package io.sunhe.sealion.pr;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents the console parameters of shell client.
 * 
 * Note that after instantiated the field of this class may be null, 
 * signifying that the parameter's value is not given. 
 * This class will check the basic validation of the console parameters.
 * 
 * Thread-safe.
 * 
 * @author sunhe
 * @date 2015年5月28日 上午10:03:29
 */
public class PerformanceRunnerParams {

	/**
	 * --hitRatio. Hit ratio (0 ~ 100)
	 */
	private int hitRatio = 100;
	
	/**
	 * --readRatio. (0~100)
	 */
	private int readRatio = 0;
	
	/**
	 * --mode. safe/unsafe
	 */
	private String mode = "safe";
	
	/**
	 * --size. Data size in byte unit.
	 */
	private int size = 1024;
	
	/**
	 * --connNum. Connection number.
	 */
	private int conn = 1;
	
	/**
	 * --thread. Thread number per connection.
	 */
	private int thread = 4;
	
	/**
	 * --help.
	 */
	private String help;
	
	/**
	 * --file
	 */
	private String file;
	
	/**
	 * --expiry. Expiry in minute unit.
	 */
	private int expiry = 0;
	
	
	/**
	 * @author sunhe
	 * @date 2015年5月28日 上午10:34:54
	 */
	public PerformanceRunnerParams(String[] args) {
		if (args.length % 2 != 0) {
			if (args.length == 1 && ("-h".equals(args[0]) || "--help".equals(args[0]))) {
				// --help or -h
				help = "help";
				return;
			}
			else {
				throwException();
			}
		}
		for (int i = 0; i < args.length; i += 2) {
			String paramName = args[i];
			String paramValue = args[i+1];
			if ("--hitRatio".equals(paramName)) {
				// --hitRatio
				if (isRatio(paramValue)) {
					hitRatio = Integer.parseInt(paramValue);
				}
				else {
					throwException();
				}
			}
			else if ("--readRatio".equals(paramName)) {
				// --readRatio
				if (isRatio(paramValue)) {
					readRatio = Integer.parseInt(paramValue);
				}
				else {
					throwException();
				}
			}
			else if ("--mode".equals(paramName)) {
				// --mode
				if ("safe".equals(paramValue) || "unsafe".equals(paramValue)) {
					mode = paramValue;
				}
				else {
					throwException();
				}
			}
			else if ("--size".equals(paramName)) {
				// --size
				if (isNumeric(paramValue)) {
					size = Integer.parseInt(paramValue);
				}
				else {
					throwException();
				}
			}
			else if ("--conn".equals(paramName)) {
				// --conn
				if (isNumeric(paramValue)) {
					conn = Integer.parseInt(paramValue);
				}
				else {
					throwException();
				}
			}
			else if ("--thread".equals(paramName)) {
				// --thread
				if (isNumeric(paramValue)) {
					thread = Integer.parseInt(paramValue);
				}
				else {
					throwException();
				}
			}
			else if ("--file".equals(paramName) || "-f".equals(paramName)) {
				// --file or -f
				file = paramValue;
			}
			else if ("--expiry".equals(paramName)) {
				// --expiry
				if (isNumeric(paramValue)) {
					expiry = Integer.parseInt(paramValue);
				}
				else {
					throwException();
				}
			}
			else {
				throwException();
			}
		}
	}
	
	/**
	 * Must be integer.
	 * 
	 * @param str
	 * @return
	 * @author sunhe
	 * @date 2015年5月28日 下午1:13:53
	 */
	private boolean isNumeric(String str) {
		return StringUtils.isNumeric(str);
	}
	
	/**
	 * Must be an integer (0 ~ 100).
	 * 
	 * @param str
	 * @return
	 * @author sunhe
	 * @date 2015年5月28日 下午1:14:07
	 */
	private boolean isRatio(String str) {
		if (isNumeric(str)) {
			int num = Integer.parseInt(str);
			return num >= 0 && num <= 100;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Throw the IllegalArgumentException exception.
	 * 
	 * @author sunhe
	 * @date 2015年5月3日 下午10:34:41
	 */
	private void throwException() {
		throw new IllegalArgumentException("The given console paramters are invalid");
	}

	public int getHitRatio() {
		return hitRatio;
	}

	public int getReadRatio() {
		return readRatio;
	}

	public String getMode() {
		return mode;
	}

	public int getSize() {
		return size;
	}

	public int getConn() {
		return conn;
	}

	public int getThread() {
		return thread;
	}
	
	public String getHelp() {
		return help;
	}
	
	public String getFile() {
		return file;
	}
	
	public int getExpiry() {
		return expiry;
	}
	
}
