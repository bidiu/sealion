package io.sunhe.sealion.server.dashboard;

import com.alibaba.fastjson.JSON;

import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.sunhe.sealion.util.TimeStampFormatter;
import io.sunhe.sealion.util.UnitConverter;

/**
 * Dashboard task will be executed by the same executor which executes 
 * the monitor task too.
 * 
 * Not thread-safe. It's okay because only the thread to bootstrap the SeaLion server 
 * passes this Runnable-implementation class to the monitor executor, which is a single 
 * thread executor.
 * 
 * The main duty of dashboard task is to broadcast the status of SeaLion server 
 * periodically through the established WebSocket connection, which are stored in 
 * the ChannelPipeline (dbChannelPipeline, db is the shorthand of dashboard).
 * 
 * @author sunhe
 * @date 2015年4月14日 下午9:06:32
 */
public class DashboardTask implements Runnable {
	
	/**
	 * Default 5s.
	 */
	private static final int DEFAULT_INTERVAL_SECOND = 5;

	private ChannelGroup dbChannelGroup;
	
	private SeaLionServerMonitor serverMonitor;
	
	/**
	 * The interval time length in second unite 
	 * between each broadcast.
	 */
	private int intervalSecond;
	
	/**
	 * The SeaLion status data to be broadcasted.
	 */
	private StatusData data;
	
	public DashboardTask(ChannelGroup dbChannelGroup, SeaLionServerMonitor serverMonitor) {
		this (dbChannelGroup, serverMonitor, DEFAULT_INTERVAL_SECOND);
	}
	
	/**
	 * @param dbChannelGroup
	 * @param serverMonitor
	 * @param intervalSecond
	 * @author sunhe
	 * @date 2015年4月15日 下午1:36:52
	 */
	public DashboardTask(ChannelGroup dbChannelGroup, SeaLionServerMonitor serverMonitor, int intervalSecond) {
		this.dbChannelGroup = dbChannelGroup;
		this.serverMonitor = serverMonitor;
		this.intervalSecond = intervalSecond;
		data = new StatusData();
	}
	
	public void run() {
		while (true) {
			if (dbChannelGroup.size() != 0) {
				broadcastStatus();
			}
			try {
				Thread.sleep(intervalSecond * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @author sunhe
	 * @date 2015年4月14日 下午9:19:35
	 */
	public void broadcastStatus() {
		data.setTimeStamp(TimeStampFormatter.getCurrentTimeStamp());
		data.setMemLimit(UnitConverter.fromByteToMegabyte(serverMonitor.getMemLimit()));
		data.setMemPercentage(UnitConverter.fromFloatToPercentage(serverMonitor.getMemPercentage()));
		data.setMemUsage(UnitConverter.fromByteToMegabyte(serverMonitor.getMemUsage()));
		data.setNumOfConn(serverMonitor.getNumOfConn());
		data.setTaskQueueSize(serverMonitor.getTaskQueueSize());
		// unit - Byte/s
		data.setNetworkOut(serverMonitor.getAndClearOutboundTrafficStat() / intervalSecond);
		// unit - Byte/s
		data.setNetworkIn(serverMonitor.getAndClearInboundTrafficStat() / intervalSecond);
		// unit - %
		data.setHitRatio(UnitConverter.fromFloatToPercentage(serverMonitor.getAndClearHitRatio()));
		// unit - s
		data.setBlockingTime(serverMonitor.getBlockingTime());
		// unit - %
		data.setCpuUsage(serverMonitor.getCpuUsage());
		writeToChannelGroup(data);
	}
	
	/**
	 * @param data
	 * @author sunhe
	 * @date 2015年4月14日 下午10:29:57
	 */
	public void writeToChannelGroup(StatusData data) {
		dbChannelGroup.writeAndFlush(new TextWebSocketFrame(JSON.toJSONString(data)));
	}

}
