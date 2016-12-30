package io.sunhe.sealion.server.mem;

import io.netty.buffer.ByteBuf;
import io.sunhe.sealion.util.OpaqueGenerator;

/**
 * The item implementation to store a single key-value item.
 * Not thread-safe.
 * 
 * @author sunhe
 * @date 2015年3月25日 下午8:27:04
 */
public class SimpleItem implements Item {
	
	private String key;
	
	private ByteBuf data;
	
	/**
	 * The size in byte of the backing ByteBuf.
	 */
	private int dataSize;
	
	/**
	 * The expiration time stamp.
	 */
	private long timeStamp;
	
	/**
	 * See the java doc of getStrTimeStamp in the super class.
	 */
	private String strTimeStamp;
	
	/**
	 * CAS
	 */
	private String cas;
	
	private Item prior;
	
	private Item next;

	public int getDataSize() {
		return dataSize;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
		if (timeStamp == 0) {
			this.strTimeStamp = "0";
		}
		else {
			this.strTimeStamp = timeStamp + OpaqueGenerator.generate();
		}
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public String getStrTimeStamp() {
		return strTimeStamp;
	}

	public Item getPrior() {
		return prior;
	}

	public void setPrior(Item prior) {
		this.prior = prior;
	}

	public Item getNext() {
		return next;
	}

	public void setNext(Item next) {
		this.next = next;
	}
	
	public ByteBuf getData() {
		return data;
	}
	
	public void setData(ByteBuf data) {
		this.data = data;
		dataSize = data.capacity();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getCas() {
		if (cas == null) {
			throw new IllegalStateException("This item doesn't have a valid CAS value: " + cas);
		}
		return cas;
	}

	public String refreshCas() {
		cas = OpaqueGenerator.generate();
		return cas;
	}
	
}
