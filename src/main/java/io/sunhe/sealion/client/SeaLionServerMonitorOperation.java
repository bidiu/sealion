package io.sunhe.sealion.client;

/**
 * Interface definition of monitoring SeaLion server's status.
 * All of those operations work in safe mode.
 * 
 * Note that you MUST open a new designated client, which donesn't 
 * do any data manipulation operation, to monitor the status of the 
 * SeaLion server.
 * 
 * And also note that with respect to monitor mode, One client can ONLY 
 * connect to ONE remote SeaLion server. in the future, I may will 
 * encapsulate those client(s) again with a SeaLionMonitorClient class, 
 * which sustains a set of SeaLionClient.
 * 
 * @author sunhe
 * @date 2015年4月10日 下午8:03:04
 */
public interface SeaLionServerMonitorOperation {

	/**
	 * Get the remote SeaLion server's memory usage percentage.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年4月10日 下午8:04:38
	 */
	public Double getMemPercentage();
	
	/**
	 * Get current remote SeaLion server's memory usage in byte unit.
	 * 
	 * @return The memory usage in byte unit. Null if some error occurs.
	 * @author sunhe
	 * @date 2015年4月10日 下午8:47:06
	 */
	public Long getMemUsage();
	
	/**
	 * Get remote server's memory size limit(the maximum memory size user assigned 
	 * SeaLion server can use).
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年4月10日 下午8:59:55
	 */
	public Long getMemLimit();
	
	/**
	 * Get current number of tasks in the task queue of the remote SeaLion server.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年4月10日 下午9:12:43
	 */
	public Integer getTaskQueueSize();
	
	/**
	 * Get the current number of opened connections.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年4月12日 下午7:32:53
	 */
	public Integer getNumOfConn();
	
}
