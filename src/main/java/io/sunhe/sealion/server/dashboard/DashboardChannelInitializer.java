package io.sunhe.sealion.server.dashboard;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

/**
 * @author sunhe
 * @date 2015年4月14日 下午1:45:30
 */
public class DashboardChannelInitializer extends ChannelInitializer<Channel> {
	
	private int dashboardPort;
	
	private ChannelRegisterHandler channelRegisterHandler;
	
	public DashboardChannelInitializer(int dashboardPort, ChannelRegisterHandler channelRegisterHandler) {
		this.dashboardPort = dashboardPort;
		this.channelRegisterHandler = channelRegisterHandler;
	}
	
	@Override
	protected void initChannel(Channel ch) throws Exception {
		ch.pipeline().addLast(new HttpRequestDecoder())
				.addLast(new HttpObjectAggregator(65536))
				.addLast(new HttpResponseEncoder())
				.addLast(new HttpRequestHandler("/ws", dashboardPort))
				.addLast(new WebSocketServerProtocolHandler("/ws"))
				.addLast(channelRegisterHandler);
	}

}
