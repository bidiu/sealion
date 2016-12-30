package io.sunhe.sealion.server;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents the server boot parameters established 
 * with the given console parameters.
 * Note that after instantiated the field of this class may be null, 
 * signifying that the parameter's value is not given. 
 * This class will check the basic validation of the console parameters.
 * 
 * Thread-safe.
 * 
 * @author sunhe
 * @date 2015年5月3日 下午10:22:18
 */
public class SeaLionServerParams {
	
	/**
	 * -p port.
	 */
	private String p;
	
	/**
	 * -m max memory in MegaByte unit.
	 */
	private String m;
	
	/**
	 * @param args The console arguments.
	 * @author sunhe
	 * @date 2015年5月3日 下午10:28:57
	 */
	public SeaLionServerParams(String[] args) {
		if (args.length % 2 != 0) {
			throwException();
		}
		for (int i = 0; i < args.length; i += 2) {
			String paramName = args[i];
			String paramValue = args[i+1];
			if ("-p".equals(paramName)) {
				checkPostiveNumeric(paramValue);
				p = paramValue;
			}
			else if ("-m".equals(paramName)) {
				checkPostiveNumeric(paramValue);
				m = paramValue;
			}
			else {
				throwException();
			}
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
	
	/**
	 * Check if the parameter value is numeric, if so do nothing, 
	 * otherwise throw exception
	 * 
	 * @param paramValue
	 * @author sunhe
	 * @date 2015年5月3日 下午10:38:02
	 */
	private void checkPostiveNumeric(String paramValue) {
		if (! StringUtils.isNumeric(paramValue)) {
			throwException();
		}
	}

	public String getP() {
		return p;
	}

	public String getM() {
		return m;
	}
	
}
