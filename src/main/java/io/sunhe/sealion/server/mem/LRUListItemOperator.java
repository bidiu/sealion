package io.sunhe.sealion.server.mem;

/**
 * Interface definition of operation 
 * to manipulate item in the LRU list when traverse through it.
 * 
 * @author sunhe
 * @date 2015年3月31日 上午10:04:29
 */
public interface LRUListItemOperator {
	
	/**
	 * You are allowed to remove this item from the 
	 * LRU list by MapContainer.removeItem(Item), and note that you 
	 * should also remove the key-value from the MapContainer.map and 
	 * release the buffer's pooled memory.
	 * 
	 * You should NOT to change the structure of the LRU list by the parameter 
	 * item's prior and next reference.
	 * 
	 * @param item
	 * @return Return true the traversal will continue, otherwise it won't.
	 * @author sunhe
	 * @date 2015年3月31日 上午10:06:23
	 */
	public boolean operate(Item item);

}
