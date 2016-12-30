package io.sunhe.sealion.server;

import io.netty.channel.Channel;
import io.sunhe.sealion.protocol.SeaLionRequest;

/**
 * Represents a task in the task queue.
 * Immutable class, so it's thread-safe.
 * 
 * @author sunhe
 * @date 2015年3月15日 上午8:37:10
 */
public class Task {

	private SeaLionRequest request;
	
	/**
	 * The channel sending the request.
	 */
	private Channel channel;
	
	public Task(SeaLionRequest request, Channel channel) {
		this.request = request;
		this.channel = channel;
	}
	
	public SeaLionRequest getSeaLionRequest() {
		return request;
	}
	
	public Channel getChannel() {
		return channel;
	}
	
}
