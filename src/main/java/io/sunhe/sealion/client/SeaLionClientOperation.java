package io.sunhe.sealion.client;

import io.netty.channel.ChannelFuture;
import io.sunhe.sealion.protocol.SeaLionResponse;

/**
 * @author sunhe
 * @date 2015年3月15日 上午11:16:25
 */
public interface SeaLionClientOperation {

	/**
	 * @param key
	 * @return 
	 * 		The string value retrieved with the key.
	 * 		Null, if the key doesn't exist in the server.
	 * @author sunhe
	 * @date 2015年3月15日 下午5:57:33
	 */
	public String getString(String key);
	
	/**
	 * @param key
	 * @return
	 * 		The string value retrieved with the key.
	 * 		Null, if the key doesn't exist in the server etc.
	 * @author sunhe
	 * @date 2015年4月9日 下午1:02:07
	 */
	public String getString(String key, String cas);
	
	/**
	 * Note that you can set a expired key-value to the SeaLion server, 
	 * in this case, the server will substitute the existed key-value, if exists, for the
	 * newly set one. So it's your responsibility to ensure the validity of the given time stamp.
	 * 
	 * @param key
	 * @param value
	 * @param timeStamp The expiration time stamp. 0 signifies never expiring.
	 * @return True if operation is successful, otherwise false.
	 * @author sunhe
	 * @date 2015年3月25日 下午7:30:33
	 */
	public boolean setString(String key, String value, long timeStamp);
	
	/**
	 * Note that you can set a expired key-value to the SeaLion server, 
	 * in this case, the server will substitute the existed key-value, if exists, for the
	 * newly set one. So it's your responsibility to ensure the validity of the given time stamp.
	 * 
	 * @param key
	 * @param value
	 * @param timeStamp The expiration time stamp. 0 signifies never expiring.
	 * @param cas 
	 * @return True if operation is successful, otherwise false.
	 * @author sunhe
	 * @date 2015年4月8日 下午4:20:08
	 */
	public boolean setString(String key, String value, long timeStamp, String cas);
	
	/**
	 * setString's unsafe version.
	 * In unsafe mode, the client doesn't need to wait for the response server send(non-blocking).
	 * 
	 * @param key
	 * @param value
	 * @param timeStamp The expiration time stamp. 0 signifies never expiring.
	 * @return
	 * @author sunhe
	 * @date 2015年3月30日 下午7:31:12
	 */
	public ChannelFuture setStringUnsafe(String key, String value, long timeStamp);
	
	/**
	 * setString's unsafe version.
	 * In unsafe mode, the client doesn't need to wait for the response server send(non-blocking).
	 * 
	 * @param key
	 * @param value
	 * @param timeStamp The expiration time stamp. 0 signifies never expiring.
	 * @param cas
	 * @return
	 * @author sunhe
	 * @date 2015年4月8日 下午4:23:00
	 */
	public ChannelFuture setStringUnsafe(String key, String value, long timeStamp, String cas);
	
	/**
	 * @param key
	 * @return
	 * 		The byte array retrieved with the key.
	 * 		Null, if the key doesn't exist in the server.
	 * @author sunhe
	 * @date 2015年3月29日 下午6:43:09
	 */
	public byte[] getBytes(String key);
	
	/**
	 * @param key
	 * @param cas
	 * @return
	 * 		The byte array retrieved with the key.
	 * 		Null, if the key doesn't exist in the server or not passes 
	 * 		CAS check or the method is interrupted etc.
	 * @author sunhe
	 * @date 2015年4月9日 下午12:57:34
	 */
	public byte[] getBytes(String key, String cas);
	
	/**
	 * Get the bytes, corresponding to the given key, and its CAS check value.
	 * 
	 * @param key
	 * @return 
	 * 		The response sent by remote peer server directly. 
	 * 		Note that the returned response will be null if there's a connection error or something 
	 * 		else. Additionally, the data field of the returned response may be null if
	 * 		the given parameter key is not stored in the server. It's your responsibility 
	 * 		to check all of those sort of situations before using it by checking the status field 
	 * 		of the response.
	 * @author sunhe
	 * @date 2015年4月9日 下午12:18:44
	 */
	public SeaLionResponse getBytesAndCas(String key);
	
	/**
	 * Get the bytes, corresponding to the given key, and its CAS check value.
	 * 
	 * @param key
	 * @param cas
	 * @return
	 * 		The response sent by remote peer server directly. 
	 * 		Note that the returned response will be null if there's a connection error or something 
	 * 		else. Additionally, the data field of the returned response may be null if
	 * 		the given parameter key is not stored in the server. It's your responsibility 
	 * 		to check all of those sort of situations before using it by checking the status field 
	 * 		of the response.
	 * @author sunhe
	 * @date 2015年4月9日 下午1:00:52
	 */
	public SeaLionResponse getBytesAndCas(String key, String cas);
	
	/**
	 * Note that you can set a expired key-value to the SeaLion server, 
	 * in this case, the server will substitute the existed key-value, if exists, for the
	 * newly set one. So it's your responsibility to ensure the validity of the given time stamp.
	 * 
	 * @param key
	 * @param bytes
	 * @param timeStamp The expiration time stamp. 0 signifies never expiring.
	 * @return
	 * @author sunhe
	 * @date 2015年3月28日 下午10:45:01
	 */
	public boolean setBytes(String key, byte[] value, long timeStamp);
	
	/**
	 * Note that you can set a expired key-value to the SeaLion server, 
	 * in this case, the server will substitute the existed key-value, if exists, for the
	 * newly set one. So it's your responsibility to ensure the validity of the given time stamp.
	 * 
	 * @param key
	 * @param value
	 * @param timeStamp The expiration time stamp. 0 signifies never expiring.
	 * @param cas
	 * @return
	 * @author sunhe
	 * @date 2015年4月8日 下午4:24:43
	 */
	public boolean setBytes(String key, byte[] value, long timeStamp, String cas);
	
	/**
	 * setBytes's unsafe version.
	 * In unsafe mode, the client doesn't need to wait for the response server send(non-blocking).
	 * 
	 * @param key
	 * @param value
	 * @param timeStamp The expiration time stamp. 0 signifies never expiring.
	 * @return
	 * @author sunhe
	 * @date 2015年3月30日 下午7:29:40
	 */
	public ChannelFuture setBytesUnsafe(String key, byte[] value, long timeStamp);
	
	/**
	 * setBytes's unsafe version.
	 * In unsafe mode, the client doesn't need to wait for the response server send(non-blocking).
	 * 
	 * @param key
	 * @param value
	 * @param timeStamp The expiration time stamp. 0 signifies never expiring.
	 * @param cas
	 * @return
	 * @author sunhe
	 * @date 2015年4月8日 下午4:27:57
	 */
	public ChannelFuture setBytesUnsafe(String key, byte[] value, long timeStamp, String cas);
	
	/**
	 * @param key
	 * @return True if operation is successful, otherwise false.
	 * @author sunhe
	 * @date 2015年3月15日 下午6:04:32
	 */
	public boolean deleteValue(String key);
	
	/**
	 * @param key
	 * @param cas
	 * @return True if operation is successful, otherwise false.
	 * @author sunhe
	 * @date 2015年4月8日 下午4:31:47
	 */
	public boolean deleteValue(String key, String cas);
	
	/**
	 * deleteValue's unsafe version.
	 * In unsafe mode, the client doesn't need to wait for the response server send(non-blocking).
	 * 
	 * @param key
	 * @return
	 * @author sunhe
	 * @date 2015年3月30日 下午7:29:03
	 */
	public ChannelFuture deleteValueUnsafe(String key);
	
	/**
	 * deleteValue's unsafe version.
	 * In unsafe mode, the client doesn't need to wait for the response server send(non-blocking).
	 * 
	 * @param key
	 * @param cas
	 * @return
	 * @author sunhe
	 * @date 2015年4月8日 下午4:33:36
	 */
	public ChannelFuture deleteValueUnsafe(String key, String cas);
	
	/**
	 * Get CAS value of a specific key-value pair.
	 * 
	 * @param key
	 * @return 
	 * 		The returned CAS value. Null if connection is broken 
	 * 		or current thread is interrupted or the key doesn't 
	 * 		exist in the server.
	 * @author sunhe
	 * @date 2015年4月9日 下午1:23:54
	 */
	public String getCas(String key);
	
}
