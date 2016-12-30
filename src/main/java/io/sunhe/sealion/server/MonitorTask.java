package io.sunhe.sealion.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.sunhe.sealion.protocol.Commands;
import io.sunhe.sealion.protocol.SeaLionRequest;
import io.sunhe.sealion.protocol.SeaLionResponse;
import io.sunhe.sealion.protocol.Statuses;
import io.sunhe.sealion.server.dashboard.SeaLionServerMonitor;
import io.sunhe.sealion.util.Logger;

import java.util.concurrent.BlockingQueue;

/**
 * The task monitor thread execute this task.
 * Note that monitor thread should be a single-thread pool, 
 * so monitor thread must not be blocked.
 * 
 * Not thread-safe. It's okay because only the thread to bootstrap the SeaLion server 
 * passes this Runnable-implementation class to the monitor executor, which is a single 
 * thread executor.
 * 
 * Note that all monitor task are different from worker task - monitor tasks don't have pooled 
 * byte buffer resources to release, they are all in safe mode and no CAS value check.
 * 
 * Zero-byte-size (the maximum size, not initial size is zero) pooled byte buffer seems 
 * not to be needed to release(for example the DELETE command etc).
 * 
 * @author sunhe
 * @date 2015年4月10日 下午2:33:50
 */
public class MonitorTask implements Runnable {
	
	private BlockingQueue<Task> monitorTaskQueue;
	
	private SeaLionServerMonitor serverMonitor;
	
	public MonitorTask(SeaLionServerMonitor serverMonitor, BlockingQueue<Task> monitorTaskQueue) {
		this.serverMonitor = serverMonitor;
		this.monitorTaskQueue = monitorTaskQueue;
	}
	
	public void run() {
		try {
			while (true) {
				Task task = monitorTaskQueue.take();
				processTask(task);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	/**
	 * @param task
	 * @author sunhe
	 * @date 2015年4月10日 下午8:57:42
	 */
	public void processTask(Task task) {
		SeaLionRequest request = task.getSeaLionRequest();
		Channel channel = task.getChannel();
		Logger.log(channel.remoteAddress().toString() + " Processing request", request.toString());
		
		String command = request.getCommand();
		boolean isSafeMode = request.getIsSafeMode();
		SeaLionResponse response = new SeaLionResponse();
		response.setOpaque(request.getOpaque());
		
		if (Commands.MEM_PERCENTAGE.equals(command)) {
			// MEM_PERCENTAGE command.
			response.setStatus(Statuses.SUCCESS);
			response.setData(Unpooled.copyDouble(serverMonitor.getMemPercentage()));
			response.setDataLen(response.getData().capacity());
		}
		else if (Commands.MEM_USAGE.equals(command)) {
			// MEM_USAGE command.
			response.setStatus(Statuses.SUCCESS);
			response.setDataLen(8);
			response.setData(Unpooled.copyLong(serverMonitor.getMemUsage()));
		}
		else if (Commands.MEM_LIMIT.equals(command)) {
			// MEM_LIMIT command.
			response.setStatus(Statuses.SUCCESS);
			response.setDataLen(8);
			response.setData(Unpooled.copyLong(serverMonitor.getMemLimit()));
		}
		else if (Commands.TASK_QUEUE_SIZE.equals(command)) {
			// TASK_QUEUE_SIZE command.
			response.setStatus(Statuses.SUCCESS);
			response.setDataLen(4);
			response.setData(Unpooled.copyInt(serverMonitor.getTaskQueueSize()));
		}
		else if (Commands.NUM_CONN.equals(command)) {
			// NUM_CONN command.
			response.setStatus(Statuses.SUCCESS);
			response.setDataLen(4);
			response.setData(Unpooled.copyInt(serverMonitor.getNumOfConn()));
		}
		else {
			response.setStatus(Statuses.BAD_REQUEST);
			response.setDataLen(0);
			response.setData(Unpooled.EMPTY_BUFFER);
		}
		if (isSafeMode) {
			channel.writeAndFlush(response);
		}
	}

}
