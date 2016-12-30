package io.sunhe.sealion.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.sunhe.sealion.protocol.SeaLionRequestDecoder;
import io.sunhe.sealion.protocol.SeaLionResponseEncoder;
import io.sunhe.sealion.server.dashboard.BlockingTimeAccumulator;
import io.sunhe.sealion.server.dashboard.ChannelRegisterHandler;
import io.sunhe.sealion.server.dashboard.DashboardChannelInitializer;
import io.sunhe.sealion.server.dashboard.DashboardTask;
import io.sunhe.sealion.server.dashboard.HitRatioCollector;
import io.sunhe.sealion.server.dashboard.SeaLionServerMonitor;
import io.sunhe.sealion.server.mem.Item;
import io.sunhe.sealion.server.mem.MapContainer;
import io.sunhe.sealion.server.traffic.SeaLionInboundTrafficStat;
import io.sunhe.sealion.server.traffic.SeaLionOutboundTrafficStat;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * SeaLion server.
 * 
 * @author sunhe
 * @date 2015年3月15日 下午5:25:39
 */
public class SeaLionServer {
	
	/**
	 * The default port of SeaLion server is 1113.
	 */
	public static final int DEFAULT_PORT = 1113;
	
	private int port;
	
	/**
	 * The dashboard server's port.
	 * By default, it the SeaLion server's port plus 1.
	 * So if user don't assign the SeaLion server's port, 
	 * then the dashboard server's port should by 1114.
	 */
	private int dashboardPort;
	
	/**
	 * Worker thread task queue.
	 */
	private BlockingQueue<Task> workerTaskQueue;
	
	/**
	 * Monitor thread task queue.
	 */
	private BlockingQueue<Task> monitorTaskQueue;
	
	private SeaLionRequestHandler requestHandler;
	
	private SeaLionResponseEncoder responseEncoder;
	
	private ChannelRegisterHandler channelRegisterHanlder;
	
	private SeaLionInboundTrafficStat inboundTrafficStat;
	
	private SeaLionOutboundTrafficStat outboundTrafficStat;
	
	/**
	 * Hold all active channels (client channels that server channel accepts).
	 * Used mainly for determining the current connection number.
	 */
	private ChannelGroup channelGroup;
	
	/**
	 * Dashboard channel group.
	 */
	private ChannelGroup dbChannelGroup;
	
	private volatile SeaLionServerMonitor serverMonitor;
	
	/**
	 * In byte.
	 * Cann't be changed during uptime.
	 */
	private long maxMemSize;
	
	public SeaLionServer() {
		this (DEFAULT_PORT, MapContainer.DEFAULT_MAX_MEM_SIZE);
	}
	
	public SeaLionServer(int port) {
		this (port, MapContainer.DEFAULT_MAX_MEM_SIZE);
	}
	
	public SeaLionServer(long maxMemSize) {
		this (DEFAULT_PORT, maxMemSize);
	}
	
	public SeaLionServer(int port, long maxMemSize) {
		this.port = port;
		dashboardPort = port + 1;
		this.maxMemSize = maxMemSize;
		channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		dbChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		workerTaskQueue = new LinkedBlockingQueue<Task>();
		monitorTaskQueue = new LinkedBlockingQueue<Task>();
		requestHandler = new SeaLionRequestHandler(workerTaskQueue, monitorTaskQueue, channelGroup);
		responseEncoder = new SeaLionResponseEncoder();
		channelRegisterHanlder = new ChannelRegisterHandler(dbChannelGroup);
		inboundTrafficStat = new SeaLionInboundTrafficStat();
		outboundTrafficStat = new SeaLionOutboundTrafficStat();
	}
	
	/**
	 * Make the server start to serve.
	 * 
	 * @author sunhe
	 * @date 2015年3月15日 上午10:13:09
	 */
	public void serve() {
		EventLoopGroup group = new NioEventLoopGroup();
		EventLoopGroup childGroup = new NioEventLoopGroup();
		BlockingTimeAccumulator blockingTimeAccumulator = new BlockingTimeAccumulator();
		final MapContainer mapContainer = new MapContainer(new HashMap<String, Item>(), 
				blockingTimeAccumulator, maxMemSize);
		HitRatioCollector hitRatioCollector = new HitRatioCollector();
		serverMonitor = new SeaLionServerMonitor(workerTaskQueue, mapContainer, channelGroup, 
				inboundTrafficStat, outboundTrafficStat, hitRatioCollector, blockingTimeAccumulator);
		
		try {
			// bootstrap the worker thread here.
			ExecutorService workerExecutor = Executors.newSingleThreadExecutor();
			Future<?> workerFuture = workerExecutor.submit(new WorkerTask(workerTaskQueue, mapContainer, hitRatioCollector));
			
			// bootstrap the monitor thread here.
			ExecutorService monitorExecutor = Executors.newSingleThreadExecutor();
			Future<?> monitorFuture 
					= monitorExecutor.submit(
					new MonitorTask(serverMonitor, monitorTaskQueue));
			
			// bootstrap the dashboard thread here.
			ExecutorService dashboardExecutor = Executors.newSingleThreadExecutor();
			Future<?> dashboardFuture = dashboardExecutor.submit(new DashboardTask(dbChannelGroup, serverMonitor, 1));
			
			// bootstrap server here.
			ServerBootstrap bootstrap = new ServerBootstrap();
			bootstrap.group(group, childGroup)
					.channel(NioServerSocketChannel.class)
					.localAddress(port)
					.childHandler(new ChannelInitializer<Channel>() {
						
						@Override
						protected void initChannel(Channel ch) throws Exception {
							ch.pipeline().addLast(inboundTrafficStat)
									.addLast(outboundTrafficStat)
									.addLast(new SeaLionRequestDecoder(mapContainer))
									.addLast(requestHandler)
									.addLast(responseEncoder);
						}
						
					});
			
			// bootstrap dashboard server here.
			ServerBootstrap dbBootstrap = new ServerBootstrap();

			dbBootstrap.group(group, childGroup)
					.channel(NioServerSocketChannel.class)
					.localAddress(dashboardPort)
					.childHandler(new ChannelInitializer<Channel>() {

						@Override
						protected void initChannel(Channel ch) throws Exception {
							ch.pipeline().addLast(
									new DashboardChannelInitializer(dashboardPort, channelRegisterHanlder));
						}
						
					});
			
			// bind servers here.
			ChannelFuture serverFuture = bootstrap.bind().syncUninterruptibly();
			System.out.println("SeaLion listening on port #" + port);
			ChannelFuture dbServerFuture = dbBootstrap.bind().syncUninterruptibly();
			System.out.println("Dashboard listening on port #" + dashboardPort);
			
			serverFuture.channel().closeFuture().syncUninterruptibly();
			dbServerFuture.channel().closeFuture().syncUninterruptibly();
		}
		finally {
			group.shutdownGracefully().syncUninterruptibly();
			childGroup.shutdownGracefully().syncUninterruptibly();
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		SeaLionServerParams params = new SeaLionServerParams(args);
		int port = DEFAULT_PORT;
		long maxMemSize = MapContainer.DEFAULT_MAX_MEM_SIZE;
		if (params.getP() != null) {
			port = Integer.parseInt(params.getP());
		}
		if (params.getM() != null) {
			maxMemSize = Long.valueOf(params.getM()) * 1024L * 1024L;
		}
		// start server
		new SeaLionServer(port, maxMemSize).serve();
	}
	
}
