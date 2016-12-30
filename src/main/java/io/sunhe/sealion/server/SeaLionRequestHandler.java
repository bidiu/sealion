package io.sunhe.sealion.server;

import java.util.concurrent.BlockingQueue;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.group.ChannelGroup;
import io.sunhe.sealion.protocol.Commands;
import io.sunhe.sealion.protocol.SeaLionRequest;
import io.sunhe.sealion.util.Logger;

/**
 * Processing the SeanLion request POJO.
 * Thread-safe
 * 
 * @author sunhe
 * @date 2015年3月15日 上午8:34:05
 */
@Sharable
public class SeaLionRequestHandler extends ChannelInboundHandlerAdapter {
	
	/**
	 * Worker task queue
	 */
	private BlockingQueue<Task> workerTaskQueue;
	
	/**
	 * Monitor task queue
	 */
	private BlockingQueue<Task> monitorTaskQueue;
	
	/**
	 * Hold all active channels (client channels that server channel accepts).
	 * Used mainly for determining the current connection number.
	 */
	private ChannelGroup channelGroup;
	
	public SeaLionRequestHandler(BlockingQueue<Task> taskQueue, BlockingQueue<Task> monitorTaskQueue, 
			ChannelGroup channelGroup) {
		this.workerTaskQueue = taskQueue;
		this.monitorTaskQueue = monitorTaskQueue;
		this.channelGroup = channelGroup;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		channelGroup.add(ctx.channel());
		Logger.log(ctx.channel().remoteAddress().toString(), "Connection established");
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof SeaLionRequest) {
			SeaLionRequest request = (SeaLionRequest) msg;
			if (Commands.isMonitorCommand(request.getCommand())) {
				monitorTaskQueue.offer(new Task(request, ctx.channel()));
			}
			else {
				workerTaskQueue.offer(new Task(request, ctx.channel()));
			}
		}
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Logger.log(ctx.channel().remoteAddress().toString(), "Connecton broken");
		ctx.channel().close();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
	}
	
}
