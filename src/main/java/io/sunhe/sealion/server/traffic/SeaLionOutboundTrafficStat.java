package io.sunhe.sealion.server.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandler.Sharable;

/**
 * Sample the outbound traffic stat.
 * 
 * Thread-safe.
 * 
 * @author sunhe
 * @date 2015年4月16日 下午4:05:16
 */
@Sharable
public class SeaLionOutboundTrafficStat extends ChannelOutboundHandlerAdapter {

	/**
	 * The traffic data size in byte
	 */
	private int trafficStat;
	
	/**
	 * Increment the traffic stat.
	 * 
	 * @param increment
	 * @author sunhe
	 * @date 2015年4月16日 下午4:04:50
	 */
	private synchronized void increTrafficStat(int increment) {
		trafficStat += increment;
	}
	
	/**
	 * Get the traffic stat.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年4月16日 下午4:05:10
	 */
	public synchronized int getTrafficStat() {
		return trafficStat;
	}
	
	/**
	 * Clear the traffic stat.
	 * 
	 * @author sunhe
	 * @date 2015年4月16日 下午4:05:59
	 */
	public synchronized void clearTrafficStat() {
		trafficStat = 0;
	}
	
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof ByteBuf) {
			increTrafficStat(((ByteBuf) msg).readableBytes());
		}
		ctx.write(msg, promise);
	}
	
}
