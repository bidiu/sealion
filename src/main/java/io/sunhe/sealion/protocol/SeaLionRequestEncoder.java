package io.sunhe.sealion.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.sunhe.sealion.util.Charsets;
import io.sunhe.sealion.util.Logger;

/**
 * SeaLion request encoder
 * Thread-safe.
 * 
 * The format of the request of the protocol:
 * 		(command) (key) (opaque) (dataLen) (safe mode)[ (CAS)]\r\n
 * 		(expiration time stamp in binary)(data in binary)\r\n
 * 
 * Note that the 'dataLen' is the length of the data in byte, 
 * not including the time stamp and the \r\n at the end.
 * All commands have the time stamp field, which is 8 bytes, even though some command, like 
 * DELETE, don't need it.
 * Safe mode is either '1' character, in safe mode, or '0' character, in unsafe mode.
 * The formation of CAS is identical to opaque, 4 arbitrary characters or numbers.
 * 
 * @author sunhe
 * @date 2015年3月14日 下午8:56:36
 */
@Sharable
public class SeaLionRequestEncoder extends MessageToByteEncoder<SeaLionRequest> {

	/*
	 * (non-Javadoc)
	 * @see io.netty.handler.codec.MessageToByteEncoder#encode(io.netty.channel.ChannelHandlerContext, java.lang.Object, io.netty.buffer.ByteBuf)
	 * @author sunhe
	 * @date 2015年3月14日 下午8:57:09
	 */
	@Override
	protected void encode(ChannelHandlerContext ctx, SeaLionRequest msg, ByteBuf out) throws Exception {
		out.writeBytes((msg.getCommand() + " ").getBytes(Charsets.UTF_8));
		out.writeBytes((msg.getKey() + " ").getBytes(Charsets.UTF_8));
		out.writeBytes((msg.getOpaque() + " ").getBytes(Charsets.UTF_8));
		out.writeBytes((msg.getDataLen() + " ").getBytes(Charsets.UTF_8));
		if (msg.getIsSafeMode()) {
			// safe mode
			out.writeBytes("1".getBytes(Charsets.UTF_8));
		}
		else {
			// unsafe mode
			out.writeBytes("0".getBytes(Charsets.UTF_8));
		}
		if (msg.getCas() != null) {
			// the request has a CAS check.
			out.writeBytes((" " + msg.getCas()).getBytes(Charsets.UTF_8));
		}
		out.writeBytes("\r\n".getBytes(Charsets.UTF_8));
		// write the expiration time stamp.
		out.writeLong(msg.getTimeStamp());
		// It's imperative to invoke duplicate method.
		out.writeBytes(msg.getData().duplicate());
		out.writeBytes("\r\n".getBytes(Charsets.UTF_8));
		Logger.log(ctx.channel().remoteAddress().toString() + " Encoded request", msg.toString());
	}

}
