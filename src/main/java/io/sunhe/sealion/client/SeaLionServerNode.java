package io.sunhe.sealion.client;

import io.netty.channel.Channel;
import io.sunhe.sealion.server.SeaLionServer;

import java.net.InetSocketAddress;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents a single SeaLion server node in the cluster.
 * Should be thread-safe by means of making this object a invariable one.
 * 
 * @author sunhe
 * @date 2015年3月18日 下午3:04:46
 */
public class SeaLionServerNode implements Comparable<SeaLionServerNode> {
	
	/**
	 * Once assigned, cann't be changed.
	 */
	// Log weight
	public static final int WEIGHT_LOW = 0;
	// Normal and default weight
	public static final int WEIGHT_NORMAL = 1;
	// High weight
	public static final int WEIGHT_HIGH = 2;
	
	/**
	 * The socket address of the server node.
	 * Cann't be changed.
	 */
	private InetSocketAddress socketAddress;
	
	/**
	 * The socket channel associated with the socket address.
	 */
	private volatile Channel channel;
	
	/**
	 * Should be WEIGHT_LOW or WEIGHT_NORMAL or WEIGHT_HIGH.
	 * Cann't be changed.
	 */
	private int weight;
	
	/**
	 * @param strSocketAddress 
	 * 				The socket address in string format, the format should
	 * 				be (host|ip)[:port][:weight]. the weight is the server's 
	 * 				weight, user can configured it according the server's capability
	 * 				or something else.
	 * In order to assign the weight, you must assign the port in the same time.
	 * 
	 * @author sunhe
	 * @date 2015年3月18日 下午3:42:53
	 */
	public SeaLionServerNode(String strSocketAddress) {
		boolean isValidSocketAddress = true;
		String host = null;
		int port = 0, weight = 0;
		
		String[] strs = strSocketAddress.split(":");
		if (strs.length > 3) {
			isValidSocketAddress = false;
		}
		if (strs.length == 1) {
			host = strs[0];
			port = SeaLionServer.DEFAULT_PORT;
			weight = WEIGHT_NORMAL;
		}
		else if (strs.length == 2) {
			if (StringUtils.isNumeric(strs[1])) {
				host = strs[0];
				port = Integer.parseInt(strs[1]);
				weight = WEIGHT_NORMAL;
			}
			else {
				isValidSocketAddress = false;
			}
		}
		else {
			if (StringUtils.isNumeric(strs[1]) && StringUtils.isNumeric(strs[2])) {
				host = strs[0];
				port = Integer.parseInt(strs[1]);
				weight = Integer.parseInt(strs[2]);
				if (weight != WEIGHT_LOW && weight != WEIGHT_NORMAL && weight != WEIGHT_HIGH) {
					weight = WEIGHT_NORMAL;
				}
			}
			else {
				isValidSocketAddress = false;
			}
		}
		if (isValidSocketAddress) {
			socketAddress = new InetSocketAddress(host, port);
			this.weight = weight;
		}
		else {
			throw new IllegalArgumentException(
					"Invalid socket address: " + strSocketAddress + ", the format should be: (host|ip)[:port][:weight]");
		}
	}
	
	public SeaLionServerNode(String host, int port) {
		this (host, port, WEIGHT_NORMAL);
	}
	
	public SeaLionServerNode(String host, int port, int weight) {
		if (weight != WEIGHT_LOW && weight != WEIGHT_NORMAL && weight != WEIGHT_HIGH) {
			throw new IllegalArgumentException("Invalid weight value: " + weight);
		}
		else {
			socketAddress = new InetSocketAddress(host, port);
			this.weight = weight;
		}
	}
	
	public SeaLionServerNode(InetSocketAddress socketAddress, int weight) {
		if (weight != WEIGHT_LOW && weight != WEIGHT_NORMAL && weight != WEIGHT_HIGH) {
			throw new IllegalArgumentException("Invalid weight value: " + weight);
		}
		else {
			this.socketAddress = socketAddress;
			this.weight = weight;
		}
	}
	
	/**
	 * 
	 * @return A copy of the socket address
	 * @author sunhe
	 * @date 2015年3月18日 下午10:13:10
	 */
	public InetSocketAddress getSocketAddress() {
		return socketAddress;
	}
	
	/**
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年3月19日 下午1:29:52
	 */
	public int getWeight() {
		return weight;
	}
	
	/**
	 * 
	 * @param channel
	 * @author sunhe
	 * @date 2015年3月18日 下午10:15:31
	 */
	public synchronized void setChannel(Channel channel) {
		this.channel = channel;
	}
	
	/**
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年3月18日 下午10:15:38
	 */
	public Channel getChannel() {
		return channel;
	}
	
	/**
	 * Get the socket address of this server node in string format.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年3月19日 下午2:03:25
	 */
	public String getStrSocketAddress() {
		return socketAddress.getHostName() + ":" + socketAddress.getPort();
	}
	
	@Override
	public String toString() {
		return getStrSocketAddress();
	}

	public int compareTo(SeaLionServerNode o) {
		return getStrSocketAddress().compareTo(o.getStrSocketAddress());
	}
	
	/**
	 * When two server have same socket address, then suppose they are equal.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SeaLionServerNode) {
			SeaLionServerNode other = (SeaLionServerNode) obj;
			return getStrSocketAddress().equals(other.getStrSocketAddress());
		}
		else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		// TODO Because the class has override the equals(), 
		// it should also provide its own hashCode() implementation.
		return super.hashCode();
	}
	
}
