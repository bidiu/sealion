package io.sunhe.sealion.server.dashboard;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.sunhe.sealion.util.Charsets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Altered by sunhe.
 * 
 * @author <a href="mailto:norman.maurer@googlemail.com">Norman Maurer</a>
 * @date 2015年4月14日 下午2:59:11
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	
    private static final String REAL_DASHBOARD_PATH;
    
    private static final String REAL_404_PATH;
    
    private static final String REAL_WEB_ROOT_PATH; 
    
    private static String dashboardFileContent;
    
    private static byte[] dashboardFileBytes;
    
    private static ByteBuf dashboardFileByteBuf;
    
    private static byte[] file404Bytes;
    
    private static ByteBuf file404ByteBuf;
    
    /**
     * It's should be "/ws".
     */
    private final String wsUri;
    
    private final int dashboardPort;

    static {
        URL location = HttpRequestHandler.class.getProtectionDomain().getCodeSource().getLocation();
        try {
            String path = location.toURI().toString();
            path = !path.contains("file:") ? path : path.substring(5);
            REAL_WEB_ROOT_PATH = getRealWebRootPath(path);
            REAL_DASHBOARD_PATH = REAL_WEB_ROOT_PATH + "web/html/dashboard.html";
            REAL_404_PATH = REAL_WEB_ROOT_PATH + "web/html/404.html";
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to locate index.html", e);
        }
    }
    
    public HttpRequestHandler(String wsUri, int dashboardPort) throws IOException {
        this.wsUri = wsUri;
        this.dashboardPort  = dashboardPort;
        init();
    }
    
    /**
     * @author sunhe
     * @throws FileNotFoundException 
     * @date 2015年4月14日 下午2:27:57
     */
    public void init() throws IOException  {
    	RandomAccessFile file = null;
    	RandomAccessFile file404 = null;
    	String webSocketUrl = "ws://" + InetAddress.getLocalHost().getHostAddress() + ":"
    			+ dashboardPort + wsUri;
    	try {
    		// Read the dashboard.html file
    		file = new RandomAccessFile(REAL_DASHBOARD_PATH, "r");
    		byte[] buf = new byte[(int) file.length()];
    		file.readFully(buf);
    		dashboardFileContent = new String(buf, Charsets.UTF_8);
    		dashboardFileContent = dashboardFileContent.replace("${webSocketUrl}$", webSocketUrl);
    		dashboardFileBytes = dashboardFileContent.getBytes(Charsets.UTF_8);
    		dashboardFileByteBuf = Unpooled.wrappedBuffer(dashboardFileBytes);
    		
    		// Read the 404.html file
    		file404 = new RandomAccessFile(REAL_404_PATH, "r");
    		file404Bytes = new byte[(int) file404.length()];
    		file404.readFully(file404Bytes);
    		file404ByteBuf = Unpooled.wrappedBuffer(file404Bytes);
    	}
    	finally {
    		if (file != null) {
    			file.close();
    		}
    		if (file404 != null) {
    			file404.close();
    		}
    	}
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (wsUri.equalsIgnoreCase(request.getUri())) {
            ctx.fireChannelRead(request.retain());
        } else {
            if (HttpHeaders.is100ContinueExpected(request)) {
                send100Continue(ctx);
            }
            
            String realRequestedPath = REAL_WEB_ROOT_PATH + request.getUri().substring(1);
            String extendedFileName = getExtendedFileName(realRequestedPath);
            
            boolean keepAlive = HttpHeaders.isKeepAlive(request);
            
            if (realRequestedPath.equals(REAL_DASHBOARD_PATH)) {
            	HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK);
            	response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
                
                if (keepAlive) {
                    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, dashboardFileBytes.length);
                    response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                }
                ctx.write(response);
            	ctx.write(dashboardFileByteBuf);
            }
            else {
            	try {
            		final RandomAccessFile file = new RandomAccessFile(realRequestedPath, "r");
            		
            		HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.OK);
    	            
    	            if (extendedFileName.equals("html") || extendedFileName.equals("htm")) {
    	            	response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
    	            }
    	            else if (extendedFileName.equals("js")) {
    	            	response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/javascript; charset=UTF-8");
    	            }
    	            else if (extendedFileName.equals("ico")) {
    	            	response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "image/x-icon");
    	            }
    	            else if (extendedFileName.equals("css")) {
    	            	response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/css");
    	            }
    	            else if (extendedFileName.equals("png")) {
    	            	response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "image/png");
    	            }
    	            else {
    	            	response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
    	            }
    	            
    	            if (keepAlive) {
    	                response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, file.length());
    	                response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    	            }
    	            ctx.write(response);
    	            
    	            if (ctx.pipeline().get(SslHandler.class) == null) {
    	                ctx.write(new DefaultFileRegion(file.getChannel(), 0, file.length()))
    	                		.addListener(new GenericFutureListener<Future<? super Void>>() {
    	
    								public void operationComplete(Future<? super Void> future) throws Exception {
    									file.close();
    								}
    								
    							});
    	            } 
    	            else {
    	                ctx.write(new ChunkedNioFile(file.getChannel()))
    	                		.addListener(new GenericFutureListener<Future<? super Void>>() {
    	
    								public void operationComplete(Future<? super Void> future) throws Exception {
    									file.close();
    								}
    								
    							});
    	            }
            	}
            	catch (FileNotFoundException e) {
            		// File not found, 404.
            		HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(), HttpResponseStatus.NOT_FOUND);
            		response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
            		if (keepAlive) {
                        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, file404Bytes.length);
                        response.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                    }
            		ctx.write(response);
            		ctx.write(file404ByteBuf.retain());
            	}
            }
            
            ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            if (!keepAlive) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
    
    /**
     * @param path
     * @return
     * @author sunhe
     * @date 2015年4月14日 下午1:05:07
     */
    public static String getExtendedFileName(String path) {
    	int dotIndex = path.lastIndexOf(".");
    	if (dotIndex + 1 >= path.length()) {
    		return "";
    	}
    	else {
    		return path.substring(dotIndex + 1);
    	}
    }
    
    /**
     * @param uri
     * @return
     * @author sunhe
     * @date 2015年4月14日 下午4:05:47
     */
    public static String getRealWebRootPath(String uri) {
    	int forwardSlashIndex = uri.lastIndexOf("/");
    	return uri.substring(0, forwardSlashIndex + 1);
    }
    
}
