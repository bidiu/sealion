package io.sunhe.sealion.server.mem;

/**
 * The abstraction of the unstructured data holder.
 * Implementation can be not thread-safe, because the SeaLion server's worker thread is single thread model.
 * 
 * @author sunhe
 * @date 2015年3月22日 下午9:04:21
 */
public interface Item {
	
	/**
	 * Get the corresponding key of the item.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年3月26日 下午8:40:41
	 */
	public String getKey();
	
	/**
	 * Set the corresponding key of the item.
	 * 
	 * @param key
	 * @author sunhe
	 * @date 2015年3月26日 下午8:41:43
	 */
	public void setKey(String key);

	/**
	 * Get the number of byte of the backing ByteBuf(s).
	 * 
	 * @return the number byte of the back-up ByteBuf
	 * @author sunhe
	 * @date 2015年3月22日 下午9:02:00
	 */
	public int getDataSize();
	
	/**
	 * Set the expiration time stamp of the item.
	 * 
	 * @param timeStamp
	 * @author sunhe
	 * @date 2015年3月25日 下午2:49:38
	 */
	public void setTimeStamp(long timeStamp);
	
	/**
	 * Set the expiration time stamp of the item.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年3月25日 下午2:51:12
	 */
	public long getTimeStamp();
	
	/**
	 * Get the expiration time stamp in string format of the item.
	 * The format should be:
	 * 		"(time stamp)(opaque)"
	 * 		* Not includes the parenthesis.
	 * 		* Exception: the 0 (never expire) time stamp doesn't need to append the opaque.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年3月25日 下午2:51:57
	 */
	public String getStrTimeStamp();
	
	/**
	 * Get the item's CAS.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年4月8日 下午1:53:21
	 */
	public String getCas();
	
	/**
	 * Refresh a new CAS for this item and return it.
	 * 
	 * @return The newly-genrated CAS.
	 * @author sunhe
	 * @date 2015年4月8日 下午1:56:01
	 */
	public String refreshCas();
	
	/**
	 * The double linked list method, supporting the LRU queue.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年3月25日 下午8:24:59
	 */
	public Item getPrior();
	public void setPrior(Item prior);
	public Item getNext();
	public void setNext(Item next);
	
}
