package io.sunhe.sealion.protocol;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains all command that SeaLion server supports.
 * 
 * @author sunhe
 * @date 2015年3月14日 下午5:46:49
 */
public class Commands {
	
	public static final String SET = "SET";
	
	public static final String GET = "GET";
	
	public static final String DELETE = "DELETE";
	
	/**
	 * This command is not as practical as it seems, 'cause the 
	 * GET command also return the CAS value coupled with the data.
	 */
	public static final String CAS = "CAS";
	
	/**
	 * Following is those commands used to monitor the state of SeaLion server.
	 */
	
	/**
	 * View the memory occupation percentage.
	 * = Current used memory(Just value storage) / Assigned maximum memory
	 */
	public static final String MEM_PERCENTAGE = "MEM_PERCENTAGE";
	
	public static final String MEM_USAGE = "MEM_USAGE";
	
	public static final String MEM_LIMIT = "MEM_LIMIT";
	
	public static final String TASK_QUEUE_SIZE = "TASK_QUEUE_SIZE";
	
	public static final String NUM_CONN = "NUM_CONN";
	
	public static Set<String> monitorCommandSet = new HashSet<String>();
	
	static {
		monitorCommandSet.add(MEM_PERCENTAGE);
		monitorCommandSet.add(MEM_USAGE);
		monitorCommandSet.add(MEM_LIMIT);
		monitorCommandSet.add(TASK_QUEUE_SIZE);
		monitorCommandSet.add(NUM_CONN);
	}
	
	public static boolean isMonitorCommand(String command) {
		return monitorCommandSet.contains(command);
	}
	
}
