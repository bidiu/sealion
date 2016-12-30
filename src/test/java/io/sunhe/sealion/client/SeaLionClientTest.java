package io.sunhe.sealion.client;

import static org.junit.Assert.*;
import io.sunhe.sealion.util.Charsets;

import org.junit.Ignore;
import org.junit.Test;

// TODO 测试时间戳替换、LRU替换的正确性 Done.
// TODO 申请、释放内存的线程模型的正确性，性能是否过差
// TODO 重现内存减少的场景,使用更改内存分配/释放线程模型之前的系统测试，是否会发生内存减少的情况
public class SeaLionClientTest {

	@Ignore
	@Test
	public void test() throws InterruptedException {
		SeaLionClient client = new SeaLionClient("127.0.0.1", 1113);
		String str1 = "hello,world";
		String str2 = "stackoverflow";
		String str3 = "segmentfault";
		client.setString(str1, str2, 0);
		assertTrue(str2.equals(client.getString(str1)));
		client.deleteValue(str1);
		assertNull(client.getString(str1));
		client.setBytes(str1, str3.getBytes(Charsets.UTF_8), 1);
		assertTrue(str3.equals(new String(client.getBytes(str1), Charsets.UTF_8)));
		Thread.sleep(1000 * 60);
		assertNull(client.getBytes(str1));
	}

}
