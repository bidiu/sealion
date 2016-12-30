package io.sunhe.sealion.protocol;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.sunhe.sealion.util.Charsets;
import io.sunhe.sealion.util.Logger;

/**
 * SeaLion response decoder.
 * Not thread-safe.
 * 
 * @author sunhe
 * @date 2015年3月14日 下午9:06:09
 */
public class SeaLionResponseDecoder extends ByteToMessageDecoder {
	
	// Decoding text line
	private static final int TEXT_LINE = 0;
	// Decoding data field(unstructured data)
	private static final int DATA_FIELD = 1;
	
	private int state = TEXT_LINE;
	
	/**
	 * The resulting response POJO.
	 */
	private SeaLionResponse response = null;

	/*
	 * 
	 * (non-Javadoc)
	 * @see io.netty.handler.codec.ByteToMessageDecoder#decode(io.netty.channel.ChannelHandlerContext, io.netty.buffer.ByteBuf, java.util.List)
	 * @author sunhe
	 * @date 2015年3月14日 下午9:18:57
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (state == TEXT_LINE) {
			if (response == null) {
				response = new SeaLionResponse();
			}
			int betweenR = in.bytesBefore("\r".getBytes(Charsets.UTF_8)[0]);
			int betweenN = in.bytesBefore("\n".getBytes(Charsets.UTF_8)[0]);
			if (betweenR == -1 || betweenN == -1 || betweenR+1 != betweenN) {
				// the text line hasn't yet been read completely
				return;
			}
			String textLine = in.toString(in.readerIndex(), betweenR+1, Charsets.UTF_8);
			String[] words = textLine.split("\\s+");
			in.readerIndex(in.readerIndex() + betweenR + 2);
			String status = words[0];
			String opaque = words[1];
			int dataLen = Integer.parseInt(words[2]);
			//For now, the response's text line should have either 3 or 4 words.
			String cas = words.length == 4 ? words[3] : null;
			response.setStatus(status);
			response.setOpaque(opaque);
			response.setDataLen(dataLen);
			response.setCas(cas);
			state = DATA_FIELD;
		}
		else {
			// Decode the data field.
			// Note that '+ 2' is the length of '\r\n'.
			if (in.readableBytes() >= response.getDataLen() + 2) {
				ByteBuf data = Unpooled.buffer(response.getDataLen(), response.getDataLen());
				in.readBytes(data);
				in.readerIndex(in.readerIndex() + 2);
				response.setData(data);
				Logger.log(ctx.channel().remoteAddress().toString() + " Decoded response", response.toString());
				out.add(response);
				resetState();
			}
		}
	}
	
	/**
	 * Reset the decoding state.
	 * 
	 * @author sunhe
	 * @date 2015年3月14日 下午8:42:44
	 */
	private void resetState() {
		state = TEXT_LINE;
		response = null;
	}

}
