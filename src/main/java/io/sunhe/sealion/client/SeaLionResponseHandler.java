package io.sunhe.sealion.client;

import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import io.sunhe.sealion.client.hash.HashBalancer;
import io.sunhe.sealion.protocol.SeaLionResponse;
import io.sunhe.sealion.util.Logger;

/**
 * Process the SeaLion response POJO.
 * Thread-safe
 * 
 * @author sunhe
 * @date 2015年3月15日 下午6:47:55
 */
@Sharable
public class SeaLionResponseHandler extends ChannelInboundHandlerAdapter {

	private ConcurrentHashMap<String, Long> opaqueToThreadId;
	
	private ConcurrentHashMap<Long, BlockingQueue<SeaLionResponse>> threadIdToResponseQueue;
	
	private volatile HashBalancer hashBalancer;
	
	public SeaLionResponseHandler(ConcurrentHashMap<String, Long> opaqueToThreadId, 
			ConcurrentHashMap<Long, BlockingQueue<SeaLionResponse>> threadIdToResponseQueue) {
		this.opaqueToThreadId = opaqueToThreadId;
		this.threadIdToResponseQueue = threadIdToResponseQueue;
	}
	
	public synchronized void setHashBalancer(HashBalancer hashBalancer) {
		this.hashBalancer = hashBalancer;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Logger.log(ctx.channel().remoteAddress().toString(), "Connection established");
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof SeaLionResponse) {
			try {
				SeaLionResponse response = (SeaLionResponse) msg;
				String opaque = response.getOpaque();
				Long threadId = opaqueToThreadId.get(opaque);
				if (threadId == null) {
					throw new IllegalStateException("Received a response that no thread'll process. Opaque: " + opaque);
				}
				BlockingQueue<SeaLionResponse> responseQueue = threadIdToResponseQueue.get(threadId);
				responseQueue.offer(response);
			}
			catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Logger.log(ctx.channel().remoteAddress().toString(), "Connecton broken");
		SeaLionServerNode node = new SeaLionServerNode(
				(InetSocketAddress) ctx.channel().remoteAddress(), SeaLionServerNode.WEIGHT_NORMAL);
		node = hashBalancer.getServerNodes().get(node.getStrSocketAddress());
		if (node != null) {
			hashBalancer.removeServerNode(node);
		}
		ctx.channel().close();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
	}
	
}
