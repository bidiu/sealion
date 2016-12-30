package io.sunhe.sealion.server.dashboard;

/**
 * A frame of status data broadcasted to browser 
 * through WebSocket.
 * Will be converted JSON before write to channels.
 * 
 * @author sunhe
 * @date 2015年4月14日 下午10:30:40
 */
public class StatusData {
	
	private long timeStamp;
	
	/**
	 * From 0 to 100 (0% ~ 100%).
	 */
	private int memPercentage;
	
	/**
	 * Memory usage in megabyte unit.
	 */
	private int memUsage;
	
	/**
	 * In megabyte unit.
	 */
	private int memLimit;
	
	private int taskQueueSize;
	
	/**
	 * Number of connection to the SeaLion server.
	 */
	private int numOfConn;
	
	/**
	 * The inbound network traffic throughput in byte unit.
	 */
	private int networkIn;
	
	/**
	 * The outbound network traffic throughput in byte unit.
	 */
	private int networkOut;
	
	/**
	 * Percentage (0% ~ 100%).
	 */
	private int hitRatio;
	
	/**
	 * Blocking time: the time when worker thread are execute task 
	 * to remove expired (TTL) or old data (LRU). During this period 
	 * of time, the worker thread can't do any data manipulation task.
	 * 
	 * In second unit.
	 */
	private int blockingTime;
	
	/**
	 * CPU usage (0% ~ 100%)
	 */
	private int cpuUsage;
	
	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public int getMemPercentage() {
		return memPercentage;
	}

	public void setMemPercentage(int memPercentage) {
		this.memPercentage = memPercentage;
	}

	public int getMemUsage() {
		return memUsage;
	}

	public void setMemUsage(int memUsage) {
		this.memUsage = memUsage;
	}

	public int getMemLimit() {
		return memLimit;
	}

	public void setMemLimit(int memLimit) {
		this.memLimit = memLimit;
	}

	public int getTaskQueueSize() {
		return taskQueueSize;
	}

	public void setTaskQueueSize(int taskQueueSize) {
		this.taskQueueSize = taskQueueSize;
	}

	public int getNumOfConn() {
		return numOfConn;
	}

	public void setNumOfConn(int numOfConn) {
		this.numOfConn = numOfConn;
	}

	public int getNetworkIn() {
		return networkIn;
	}

	public void setNetworkIn(int networkIn) {
		this.networkIn = networkIn;
	}

	public int getNetworkOut() {
		return networkOut;
	}

	public void setNetworkOut(int networkOut) {
		this.networkOut = networkOut;
	}

	public int getHitRatio() {
		return hitRatio;
	}

	public void setHitRatio(int hitRatio) {
		this.hitRatio = hitRatio;
	}

	public int getBlockingTime() {
		return blockingTime;
	}

	public void setBlockingTime(int blockingTime) {
		this.blockingTime = blockingTime;
	}

	public int getCpuUsage() {
		return cpuUsage;
	}

	public void setCpuUsage(int cpuUsage) {
		this.cpuUsage = cpuUsage;
	}
	
}
