package io.sunhe.sealion.protocol;

import io.netty.buffer.ByteBuf;

/**
 * Represents the server request.
 * 
 * @author sunhe
 * @date 2015年3月14日 下午5:29:39
 */
public class SeaLionResponse {
	
	/**
	 * Response status string.
	 */
	private String status;
	
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
	 * CAS
	 * Take null as not containing CAS value.
	 */
	private String cas;
	
	private ByteBuf data;
	
	/**
	 * 
	 * @author sunhe
	 * @date 2015年3月14日 下午5:43:21
	 */
	public SeaLionResponse() {
		
	}
	
	public SeaLionResponse(String status, int dataLen, ByteBuf data, String cas) {
		this.status = status;
		this.dataLen = dataLen;
		this.data = data;
		this.cas = cas;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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
	
	public String getCas() {
		return cas;
	}
	
	public void setCas(String cas) {
		this.cas = cas;
	}

	public ByteBuf getData() {
		return data;
	}

	public void setData(ByteBuf data) {
		this.data = data;
	}
	
	/*
	 * Only contains the text line part.
	 * 
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 * @author sunhe
	 * @date 2015年3月15日 上午9:03:46
	 */
	public String toString() {
		return status + " " + opaque + " " + dataLen + (cas == null ? "" : " " + cas);
	}
	
}
