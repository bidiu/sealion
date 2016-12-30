package io.sunhe.sealion.server.mem;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import io.sunhe.sealion.server.dashboard.BlockingTimeAccumulator;
import io.sunhe.sealion.util.TimeStampFormatter;

import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Should be thread-safe.
 * 
 * @author sunhe
 * @date 2015年3月15日 上午8:52:50
 */
public class MapContainer {
	
	/**
	 * 256 MBytes
	 */
	public static final long DEFAULT_MAX_MEM_SIZE = 268435456;
	
	/**
	 * 16 MBytes
	 */
	public static final long MIN_MEM_SIZE = 16777216;
	
	private Integer memoryLock = 1113;
	
	/**
	 * In byte.
	 * Cann't be changed during uptime.
	 */
	private long maxMemSize;
	
	/**
	 * In byte.
	 */
	private long curMemSize;
	
	/**
	 * Note that only one thread at a time can access the LRU list, 
	 * 'cause it's not thread-safe.
	 */
	private Item LRUListHead;
	private Item LRUListTail;
	private long LRUListSize;
	
	/**
	 * Map strTimeStamp to key.
	 */
	private ConcurrentSkipListMap<String, String> timeStampListMap;

	/**
	 * Note that although this class is supposed to be thread-safe, 
	 * this field is not thread-safe, because it's a HashMap.
	 * Consequently only the worker thread can access this field, which is a single thread pool.
	 */
	private Map<String, Item> map;
	
	/**
	 * Only one thread will access this field.
	 */
	private PooledByteBufAllocator allocator;
	
	/**
	 * The buffer list to be released
	 * Not thread-safe.
	 */
	private List<ByteBuf> bufList;
	
	/**
	 * The single thread executor to allocate and release pooled memory(byte buffer).
	 */
	private volatile ExecutorService memExecutor;
	
	/**
	 * Memory collector thread's future.
	 * Only worker thread will access this field. 
	 */
	private Future<?> memCollectorFuture;
	
	/**
	 * The runnable task that release(collect) the unused buffer to the pool.
	 * Thread safe.
	 */
	private Runnable collectorRunnable;
	
	private BlockingTimeAccumulator blockingTimeAccumulator;
	
	/**
	 * Runnable task to accumulate the blocking time.
	 */
	private Runnable accumulatorRunnable;
	
	/**
	 * Runnable task to clear the blocking time.
	 */
	private Runnable clearRunnable;
	
	/**
	 * The executor to schedule the accumulatorRunnable.
	 */
	private ScheduledExecutorService scheduledExecutor;
	
	private Future<?> accumulatorFuture;
	
	public MapContainer(Map<String, Item> map, BlockingTimeAccumulator blockingTimeAccumulator) {
		this (map, blockingTimeAccumulator, DEFAULT_MAX_MEM_SIZE);
	}
	
	public MapContainer(Map<String, Item> map, BlockingTimeAccumulator blockingTimeAccumulator, 
			long maxMemSize) {
		if (maxMemSize < MIN_MEM_SIZE) {
			throw new IllegalArgumentException("SeaLion must have at least 16 MB memory space: " + maxMemSize);
		}
		this.map = map;
		this.maxMemSize = maxMemSize;
		allocator = new PooledByteBufAllocator(true);
		timeStampListMap = new ConcurrentSkipListMap<String, String>(new Comparator<String>() {

			public int compare(String key1, String key2) {
				key1 = key1.substring(0, key1.length() - 4);
				key2 = key2.substring(0, key2.length() - 4);
				long timeStamp1 = Long.valueOf(key1);
				long timeStamp2 = Long.valueOf(key2);
				return (int) (timeStamp1 - timeStamp2);
			}
			
		});
		bufList = new LinkedList<ByteBuf>();
		memExecutor = Executors.newSingleThreadExecutor();
		collectorRunnable = new Runnable() {
			
			public void run() {
				// Ensure the visibility cross threads.
				synchronized (bufList) {
					Iterator<ByteBuf> iterator = bufList.iterator();
					ByteBuf buf;
					while (iterator.hasNext()) {
						buf = iterator.next();
						decreCurMemSize(buf.capacity());
						ReferenceCountUtil.release(buf);
						iterator.remove();
					}
				}
			}
			
		};
		this.blockingTimeAccumulator = blockingTimeAccumulator;
		accumulatorRunnable = new Runnable() {
			
			/*
			 * Will be scheduled to be executed for every 1 sec.
			 * 
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 * @author sunhe
			 * @date 2015年4月16日 下午10:00:15
			 */
			public void run() {
				MapContainer.this.blockingTimeAccumulator.accumulate();
			}
			
		};
		clearRunnable = new Runnable() {
			
			/*
			 * Will be scheduled to be executed for 1 sec delay.
			 * 
			 * (non-Javadoc)
			 * @see java.lang.Runnable#run()
			 * @author sunhe
			 * @date 2015年4月16日 下午10:16:39
			 */
			public void run() {
				MapContainer.this.blockingTimeAccumulator.clear();
			}
			
		};
		scheduledExecutor = Executors.newScheduledThreadPool(1);
	}
	
	/**
	 * @author sunhe
	 * @date 2015年4月17日 上午9:32:45
	 */
	private void startAccumulator() {
		accumulatorFuture = scheduledExecutor.scheduleAtFixedRate(accumulatorRunnable, 
					1, 1, TimeUnit.SECONDS);
	}
	
	/**
	 * @author sunhe
	 * @date 2015年4月17日 上午9:16:03
	 */
	private void stopAccumulator() {
		accumulatorFuture.cancel(false);
		scheduledExecutor.schedule(clearRunnable, 1, TimeUnit.SECONDS);
	}
	
	/**
	 * Note that ONLY worker thread should call this method.
	 * 
	 * @author sunhe
	 * @date 2015年3月31日 下午8:48:58
	 */
	public void ensureMemory() {
		if (getMemPercentage() <= 0.6
				|| (memCollectorFuture != null && !memCollectorFuture.isDone())) {
			return;
		}
		// Start to accumulate the blocking time.
		startAccumulator();
		Set<Map.Entry<String, String>> entrySet = timeStampListMap.entrySet();
		Iterator<Map.Entry<String, String>> iterator = entrySet.iterator();
		Map.Entry<String, String> entry;
		String strTimeStamp, key;
		SimpleItem item;
		long timeStamp, curTimeStamp = TimeStampFormatter.getCurrentTimeStamp();
		// ensure the visibility cross threads.
		synchronized (bufList) {
			while (iterator.hasNext()) {
				entry = iterator.next();
				strTimeStamp = entry.getKey();
				key = entry.getValue();
				timeStamp = Long.valueOf(strTimeStamp.substring(0, strTimeStamp.length() - 4));
				if (timeStamp <= curTimeStamp) {
					// expired
					// remove the strTimeStamp-key from time-stamp list.
					iterator.remove();
					// maintain LRU list
					removeItemByKey(key);
					// remove the item from map
					item = (SimpleItem) map.remove(key);
					bufList.add(item.getData());
				}
				else {
					break;
				}
			}
		}
		memCollectorFuture = memExecutor.submit(collectorRunnable);
		if (getMemPercentage() <= 0.95) {
			// Stop accumulating the blocking time.
			stopAccumulator();
			return;
		}
		// Going to exceed the threshold, so be about to stop the world.
		// Ensure that this GC will reduce the memory occupation rate down to 75%.
		// Will block all attempt to apply for more memory, in other words, 
		// all newly-received requests will have to suspend.
		synchronized (memoryLock) {
			try {
				memCollectorFuture.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			if (getMemPercentage() <= 0.75) {
				return;
			}
			final long targetSize = (long) (getMaxMemSize() * (getMemPercentage() - 0.75));
			synchronized (bufList) {
				traverseLRUListReversely(new LRUListItemOperator() {
					
					private long curSize = 0;
					
					public boolean operate(Item item) {
						if (curSize >= targetSize) {
							return false;
						}
						else {
							// maintain the LRU list.
							removeItem(item);
							// remove the strTimeStamp-key from time-stamp list.
							timeStampListMap.remove(item.getStrTimeStamp());
							// remove the item from map
							map.remove(item.getKey());
							bufList.add(((SimpleItem) item).getData());
							curSize += item.getDataSize();
							return true;
						}
					}
					
				});
			}
			memCollectorFuture = memExecutor.submit(collectorRunnable);
			try {
				memCollectorFuture.get();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		// Stop accumulating the blocking time.
		stopAccumulator();
	}
	
	/**
	 * Note that this method can ONLY be called by the worker thread, 
	 * which is a single thread pool.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年3月22日 下午10:51:26
	 */
	public Map<String, Item> getMap() {
		return map;
	}
	
	/**
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年3月22日 下午10:43:06
	 */
	public synchronized long getCurMemSize() {
		return curMemSize;
	}
	
	/**
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年3月23日 上午9:07:31
	 */
	public long getMaxMemSize() {
		return maxMemSize;
	}
	
	/**
	 * 
	 * @author sunhe
	 * @date 2015年3月22日 下午10:42:17
	 */
	private synchronized void increCurMemSize(long increment) {
		curMemSize += increment;
	}
	
	/**
	 * 
	 * @param decrement
	 * @author sunhe
	 * @date 2015年3月22日 下午10:46:24
	 */
	private synchronized void decreCurMemSize(long decrement) {
		if (curMemSize >= decrement) {
			curMemSize -= decrement;
		}
		else {
			curMemSize = 0;
		}
	}
	
	/**
	 * Release the buffer, decrementing the reference count by 1.
	 * 
	 * @param buf
	 * @author sunhe
	 * @date 2015年3月22日 下午10:26:13
	 */
	public void release(final ByteBuf buf) {
		decreCurMemSize(buf.capacity());
		memExecutor.submit(new Runnable() {
			
			public void run() {
				ReferenceCountUtil.release(buf);
			}
			
		});
	}
	
	/**
	 * Release the pooled byte buffer in a item by the item's corresponding key.
	 * 
	 * @param key
	 * @author sunhe
	 * @date 2015年3月25日 下午9:48:54
	 */
	public synchronized void releaseBufByKey(String key) {
		if (map.containsKey(key)) {
			ByteBuf buf = ((SimpleItem) map.get(key)).getData();
			release(buf);
		}
	}
	
	/**
	 * Allocate a byte buffer of specified size, which is suitable for I/O operation. 
	 * Note the returned byte buffer is pooled, so you MUST release them when you don't use them any more.
	 * 
	 * @param capacity The size in byte of the byte buffer to be allocated.
	 * @return The allocated byte buffer. Null if operation failed.
	 * @author sunhe
	 * @date 2015年4月1日 下午2:58:06
	 */
	public ByteBuf allocate(final int capacity) {
		synchronized (memoryLock) {
			increCurMemSize(capacity);
			ByteBuf buf = null;
			try {
				buf = memExecutor.submit(new Callable<ByteBuf>() {
	
					public ByteBuf call() throws Exception {
						return allocator.ioBuffer(capacity, capacity);
					}
					
				}).get();
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			return buf;
		}
	}
	
	/**
	 * Get current memory occupation percentage.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年3月23日 下午3:02:43
	 */
	public synchronized double getMemPercentage() {
		return (double) curMemSize / (double) maxMemSize;
	}
	
	/**
	 * Get the size of LRU list.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年3月30日 上午11:09:02
	 */
	public synchronized long getLRUListSize() {
		return LRUListSize;
	}
	
	/**
	 * Insert a new item in the head position of the LRU list
	 * 
	 * @param item
	 * @author sunhe
	 * @date 2015年3月26日 下午9:05:49
	 */
	public synchronized void insertItem(Item item) {
		if (LRUListSize == 0) {
			LRUListHead = LRUListTail = item;
		}
		else {
			item.setNext(LRUListHead);
			LRUListHead.setPrior(item);
			LRUListHead = item;
		}
		LRUListSize++;
	}
	
	/**
	 * Move an item in the LRU list to its head position.
	 * 
	 * @param item
	 * @author sunhe
	 * @date 2015年3月26日 下午9:17:10
	 */
	public synchronized void moveItem(Item item) {
		if (LRUListSize == 1 || item.getPrior() == null) {
			return;
		}
		if (item.getNext() == null) {
			LRUListTail = item.getPrior();
		}
		item.getPrior().setNext(item.getNext());
		if (item.getNext() != null) {
			item.getNext().setPrior(item.getPrior());
		}
		item.setPrior(null);
		item.setNext(LRUListHead);
		LRUListHead.setPrior(item);
		LRUListHead = item;
	}
	
	/**
	 * Remove an item from LRU list.
	 * 
	 * @param item
	 * @author sunhe
	 * @date 2015年3月26日 下午9:31:49
	 */
	public synchronized void removeItem(Item item) {
		if (LRUListSize == 1) {
			LRUListHead = LRUListTail = null;
			LRUListSize--;
			return;
		}
		if (item.getPrior() == null) {
			item.getNext().setPrior(null);
			LRUListHead = item.getNext();
		}
		else if (item.getNext() == null) {
			item.getPrior().setNext(null);
			LRUListTail = item.getPrior();
		}
		else {
			item.getPrior().setNext(item.getNext());
			item.getNext().setPrior(item.getPrior());
		}
		LRUListSize--;
	}
	
	/**
	 * Remove an item from LRU list by item's corresponding key.
	 * 
	 * @param key
	 * @return True if the key exists in the map container, otherwise false.
	 * @author sunhe
	 * @date 2015年3月26日 下午10:30:07
	 */
	public synchronized boolean removeItemByKey(String key) {
		Item item = map.get(key);
		if (item == null) {
			return false;
		}
		else {
			removeItem(item);
			return true;
		}
	}
	
	/**
	 * @author sunhe
	 * @date 2015年3月26日 下午9:49:29
	 */
	public synchronized void traverseLRUList(LRUListItemOperator operator) {
		Item ref = LRUListHead, nextRef;
		boolean shouldContinue = true;
		while (ref != null && shouldContinue) {
			nextRef = ref.getNext();
			shouldContinue = operator.operate(ref);
			ref = nextRef;
		}
	}
	
	/**
	 * @author sunhe
	 * @date 2015年3月26日 下午9:52:11
	 */
	public synchronized void traverseLRUListReversely(LRUListItemOperator operator) {
		Item ref = LRUListTail, priorRef;
		boolean shouldContinue = true;
		while (ref != null && shouldContinue) {
			priorRef = ref.getPrior();
			shouldContinue = operator.operate(ref);
			ref = priorRef;
		}
	}
	
	/**
	 * Only worker thread is supposed to call 
	 * this method.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年3月27日 上午10:47:24
	 */
	public ConcurrentSkipListMap<String, String> getTimeStampListMap() {
		return timeStampListMap;
	}
	
}
