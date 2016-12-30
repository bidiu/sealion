package io.sunhe.sealion.server.dashboard;

/**
 * Blocking time accumulator.
 * Blocking time: the span time when worker thread operates 
 * LRU and TTL data flush.
 * 
 * Thread-safe.
 * 
 * @author sunhe
 * @date 2015年4月16日 下午9:45:25
 */
public class BlockingTimeAccumulator {
	
	/**
	 * In second unit.
	 */
	private int blockingTime;
	
	/**
	 * Increment blocking time by 1.
	 * 
	 * @author sunhe
	 * @date 2015年4月16日 下午9:48:52
	 */
	public synchronized void accumulate() {
		blockingTime++;
	}
	
	/**
	 * @return
	 * @author sunhe
	 * @date 2015年4月16日 下午9:50:46
	 */
	public synchronized int getBlockingTime() {
		return blockingTime;
	}
	
	/**
	 * @author sunhe
	 * @date 2015年4月16日 下午9:51:19
	 */
	public synchronized void clear() {
		blockingTime = 0;
	}
	
}
