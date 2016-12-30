package io.sunhe.sealion.server.dashboard;

import io.netty.channel.group.ChannelGroup;
import io.sunhe.sealion.server.Task;
import io.sunhe.sealion.server.mem.MapContainer;
import io.sunhe.sealion.server.traffic.SeaLionInboundTrafficStat;
import io.sunhe.sealion.server.traffic.SeaLionOutboundTrafficStat;

import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

import java.util.concurrent.BlockingQueue;

/**
 * SeaLion server instance monitor.
 * Monitor a set of status of a SeaLion server instance.
 * 
 * Note that one SeaLion server instance should have and only have 
 * ONE SeaLion server monitor.
 * 
 * Should be thread-safe.
 * 
 * @author sunhe
 * @date 2015年4月14日 上午11:54:28
 */
@SuppressWarnings("restriction")
public class SeaLionServerMonitor {
	
	private BlockingQueue<Task> workerTaskQueue;
	
	private MapContainer mapContainer;
	
	private ChannelGroup channelGroup;
	
	private SeaLionInboundTrafficStat inboundTrafficStat;
	
	private SeaLionOutboundTrafficStat outboundTrafficStat;
	
	private HitRatioCollector hitRatioCollector;
	
	private BlockingTimeAccumulator blockingTimeAccumulator;
	
	private OperatingSystemMXBean osMXBean;
	
	public SeaLionServerMonitor(BlockingQueue<Task> workerTaskQueue, MapContainer mapContainer, 
			ChannelGroup channelGroup, SeaLionInboundTrafficStat inboundTrafficStat, 
			SeaLionOutboundTrafficStat outboundTrafficStat, HitRatioCollector hitRatioCollector, 
			BlockingTimeAccumulator blockingTimeAccumulator) {
		this.workerTaskQueue = workerTaskQueue;
		this.mapContainer = mapContainer;
		this.channelGroup = channelGroup;
		this.inboundTrafficStat = inboundTrafficStat;
		this.outboundTrafficStat = outboundTrafficStat;
		this.hitRatioCollector = hitRatioCollector;
		this.blockingTimeAccumulator = blockingTimeAccumulator;
		osMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
	}
	
	public double getMemPercentage() {
		return mapContainer.getMemPercentage();
	}
	
	public long getMemUsage() {
		return mapContainer.getCurMemSize();
	}
	
	public long getMemLimit() {
		return mapContainer.getMaxMemSize();
	}
	
	public int getTaskQueueSize() {
		return workerTaskQueue.size();
	}
	
	public int getNumOfConn() {
		return channelGroup.size();
	}
	
	/**
	 * Note that only support dashboard, not including monitor interface.
	 * Get the inbound traffic throughput and then clear it.
	 * 
	 * @return The inbound traffic throughput in byte unit.
	 * @author sunhe
	 * @date 2015年4月16日 下午7:06:06
	 */
	public synchronized int getAndClearInboundTrafficStat() {
		int trafficStat = inboundTrafficStat.getTrafficStat();
		inboundTrafficStat.clearTrafficStat();
		return trafficStat;
	}
	
	/**
	 * Note that only support dashboard, not including monitor interface.
	 * Get the outbound traffic throughput and then clear it.
	 * 
	 * @return The outbound traffic throughput in byte unit.
	 * @author sunhe
	 * @date 2015年4月16日 下午4:23:32
	 */
	public synchronized int getAndClearOutboundTrafficStat() {
		int trafficStat = outboundTrafficStat.getTrafficStat();
		outboundTrafficStat.clearTrafficStat();
		return trafficStat;
	}
	
	/**
	 * Get and clear the hit ratio.
	 * 
	 * @return Hit ratio (0.0 ~ 1.0).
	 * @author sunhe
	 * @date 2015年4月16日 下午7:45:55
	 */
	public synchronized double getAndClearHitRatio() {
		double hitRatio = hitRatioCollector.getHitRatio();
		hitRatioCollector.clearRecords();
		return hitRatio;
	}
	
	/**
	 * @return In second unit.
	 * @author sunhe
	 * @date 2015年4月16日 下午10:26:16
	 */
	public int getBlockingTime() {
		return blockingTimeAccumulator.getBlockingTime();
	}
	
	/**
	 * Get current CPU usage
	 * 
	 * @return CPU usage in percentage.
	 * @author sunhe
	 * @date 2015年6月5日 下午2:58:23
	 */
	public int getCpuUsage() {
		return (int) (osMXBean.getProcessCpuLoad() * 100);
	}
	
}
