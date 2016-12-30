package io.sunhe.sealion.protocol;

/**
 * Contains all possible status strings that server can return.
 * 
 * @author sunhe
 * @date 2015年3月14日 下午5:48:51
 */
public class Statuses {

	/**
	 * Operation is successful
	 */
	public static final String SUCCESS = "SUCCESS";
	
	/**
	 * Operation failed for some reason.
	 */
	public static final String FAILURE = "FAILURE";
	
	/**
	 * The request format is bad.
	 */
	public static final String BAD_REQUEST = "BAD_REQUEST";
	
	/**
	 * Server occurs error.
	 */
	public static final String SERVER_ERROR = "SERVER_ERROR";
	
	/**
	 * Server out of memory.
	 */
	public static final String SERVER_OUT_OF_MEMORY = "SERVER_OUT_OF_MEMORY";
	
	/**
	 * Key is not exists.
	 */
	public static final String KEY_NONEXISTS = "KEY_NONEXISTS";
	
	/**
	 * CAS authentication failure.
	 */
	public static final String CAS_FAILURE = "CAS_FAILURE";
	
}
