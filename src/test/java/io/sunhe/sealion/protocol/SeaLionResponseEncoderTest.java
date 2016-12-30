/**
 * 
 */
package io.sunhe.sealion.protocol;

import static org.junit.Assert.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.sunhe.sealion.util.Charsets;

import org.junit.Ignore;
import org.junit.Test;

/**
 * 
 * @author sunhe
 * @date 2015年3月16日 上午9:46:42
 */
public class SeaLionResponseEncoderTest {

	/**
	 * Test method for {@link io.sunhe.sealion.protocol.SeaLionResponseEncoder#encode(io.netty.channel.ChannelHandlerContext, io.sunhe.sealion.protocol.SeaLionResponse, io.netty.buffer.ByteBuf)}.
	 */
	@Ignore
	@Test
	public void testEncodeChannelHandlerContextSeaLionResponseByteBuf() {
		SeaLionResponse response = new SeaLionResponse();
		response.setStatus(Statuses.SUCCESS);
		ByteBuf data = Unpooled.wrappedBuffer("hello, world".getBytes(Charsets.UTF_8));
		response.setDataLen(data.capacity());
		response.setData(data);
		
		EmbeddedChannel channel = new EmbeddedChannel(new SeaLionResponseEncoder());
		
		assertTrue(channel.writeOutbound(response));
		assertTrue(channel.finish());
		
		ByteBuf buf = (ByteBuf) channel.readOutbound();
		System.out.println(ByteBufUtil.hexDump(Unpooled.copiedBuffer(data)));
		System.out.println(ByteBufUtil.hexDump(buf));
		assertEquals(data.capacity(), buf.readableBytes());
		
		assertFalse(buf.isReadable());
		assertNull(channel.readOutbound());
	}

}
