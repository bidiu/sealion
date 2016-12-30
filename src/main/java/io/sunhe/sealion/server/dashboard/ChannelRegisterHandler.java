package io.sunhe.sealion.server.dashboard;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.sunhe.sealion.util.Logger;

/**
 * This handler is totally irrelevant to the REGISTER state of a channel.
 * It just register the newly established WebSocket connection to the ChannelGroup, whose 
 * every single channel will be pushed dashboard data periodically.
 * 
 * Should be thread-safe.
 * 
 * @author sunhe
 * @date 2015年4月14日 下午4:29:52
 */
@Sharable
public class ChannelRegisterHandler extends ChannelInboundHandlerAdapter {
	
	/**
	 * WebSocket channel.
	 */
	private ChannelGroup channelGroup;
	
	/**
	 * @author sunhe
	 * @date 2015年4月14日 下午4:34:00
	 */
	public ChannelRegisterHandler(ChannelGroup channelGroup) {
		this.channelGroup = channelGroup;
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.netty.channel.ChannelInboundHandlerAdapter#userEventTriggered(io.netty.channel.ChannelHandlerContext, java.lang.Object)
	 * @author sunhe
	 * @date 2015年4月14日 下午8:55:15
	 */
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == WebSocketServerProtocolHandler.ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
        	Logger.log(ctx.channel().remoteAddress().toString(), "A WebSocket connection established");
        	channelGroup.add(ctx.channel());
        }
        else {
            super.userEventTriggered(ctx, evt);
        }
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.netty.channel.ChannelInboundHandlerAdapter#channelInactive(io.netty.channel.ChannelHandlerContext)
	 * @author sunhe
	 * @date 2015年4月14日 下午8:55:23
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		ctx.channel().close();
		super.channelInactive(ctx);
	}
	
}
