package io.sunhe.sealion.protocol;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.sunhe.sealion.server.mem.MapContainer;
import io.sunhe.sealion.util.Charsets;
import io.sunhe.sealion.util.Logger;

/**
 * SeaLion request decoder.
 * Not thread-safe.
 * 
 * @author sunhe
 * @date 2015年3月14日 下午7:50:49
 */
public class SeaLionRequestDecoder extends ByteToMessageDecoder {
	
	// Decoding text line
	private static final int TEXT_LINE = 0;
	// Decoding data field(unstructured data)
	private static final int DATA_FIELD = 1;
	
	private MapContainer mapContainer;
	
	private int state = TEXT_LINE;
	
	/**
	 * The resulting request POJO.
	 */
	private SeaLionRequest request = null;
	
	private boolean isBadRequest = false;
	
	public SeaLionRequestDecoder(MapContainer mapContainer) {
		this.mapContainer = mapContainer;
	}

	/*
	 * 
	 * (non-Javadoc)
	 * @see io.netty.handler.codec.ByteToMessageDecoder#decode(io.netty.channel.ChannelHandlerContext, io.netty.buffer.ByteBuf, java.util.List)
	 * @author sunhe
	 * @date 2015年3月14日 下午7:59:33
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (state == TEXT_LINE) {
			if (request == null) {
				request = new SeaLionRequest();
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
			// For now, the text line must have either 5 or 6 words.
			if (words.length < 5 || words.length > 6) {
				isBadRequest = true;
				request.setOpaque(words[2]);
			}
			else {
				try {
					String command = words[0];
					String key = words[1];
					String opaque = words[2];
					int dataLen = Integer.parseInt(words[3]);
					boolean isSafeMode = words[4].equals("0") ? false : true;
					String cas = words.length == 6 ? words[5] : null;
					request.setCommand(command);
					request.setKey(key);
					request.setOpaque(opaque);
					request.setDataLen(dataLen);
					request.setIsSafeMode(isSafeMode);
					request.setCas(cas);
				}
				catch (NumberFormatException e) {
					isBadRequest = true;
				}
			}
			state = DATA_FIELD;
		}
		else {
			// decode the data field
			// Note that '+ 2' is the length of '\r\n'.
			if (in.readableBytes() >= request.getDataLen() + SeaLionRequest.LENGTH_OF_TIME_STAMP + 2) {
				request.setTimeStamp(in.readLong());
				ByteBuf data = mapContainer.allocate(request.getDataLen());
				if (data == null) {
					// Server out of memory.
					SeaLionResponse response = new SeaLionResponse();
					response.setStatus(Statuses.SERVER_OUT_OF_MEMORY);
					response.setOpaque(request.getOpaque());
					response.setDataLen(0);
					response.setData(Unpooled.EMPTY_BUFFER);
					ctx.writeAndFlush(response);
				}
				else {
					in.readBytes(data);
					in.readerIndex(in.readerIndex() + 2);
					if (isBadRequest) {
						mapContainer.release(data);
						SeaLionResponse response = new SeaLionResponse();
						response.setStatus(Statuses.BAD_REQUEST);
						response.setOpaque(request.getOpaque());
						response.setDataLen(0);
						response.setData(Unpooled.EMPTY_BUFFER);
						ctx.writeAndFlush(response);
					}
					else {
						request.setData(data);
						Logger.log(ctx.channel().remoteAddress().toString() + " Decoded request", request.toString());
						out.add(request);
					}
					resetState();
				}
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
		request = null;
		isBadRequest = false;
	}

}
