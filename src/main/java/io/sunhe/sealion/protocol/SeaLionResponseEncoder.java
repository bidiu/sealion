package io.sunhe.sealion.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.sunhe.sealion.util.Charsets;
import io.sunhe.sealion.util.Logger;

/**
 * SeaLion response encoder.
 * Thread-safe.
 * 
 * The format of the response of the protocol:
 * 		(status) (opaque) (dataLen)[ (CAS)]\r\n
 * 		(data in binary)\r\n
 * 
 * @author sunhe
 * @date 2015年4月8日 下午2:30:08
 */
@Sharable
public class SeaLionResponseEncoder extends MessageToByteEncoder<SeaLionResponse> {

	@Override
	protected void encode(ChannelHandlerContext ctx, SeaLionResponse msg, ByteBuf out) throws Exception {
		out.writeBytes((msg.getStatus() + " ").getBytes(Charsets.UTF_8));
		out.writeBytes((msg.getOpaque() + " ").getBytes(Charsets.UTF_8));
		out.writeBytes(Integer.toString(msg.getDataLen()).getBytes(Charsets.UTF_8));
		if (msg.getCas() != null) {
			// Has CAS value.
			out.writeBytes((" " + msg.getCas()).getBytes(Charsets.UTF_8));
		}
		out.writeBytes("\r\n".getBytes(Charsets.UTF_8));
		// It's imperative to invoke duplicate method.
		out.writeBytes(msg.getData().duplicate());
		out.writeBytes("\r\n".getBytes(Charsets.UTF_8));
		Logger.log(ctx.channel().remoteAddress().toString() + " Encoded response", msg.toString());
	}
	
}
