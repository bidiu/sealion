package io.sunhe.sealion.client;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.sunhe.sealion.client.hash.HashBalancer;
import io.sunhe.sealion.protocol.Commands;
import io.sunhe.sealion.protocol.SeaLionRequest;
import io.sunhe.sealion.protocol.SeaLionRequestEncoder;
import io.sunhe.sealion.protocol.SeaLionResponse;
import io.sunhe.sealion.protocol.SeaLionResponseDecoder;
import io.sunhe.sealion.protocol.Statuses;
import io.sunhe.sealion.util.Charsets;
import io.sunhe.sealion.util.OpaqueGenerator;
import io.sunhe.sealion.util.TimeStampFormatter;

/**
 * SeaLion client
 * Should be thread-safe，used by multiple threads.
 * The client should be capable of maintaining multiple connection.
 * 
 * @author sunhe
 * @date 2015年3月15日 下午5:25:16
 */
public class SeaLionClient implements SeaLionClientOperation, SeaLionServerMonitorOperation {
	
	/**
	 * The timeout in second unit to wait for response. 
	 */
	private static final int RESPONSE_WAIT_TIMEOUT = 5;
	
	/**
	 * The following three fields is associated with a single channel
	 * For now, just keep the group channel store field in the SeaLionServerNode. 
	 */
	private EventLoopGroup group;
	
	/**
	 * Map opaque to thread-id.
	 * Will be accessed by multiple thread.
	 */
	private ConcurrentHashMap<String, Long> opaqueToThreadId;
	
	/**
	 * Map thread-id to response queue.
	 * Will be accessed by multiple thread.
	 */
	private ConcurrentHashMap<Long, BlockingQueue<SeaLionResponse>> threadIdToResponseQueue;
	
	private SeaLionResponseHandler responseHandler;
	
	private SeaLionRequestEncoder requestEncoder;
	
	private HashBalancer hashBalancer;
	
	/**
	 * Construct a SeaLion client through a single socket address(host and port). 
	 * 
	 * @param host
	 * @param port
	 * @throws InterruptedException
	 * @author sunhe
	 * @date 2015年3月19日 下午7:38:11
	 */
	public SeaLionClient(String host, int port) {
		init();
//		List<SeaLionServerNode> nodes = Collections.synchronizedList(new LinkedList<SeaLionServerNode>());
		ConcurrentHashMap<String, SeaLionServerNode> nodes = new ConcurrentHashMap<String, SeaLionServerNode>();
//		nodes.add(new SeaLionServerNode(host, port));
		SeaLionServerNode node = new SeaLionServerNode(host, port);
		nodes.put(node.getStrSocketAddress(), node);
		bootstrap(nodes);
		if (nodes.size() == 0) {
			group.shutdownGracefully().syncUninterruptibly();
			throw new IllegalStateException("There's not any SeaLion server to connect");
		}
		hashBalancer = new HashBalancer(nodes);
		responseHandler.setHashBalancer(hashBalancer);
	}
	
	/**
	 * Construct a SeaLion client through a list of socket address in string format. 
	 * All server strings are delimited by white space. 
	 * 
	 * The server string format should be (host|ip)[:port][:weight]. the weight 
	 * is the server's weight, user can configured it according the 
	 * server's capability or something else.
	 * 
	 * Note: 
	 * In order to assign the weight, you must assign the port in the same time.
	 * 
	 * @param socketAddresses 
	 * 			A list of socket address in string format
	 * @author sunhe
	 * @date 2015年3月19日 下午7:42:52
	 */
	public SeaLionClient(String socketAddressesString) {
		String[] socketAddresses = socketAddressesString.split("\\s+");
		init();
//		List<SeaLionServerNode> nodes = Collections.synchronizedList(new LinkedList<SeaLionServerNode>());
		ConcurrentHashMap<String, SeaLionServerNode> nodes = new ConcurrentHashMap<String, SeaLionServerNode>();
		for (String socketAddress : socketAddresses) {
			SeaLionServerNode node = new SeaLionServerNode(socketAddress);
			nodes.put(node.getStrSocketAddress(), node);
		}
		bootstrap(nodes);
		if (nodes.size() == 0) {
			group.shutdownGracefully().syncUninterruptibly();
			throw new IllegalStateException("There's not any SeaLion server to connect");
		}
		hashBalancer = new HashBalancer(nodes);
		responseHandler.setHashBalancer(hashBalancer);
	}
	
	/**
	 * Initialize some field of this class.
	 * 
	 * @author sunhe
	 * @date 2015年3月19日 下午7:52:43
	 */
	private void init() {
		group = new NioEventLoopGroup();
		opaqueToThreadId = new ConcurrentHashMap<String, Long>();
		threadIdToResponseQueue = new ConcurrentHashMap<Long, BlockingQueue<SeaLionResponse>>();
		responseHandler = new SeaLionResponseHandler(opaqueToThreadId, threadIdToResponseQueue);
		requestEncoder = new SeaLionRequestEncoder();
	}
	
	/**
	 * Bootstrap the client and connect to all assigned servers
	 * 
	 * @author sunhe
	 * @date 2015年3月15日 下午5:44:18
	 */
	private void bootstrap(final ConcurrentHashMap<String, SeaLionServerNode> nodes) {
		List<ChannelFuture> futureList = new ArrayList<ChannelFuture>();
		Set<String> keySet = nodes.keySet();
		for (String key : keySet) {
			final SeaLionServerNode node = nodes.get(key);
			Bootstrap  bootstrap = new Bootstrap();
			bootstrap.group(group)
					.channel(NioSocketChannel.class)
					.remoteAddress(node.getSocketAddress())
					.handler(new ChannelInitializer<Channel>() {

						@Override
						protected void initChannel(Channel ch) throws Exception {
							ch.pipeline().addLast(new SeaLionResponseDecoder())
									.addLast(responseHandler)
									.addLast(requestEncoder);
						}
						
					});
			// This method call is asynchronous.
			ChannelFuture future = bootstrap.connect();
			future.addListener(new ChannelFutureListener() {
				
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						node.setChannel(future.channel());
					}
					else {
						// Fail to connect to the server.
						nodes.remove(node.getStrSocketAddress());
					}
				}
				
			});
			futureList.add(future);
		}
		for (ChannelFuture future : futureList) {
			try {
				future.syncUninterruptibly();
			}
			catch (Throwable throwable) {
				throwable.printStackTrace();
			}
		}
	}
	
	/**
	 * Close the connection to SeaLion server
	 * and release all related resources.
	 * 
	 * @throws InterruptedException
	 * @author sunhe
	 * @date 2015年3月15日 下午6:09:47
	 */
	public void close() {
		Collection<SeaLionServerNode> nodes = hashBalancer.getServerNodes().values();
		for (SeaLionServerNode node : nodes) {
			Channel channel = node.getChannel();
			if (channel.isOpen()) {
				channel.close();
			}
		}
		group.shutdownGracefully();
	}
	
	/**
	 * Ensure that current thread has its own response queue.
	 * 
	 * @return current thread's response queue.
	 * @author sunhe
	 * @date 2015年3月17日 下午7:58:09
	 */
	private BlockingQueue<SeaLionResponse> ensureResponseQueue() {
		long threadId = Thread.currentThread().getId();
		BlockingQueue<SeaLionResponse> responseQueue = null;
		if (! threadIdToResponseQueue.containsKey(threadId)) {
			responseQueue = new LinkedBlockingQueue<SeaLionResponse>();
			threadIdToResponseQueue.put(threadId, responseQueue);
		}
		else {
			responseQueue = threadIdToResponseQueue.get(threadId);
		}
		return responseQueue;
	}
	
	/**
	 * Block for a while to wait for the response
	 * 
	 * @param responseQueue 
	 * 			The response queue where the received response will be put.
	 * @param opaque 
	 * 			The request's opaque field.
	 * @return The received response, null if current thread is interrupted or 
	 * 			connection is broken.
	 * @author sunhe
	 * @date 2015年3月17日 下午8:20:52
	 */
	private SeaLionResponse getResponse(BlockingQueue<SeaLionResponse> responseQueue, String opaque) {
		SeaLionResponse response = null;
		try {
			while (true) {
				response = responseQueue.poll(RESPONSE_WAIT_TIMEOUT, TimeUnit.SECONDS);
				if (response == null) {
					// Waiting for response times out.
					break;
				}
				else {
					if (opaque.equals(response.getOpaque())) {
						// The received response matches the sent request.
						opaqueToThreadId.remove(opaque);
						break;
					}
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return response;
	}

	/*
	 * 
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#getString(java.lang.String)
	 * @author sunhe
	 * @date 2015年3月15日 下午7:14:12
	 */
	public String getString(String key) {
		return getString(key, null);
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#getString(java.lang.String, java.lang.String)
	 * @author sunhe
	 * @date 2015年4月9日 下午1:09:03
	 */
	public String getString(String key, String cas) {
		byte[] value = getBytes(key, cas);
		return value == null ? null : new String(value, Charsets.UTF_8);
	}

	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#setString(java.lang.String, java.lang.String)
	 * @author sunhe
	 * @date 2015年3月15日 下午7:14:17
	 */
	public boolean setString(String key, String value, long timeStamp) {
		return setString(key, value, timeStamp, null);
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#setString(java.lang.String, java.lang.String, long, java.lang.String)
	 * @author sunhe
	 * @date 2015年4月8日 下午6:40:28
	 */
	public boolean setString(String key, String value, long timeStamp, String cas) {
		byte[] data = value.getBytes(Charsets.UTF_8);
		return setBytes(key, data, timeStamp, cas);
	}
	
	/**
	 * Set key-value with specified expiration time in minute unit.
	 * 
	 * @param key
	 * @param value
	 * @param expireMinute 
	 * 		The expiration time in minute unit. 0 signifies never expiring.
	 * @return True if operation is successful, otherwise false.
	 * @author sunhe
	 * @date 2015年3月28日 下午9:13:09
	 */
	public boolean setString(String key, String value, int expireMinute) {
		return setString(key, value, expireMinute, null);
	}
	
	/**
	 * Set key-value with specified expiration time in minute unit.
	 * 
	 * @param key
	 * @param value
	 * @param expireMinute
	 * 		The expiration time in minute unit. 0 signifies never expiring.
	 * @param cas
	 * @return True if operation is successful, otherwise false.
	 * @author sunhe
	 * @date 2015年4月8日 下午6:42:19
	 */
	public boolean setString(String key, String value, int expireMinute, String cas) {
		if (expireMinute < 0) {
			throw new IllegalArgumentException("Illegal expiration time value: " + expireMinute);
		}
		long timeStamp = 0;
		if (expireMinute != 0) {
			timeStamp = TimeStampFormatter.fromMinuteToTimeStamp(expireMinute);
		}
		return setString(key, value, timeStamp, cas);
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#setStringUnsafe(java.lang.String, java.lang.String, long)
	 * @author sunhe
	 * @date 2015年3月29日 下午7:20:34
	 */
	public ChannelFuture setStringUnsafe(String key, String value, long timeStamp) {
		return setStringUnsafe(key, value, timeStamp, null);
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#setStringUnsafe(java.lang.String, java.lang.String, long, java.lang.String)
	 * @author sunhe
	 * @date 2015年4月8日 下午6:46:58
	 */
	public ChannelFuture setStringUnsafe(String key, String value, long timeStamp, String cas) {
		byte[] data = value.getBytes(Charsets.UTF_8);
		return setBytesUnsafe(key, data, timeStamp, cas);
	}
	
	/**
	 * Set key-value with specified expiration time in minute unit.
	 * 
	 * @param key
	 * @param value
	 * @param expireMinute
	 * 			The expiration time in minute unit. 0 signifies never expiring.
	 * @return
	 * @author sunhe
	 * @date 2015年3月30日 下午7:34:38
	 */
	public ChannelFuture setStringUnsafe(String key, String value, int expireMinute) {
		return setStringUnsafe(key, value, expireMinute, null);
	}
	
	/**
	 * Set key-value with specified expiration time in minute unit.
	 * 
	 * @param key
	 * @param value
	 * @param expireMinute
	 * 			The expiration time in minute unit. 0 signifies never expiring.
	 * @param cas
	 * @return
	 * @author sunhe
	 * @date 2015年4月8日 下午6:49:45
	 */
	public ChannelFuture setStringUnsafe(String key, String value, int expireMinute, String cas) {
		if (expireMinute < 0) {
			throw new IllegalArgumentException("Illegal expiration time value: " + expireMinute);
		}
		long timeStamp = 0;
		if (expireMinute != 0) {
			timeStamp = TimeStampFormatter.fromMinuteToTimeStamp(expireMinute);
		}
		return setStringUnsafe(key, value, timeStamp, cas);
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#getBytes(java.lang.String)
	 * @author sunhe
	 * @date 2015年3月29日 下午6:45:02
	 */
	public byte[] getBytes(String key) {
		return getBytes(key, null);
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#getBytes(java.lang.String, java.lang.String)
	 * @author sunhe
	 * @date 2015年4月9日 下午1:06:23
	 */
	public byte[] getBytes(String key, String cas) {
		SeaLionResponse response = getBytesAndCas(key, cas);
		if (response == null) {
			// May because connection is broken or current thread is interrupted.
			return null;
		}
		else {
			String status = response.getStatus();
			ByteBuf data = response.getData();
			if (Statuses.SUCCESS.equals(status)) {
				return data.array();
			}
			else {
				return null;
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#getBytesAndCas(java.lang.String)
	 * @author sunhe
	 * @date 2015年4月9日 下午12:30:38
	 */
	public SeaLionResponse getBytesAndCas(String key) {
		return getBytesAndCas(key, null);
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#getBytesAndCas(java.lang.String, java.lang.String)
	 * @author sunhe
	 * @date 2015年4月9日 下午1:04:34
	 */
	public SeaLionResponse getBytesAndCas(String key, String cas) {
		BlockingQueue<SeaLionResponse> responseQueue = ensureResponseQueue();
		String opaque = OpaqueGenerator.generate();
		opaqueToThreadId.put(opaque, Thread.currentThread().getId());
		
		SeaLionRequest request = new SeaLionRequest();
		request.setCommand(Commands.GET);
		request.setKey(key);
		request.setOpaque(opaque);
		request.setDataLen(0);
		request.setIsSafeMode(true);
		request.setCas(cas);
		request.setData(Unpooled.EMPTY_BUFFER);
		hashBalancer.getServerNodeByKey(key).getChannel().writeAndFlush(request);
		
		return getResponse(responseQueue, opaque);
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#setBytes(java.lang.String, byte[], long)
	 * @author sunhe
	 * @date 2015年3月28日 下午10:46:19
	 */
	public boolean setBytes(String key, byte[] value, long timeStamp) {
		return setBytes(key, value, timeStamp, null);
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#setBytes(java.lang.String, byte[], long, java.lang.String)
	 * @author sunhe
	 * @date 2015年4月8日 下午6:08:57
	 */
	public boolean setBytes(String key, byte[] value, long timeStamp, String cas) {
		if (key.contains(" ")) {
			throw new IllegalArgumentException("Key can't contain any white characters: " + key);
		}
		if (timeStamp < 0) {
			throw new IllegalArgumentException("Illegal time stamp value: " + timeStamp);
		}
		BlockingQueue<SeaLionResponse> responseQueue = ensureResponseQueue();
		String opaque = OpaqueGenerator.generate();
		opaqueToThreadId.put(opaque, Thread.currentThread().getId());
		
		ByteBuf data = Unpooled.wrappedBuffer(value);
		SeaLionRequest request = new SeaLionRequest();
		request.setCommand(Commands.SET);
		request.setKey(key);
		request.setOpaque(opaque);
		request.setDataLen(data.capacity());
		request.setIsSafeMode(true);
		request.setCas(cas);
		request.setTimeStamp(timeStamp);
		request.setData(data);
		hashBalancer.getServerNodeByKey(key).getChannel().writeAndFlush(request);
		
		SeaLionResponse response = getResponse(responseQueue, opaque);
		if (response == null) {
			return false;
		}
		else {
			String status = response.getStatus();
			if (Statuses.SUCCESS.equals(status)) {
				return true;
			}
			else {
				return false;
			}
		}
	}
	
	/**
	 * Set key-value with specified expiration time in minute unit.
	 * 
	 * @param key
	 * @param value
	 * @param expireMinute
	 * 			The expiration time in minute unit. 0 signifies never expiring.
	 * @return True if operation is successful, otherwise false.
	 * @author sunhe
	 * @date 2015年3月28日 下午10:53:10
	 */
	public boolean setBytes(String key, byte[] value, int expireMinute) {
		return setBytes(key, value, expireMinute, null);
	}
	
	/**
	 * Set key-value with specified expiration time in minute unit.
	 * 
	 * @param key
	 * @param value
	 * @param expireMinute The expiration time in minute unit. 0 signifies never expiring.
	 * @param cas
	 * @return True if operation is successful, otherwise false.
	 * @author sunhe
	 * @date 2015年4月8日 下午6:13:10
	 */
	public boolean setBytes(String key, byte[] value, int expireMinute, String cas) {
		if (expireMinute < 0) {
			throw new IllegalArgumentException("Illegal expiration time value: " + expireMinute);
		}
		long timeStamp = 0;
		if (expireMinute != 0) {
			timeStamp = TimeStampFormatter.fromMinuteToTimeStamp(expireMinute);
		}
		return setBytes(key, value, timeStamp, cas);
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#setBytesUnsafe(java.lang.String, byte[], long)
	 * @author sunhe
	 * @date 2015年3月29日 下午7:13:13
	 */
	public ChannelFuture setBytesUnsafe(String key, byte[] value, long timeStamp) {
		return setBytesUnsafe(key, value, timeStamp, null);
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#setBytesUnsafe(java.lang.String, byte[], long, java.lang.String)
	 * @author sunhe
	 * @date 2015年4月8日 下午6:25:31
	 */
	public ChannelFuture setBytesUnsafe(String key, byte[] value, long timeStamp, String cas) {
		if (timeStamp < 0) {
			throw new IllegalArgumentException("Illegal time stamp value: " + timeStamp);
		}
		ByteBuf data = Unpooled.wrappedBuffer(value);
		SeaLionRequest request = new SeaLionRequest();
		request.setCommand(Commands.SET);
		request.setKey(key);
		request.setOpaque(OpaqueGenerator.generate());
		request.setDataLen(data.capacity());
		// unsafe mode
		request.setIsSafeMode(false);
		request.setCas(cas);
		request.setTimeStamp(timeStamp);
		request.setData(data);
		return hashBalancer.getServerNodeByKey(key).getChannel().writeAndFlush(request);
	}
	
	/**
	 * Set key-value with specified expiration time in minute unit.
	 * 
	 * @param key
	 * @param value
	 * @param expireMinute
	 * 			The expiration time in minute unit. 0 signifies never expiring.
	 * @return
	 * @author sunhe
	 * @date 2015年3月30日 下午7:33:18
	 */
	public ChannelFuture setBytesUnsafe(String key, byte[] value, int expireMinute) {
		return setBytesUnsafe(key, value, expireMinute, null);
	}
	
	/**
	 * Set key-value with specified expiration time in minute unit.
	 * 
	 * @param key
	 * @param value
	 * @param expireMinute
	 * 			The expiration time in minute unit. 0 signifies never expiring.
	 * @param cas
	 * @return
	 * @author sunhe
	 * @date 2015年4月8日 下午6:31:03
	 */
	public ChannelFuture setBytesUnsafe(String key, byte[] value, int expireMinute, String cas) {
		if (expireMinute < 0) {
			throw new IllegalArgumentException("Illegal expiration time value: " + expireMinute);
		}
		long timeStamp = 0;
		if (expireMinute != 0) {
			timeStamp = TimeStampFormatter.fromMinuteToTimeStamp(expireMinute);
		}
		return setBytesUnsafe(key, value, timeStamp, cas);
	}

	/*
	 * 
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#deleteValue(java.lang.String)
	 * @author sunhe
	 * @date 2015年3月15日 下午7:14:22
	 */
	public boolean deleteValue(String key) {
		return deleteValue(key, null);
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#deleteValue(java.lang.String, java.lang.String)
	 * @author sunhe
	 * @date 2015年4月9日 下午1:42:58
	 */
	public boolean deleteValue(String key, String cas) {
		BlockingQueue<SeaLionResponse> responseQueue = ensureResponseQueue();
		String opaque = OpaqueGenerator.generate();
		opaqueToThreadId.put(opaque, Thread.currentThread().getId());
		
		SeaLionRequest request = new SeaLionRequest();
		request.setCommand(Commands.DELETE);
		request.setKey(key);
		request.setOpaque(opaque);
		request.setDataLen(0);
		request.setIsSafeMode(true);
		request.setCas(cas);
		request.setData(Unpooled.EMPTY_BUFFER);
		hashBalancer.getServerNodeByKey(key).getChannel().writeAndFlush(request);
		
		SeaLionResponse response = getResponse(responseQueue, opaque);
		if (response == null) {
			return false;
		}
		else {
			String status = response.getStatus();
			if (Statuses.SUCCESS.equals(status)) {
				return true;
			}
			else {
				return false;
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#deleteValueUnsafe(java.lang.String)
	 * @author sunhe
	 * @date 2015年3月29日 下午7:23:52
	 */
	public ChannelFuture deleteValueUnsafe(String key) {
		return deleteValueUnsafe(key, null);
	}
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#deleteValueUnsafe(java.lang.String, java.lang.String)
	 * @author sunhe
	 * @date 2015年4月9日 下午1:28:39
	 */
	public ChannelFuture deleteValueUnsafe(String key, String cas) {
		SeaLionRequest request = new SeaLionRequest();
		request.setCommand(Commands.DELETE);
		request.setKey(key);
		request.setOpaque(OpaqueGenerator.generate());
		request.setDataLen(0);
		// unsafe mode
		request.setIsSafeMode(false);
		request.setCas(cas);
		request.setData(Unpooled.EMPTY_BUFFER);
		return hashBalancer.getServerNodeByKey(key).getChannel().writeAndFlush(request);
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionClientOperation#getCas(java.lang.String)
	 * @author sunhe
	 * @date 2015年4月9日 下午1:28:47
	 */
	public String getCas(String key) {
		BlockingQueue<SeaLionResponse> responseQueue = ensureResponseQueue();
		String opaque = OpaqueGenerator.generate();
		opaqueToThreadId.put(opaque, Thread.currentThread().getId());
		
		SeaLionRequest request = new SeaLionRequest();
		request.setCommand(Commands.CAS);
		request.setKey(key);
		request.setOpaque(opaque);
		request.setDataLen(0);
		request.setIsSafeMode(true);
		request.setData(Unpooled.EMPTY_BUFFER);
		hashBalancer.getServerNodeByKey(key).getChannel().writeAndFlush(request);
		
		SeaLionResponse response = getResponse(responseQueue, opaque);
		if (response == null) {
			// May because connection is broken or current thread is interrupted.
			return null;
		}
		else {
			String status = response.getStatus();
			if (Statuses.SUCCESS.equals(status)) {
				return response.getCas();
			}
			else {
				return null;
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionServerMonitorOperation#getMemPercentage()
	 * @author sunhe
	 * @date 2015年4月10日 下午8:05:55
	 */
	public Double getMemPercentage() {
		// The key doesn't have any meaning here.
		String key = "N/A";
		BlockingQueue<SeaLionResponse> responseQueue = ensureResponseQueue();
		String opaque = OpaqueGenerator.generate();
		opaqueToThreadId.put(opaque, Thread.currentThread().getId());
		
		SeaLionRequest request = new SeaLionRequest();
		request.setCommand(Commands.MEM_PERCENTAGE);
		request.setKey(key);
		request.setOpaque(opaque);
		request.setDataLen(0);
		request.setIsSafeMode(true);
		request.setData(Unpooled.EMPTY_BUFFER);
		hashBalancer.getServerNodeByKey(key).getChannel().writeAndFlush(request);
		
		SeaLionResponse response = getResponse(responseQueue, opaque);
		if (response == null) {
			// May because connection is broken or current thread is interrupted.
			return null;
		}
		else {
			String status = response.getStatus();
			if (Statuses.SUCCESS.equals(status)) {
				return response.getData().getDouble(0);
			}
			else {
				return null;
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionServerMonitorOperation#getMemUsage()
	 * @author sunhe
	 * @date 2015年4月10日 下午8:48:31
	 */
	public Long getMemUsage() {
		// The key doesn't have any meaning here.
		String key = "N/A";
		BlockingQueue<SeaLionResponse> responseQueue = ensureResponseQueue();
		String opaque = OpaqueGenerator.generate();
		opaqueToThreadId.put(opaque, Thread.currentThread().getId());
		
		SeaLionRequest request = new SeaLionRequest();
		request.setCommand(Commands.MEM_USAGE);
		request.setKey(key);
		request.setOpaque(opaque);
		request.setDataLen(0);
		request.setIsSafeMode(true);
		request.setData(Unpooled.EMPTY_BUFFER);
		hashBalancer.getServerNodeByKey(key).getChannel().writeAndFlush(request);
		
		SeaLionResponse response = getResponse(responseQueue, opaque);
		if (response == null) {
			// May because connection is broken or current thread is interrupted.
			return null;
		}
		else {
			String status = response.getStatus();
			if (Statuses.SUCCESS.equals(status)) {
				return response.getData().getLong(0);
			}
			else {
				return null;
			}
		}
	}
	
	public Long getMemLimit() {
		// The key doesn't have any meaning here.
		String key = "N/A";
		BlockingQueue<SeaLionResponse> responseQueue = ensureResponseQueue();
		String opaque = OpaqueGenerator.generate();
		opaqueToThreadId.put(opaque, Thread.currentThread().getId());
		
		SeaLionRequest request = new SeaLionRequest();
		request.setCommand(Commands.MEM_LIMIT);
		request.setKey(key);
		request.setOpaque(opaque);
		request.setDataLen(0);
		request.setIsSafeMode(true);
		request.setData(Unpooled.EMPTY_BUFFER);
		hashBalancer.getServerNodeByKey(key).getChannel().writeAndFlush(request);
		
		SeaLionResponse response = getResponse(responseQueue, opaque);
		if (response == null) {
			// May because connection is broken or current thread is interrupted.
			return null;
		}
		else {
			String status = response.getStatus();
			if (Statuses.SUCCESS.equals(status)) {
				return response.getData().getLong(0);
			}
			else {
				return null;
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionServerMonitorOperation#getTaskQueueSize()
	 * @author sunhe
	 * @date 2015年4月10日 下午9:13:57
	 */
	public Integer getTaskQueueSize() {
		// The key doesn't have any meaning here.
		String key = "N/A";
		BlockingQueue<SeaLionResponse> responseQueue = ensureResponseQueue();
		String opaque = OpaqueGenerator.generate();
		opaqueToThreadId.put(opaque, Thread.currentThread().getId());
		
		SeaLionRequest request = new SeaLionRequest();
		request.setCommand(Commands.TASK_QUEUE_SIZE);
		request.setKey(key);
		request.setOpaque(opaque);
		request.setDataLen(0);
		request.setIsSafeMode(true);
		request.setData(Unpooled.EMPTY_BUFFER);
		hashBalancer.getServerNodeByKey(key).getChannel().writeAndFlush(request);
		
		SeaLionResponse response = getResponse(responseQueue, opaque);
		if (response == null) {
			// May because connection is broken or current thread is interrupted.
			return null;
		}
		else {
			String status = response.getStatus();
			if (Statuses.SUCCESS.equals(status)) {
				return response.getData().getInt(0);
			}
			else {
				return null;
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see io.sunhe.sealion.client.SeaLionServerMonitorOperation#getNumOfConn()
	 * @author sunhe
	 * @date 2015年4月12日 下午7:34:48
	 */
	public Integer getNumOfConn() {
		// The key doesn't have any meaning here.
		String key = "N/A";
		BlockingQueue<SeaLionResponse> responseQueue = ensureResponseQueue();
		String opaque = OpaqueGenerator.generate();
		opaqueToThreadId.put(opaque, Thread.currentThread().getId());
		
		SeaLionRequest request = new SeaLionRequest();
		request.setCommand(Commands.NUM_CONN);
		request.setKey(key);
		request.setOpaque(opaque);
		request.setDataLen(0);
		request.setIsSafeMode(true);
		request.setData(Unpooled.EMPTY_BUFFER);
		hashBalancer.getServerNodeByKey(key).getChannel().writeAndFlush(request);
		
		SeaLionResponse response = getResponse(responseQueue, opaque);
		if (response == null) {
			// May because connection is broken or current thread is interrupted.
			return null;
		}
		else {
			String status = response.getStatus();
			if (Statuses.SUCCESS.equals(status)) {
				return response.getData().getInt(0);
			}
			else {
				return null;
			}
		}
	}
	
	/**
	 * TODO 
	 * 如何进行吞吐量测试:
	 * 因为unsafe模式下，client写数据的操作很快会执行完，写操作都是异步的，
	 * 可能写操作的IO线程还没有写完。
	 * 解决办法：
	 * 以server的时间为准，添加一个命令SYSTIME（参数为socket地址），返回server的时间戳，
	 * 在测试之前执行一次，之后执行若干次（因为server不知一台可能），计算elapse的时间
	 * 
	 * @author sunhe
	 * @date 2015年3月29日 下午4:48:28
	 */
	public static void performanceTest() throws InterruptedException {
		final int nThread = 4;
		final SeaLionClient client = new SeaLionClient("127.0.0.1");
		final CountDownLatch startLatch = new CountDownLatch(1);
		final CountDownLatch endLatch = new CountDownLatch(nThread);
		// total 1024 MB data
		final byte[] value = new byte[1024];
		final int times = 262144;
		
		for (int i = 0; i < nThread; i++) {
			new Thread(new Runnable() {
				
				public void run() {
					try {
						startLatch.await();
						String key;
						for (int i = 0; i < times; i++) {
							key = OpaqueGenerator.generate();
							client.setBytesUnsafe(key, value, 1);
						}
					}
					catch (InterruptedException e) {
						// ignored exception
					}
					finally {
						endLatch.countDown();
					}
				}
				
			}).start();
		}
		
		long start = TimeStampFormatter.getCurrentTimeStamp();
		startLatch.countDown();
		endLatch.await();
		// TODO 用future重新实现这个功能(这个是错误的，只能阻塞主线程5s)
//		client.deleteValue("test");
		// TODO 多个server node，如何保证都写入到管道?
		client.deleteValueUnsafe("test").syncUninterruptibly();
		long end = TimeStampFormatter.getCurrentTimeStamp();
		System.out.println("\n\nTime elapsed: " + (end - start) / 1000 + " s.");
		
		// TODO 在close之前请求一个safe模式的命令?
		client.close();
	}
	
	// Done. Bug: 大规模SET时（即使key相同），会发生server不提供服务的情况.
	// 原因：写操作是异步的，在写操作执行之前，主线程就把channel关闭了（client.close()）.
	public static void main(String[] args) throws FileNotFoundException {
//		System.setOut(new PrintStream("/Users/sunhe/Downloads/temp/stdout_client"));
		try {
			performanceTest();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
