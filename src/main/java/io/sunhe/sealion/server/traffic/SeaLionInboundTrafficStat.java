package io.sunhe.sealion.server.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;

/**
 * Sample the inbound traffic stat.
 * 
 * Thread-safe.
 * 
 * @author sunhe
 * @date 2015年4月16日 下午6:53:58
 */
@Sharable
public class SeaLionInboundTrafficStat extends ChannelInboundHandlerAdapter {

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
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof ByteBuf) {
			increTrafficStat(((ByteBuf) msg).readableBytes());
		}
		ctx.fireChannelRead(msg);
	}
	
}
