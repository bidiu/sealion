package io.sunhe.sealion.protocol;

import io.netty.buffer.ByteBuf;
import io.sunhe.sealion.util.TimeStampFormatter;

/**
 * Represents the request sent to SeaLion server instance.
 * 
 * @author sunhe
 * @date 2015年3月14日 下午5:10:23
 */
public class SeaLionRequest {
	
	/**
	 * The time stamp field in request is 8 bytes long.
	 */
	public static final int LENGTH_OF_TIME_STAMP = 8;

	/**
	 * For not, just implements SET, GET and DELETE.
	 */
	private String command;
	
	/**
	 * The unique key
	 */
	private String key;
	
	/**
	 * Server will return this field without any modification.
	 * So the field is usually used to map a response to a request.
	 */
	private String opaque;
	
	/**
	 * The length of unstructured data field in request.
	 */
	private int dataLen;
	
	/**
	 * Is this request in safe mode.
	 */
	private boolean isSafeMode;
	
	/**
	 * CAS
	 * take null value as not having CAS check.
	 */
	private String cas;
	
	/**
	 * The expiration time stamp.
	 * Note that 0 represents that there's not expiration time.
	 */
	private long timeStamp;
	
	private ByteBuf data;
	
	/**
	 * 
	 * @author sunhe
	 * @date 2015年3月14日 下午5:43:29
	 */
	public SeaLionRequest() {
		
	}
	
	/**
	 * 
	 * @param command
	 * @param key
	 * @param dataLen
	 * @param data
	 * @author sunhe
	 * @date 2015年3月14日 下午5:28:07
	 */
	public SeaLionRequest(String command, String key, int dataLen, ByteBuf data, String cas) {
		this.command = command;
		this.key = key;
		this.dataLen = dataLen;
		this.data = data;
		this.cas = cas;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
	
	public String getOpaque() {
		return opaque;
	}
	
	public void setOpaque(String opaque) {
		this.opaque = opaque;
	}

	public int getDataLen() {
		return dataLen;
	}

	public void setDataLen(int dataLen) {
		this.dataLen = dataLen;
	}
	
	public boolean getIsSafeMode() {
		return isSafeMode;
	}
	
	public void setIsSafeMode(boolean isSafeMode) {
		this.isSafeMode = isSafeMode;
	}
	
	public String getCas() {
		return cas;
	}
	
	public void setCas(String cas) {
		this.cas = cas;
	}
	
	public long getTimeStamp() {
		return timeStamp;
	}
	
	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public ByteBuf getData() {
		return data;
	}

	public void setData(ByteBuf data) {
		this.data = data;
	}
	
	/*
	 * Only contains text line part and expiration time stamp.
	 * 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 * @author sunhe
	 * @date 2015年3月15日 上午9:02:23
	 */
	public String toString() {
		return command + " " + key + " " + opaque + " " + dataLen + (isSafeMode ? " 1" : " 0")
				+ (cas == null ? "" : " " + cas) 
				+ " [" + (timeStamp == 0 ? 0 : TimeStampFormatter.fromTimeStampToStrDateTime(timeStamp)) + "]";
	}
	
}
