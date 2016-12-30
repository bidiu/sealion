package io.sunhe.sealion.server.dashboard;

/**
 * Hit ratio collector.
 * Take CAS_FAILURE as hit.
 * 
 * Thread-safe.
 * 
 * @author sunhe
 * @date 2015年4月16日 下午7:28:02
 */
public class HitRatioCollector {

	private int hitTimes;
	
	private int totalTimes;
	
	public synchronized void hit() {
		hitTimes++;
		totalTimes++;
	}
	
	public synchronized void miss() {
		totalTimes++;
	}
	
	public synchronized double getHitRatio() {
		if (totalTimes == 0) {
			return 1.0;
		}
		else {
			return (double) hitTimes / (double) totalTimes;
		}
	}
	
	public synchronized void clearRecords() {
		hitTimes = totalTimes = 0;
	}
	
}
