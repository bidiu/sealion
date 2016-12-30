package io.sunhe.sealion.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.sunhe.sealion.protocol.Commands;
import io.sunhe.sealion.protocol.SeaLionRequest;
import io.sunhe.sealion.protocol.SeaLionResponse;
import io.sunhe.sealion.protocol.Statuses;
import io.sunhe.sealion.server.dashboard.HitRatioCollector;
import io.sunhe.sealion.server.mem.Item;
import io.sunhe.sealion.server.mem.MapContainer;
import io.sunhe.sealion.server.mem.SimpleItem;
import io.sunhe.sealion.util.Logger;
import io.sunhe.sealion.util.TimeStampFormatter;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * The task worker thread execute.
 * Note that worker thread should be a single-thread pool, 
 * so worker thread must not be blocked.
 * 
 * Not thread-safe. It's okay because only the thread to bootstrap the SeaLion server 
 * passes this Runnable-implementation class to the worker executor, which is a single 
 * thread executor.
 * 
 * @author sunhe
 * @date 2015年3月15日 上午8:41:57
 */
public class WorkerTask implements Runnable {

	/**
	 * task queue
	 */
	private BlockingQueue<Task> taskQueue;
	
	private MapContainer mapContainer;
	
	private Map<String, Item> map;
	
	private ConcurrentSkipListMap<String, String> timeStampListMap;
	
	private HitRatioCollector hitRatioCollector;
	
	public WorkerTask(BlockingQueue<Task> taskQueue, MapContainer mapContainer, HitRatioCollector hitRatioCollector) {
		this.taskQueue = taskQueue;
		this.mapContainer = mapContainer;
		map = mapContainer.getMap();
		timeStampListMap = mapContainer.getTimeStampListMap();
		this.hitRatioCollector = hitRatioCollector;
	}

	public void run() {
		try {
			while (true) {
				Task task = taskQueue.take();
				processTask(task);
				// May stop the world.
				mapContainer.ensureMemory();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	public void processTask(Task task) {
		SeaLionRequest request = task.getSeaLionRequest();
		Channel channel = task.getChannel();
		Logger.log(channel.remoteAddress().toString() + " Processing request", request.toString());
		
		String command = request.getCommand();
		String key = request.getKey();
		boolean isSafeMode = request.getIsSafeMode();
		String casInReq = request.getCas();
		long timeStamp = request.getTimeStamp();
		ByteBuf data = request.getData();
		SeaLionResponse response = new SeaLionResponse();
		response.setOpaque(request.getOpaque());
		
		if (Commands.GET.equals(command)) {
			// GET command
			// If the GET command has CAS check and the key doesn't exist 
			// in the server(may expired or deleted by other client or thread), then take 
			// this situation as KEY_NONEXISTS, not CAS_FAILURE.
			if (map.containsKey(key) && !checkExpiration(key)) {
				// hit.
				hitRatioCollector.hit();
				SimpleItem item = (SimpleItem) map.get(key);
				String casInItem = item.getCas();
				// maintain LRU list
				mapContainer.moveItem(item);
				if (casInReq == null || casInItem.equals(casInReq)) {
					// Passed CAS check.
					data = item.getData();
					response.setStatus(Statuses.SUCCESS);
					response.setDataLen(data.capacity());
					response.setCas(casInItem);
					response.setData(data);
				}
				else {
					// Not passes CAS check.
					response.setStatus(Statuses.CAS_FAILURE);
					response.setDataLen(0);
					response.setCas(casInItem);
					response.setData(Unpooled.EMPTY_BUFFER);
				}
			}
			else {
				// miss.
				hitRatioCollector.miss();
				response.setStatus(Statuses.KEY_NONEXISTS);
				response.setDataLen(0);
				response.setData(Unpooled.EMPTY_BUFFER);
			}
		}
		else if (Commands.SET.equals(command)) {
			// SET command.
			SimpleItem item = (SimpleItem) map.get(key);
			String casInItem = item == null ? null : item.getCas();
			if ((casInReq == null) || (item != null && casInItem.equals(casInReq))) {
				// Passed CAS check.
				if (map.containsKey(key)) {
					// Note that the old item to the given key, if exists, will be abandoned.
					String strTimeStamp = map.get(key).getStrTimeStamp();
					if (! strTimeStamp.equals("0")) {
						// maintain time stamp list
						timeStampListMap.remove(strTimeStamp);			
					}
					// release pooled buffer
					mapContainer.releaseBufByKey(key);
					// maintain LRU list
					mapContainer.removeItemByKey(key);
				}
				item = new SimpleItem();
				item.setKey(key);
				item.setData(data);
				item.setTimeStamp(timeStamp);
				// Generate a CAS value.
				casInItem = item.refreshCas();
				map.put(key, item);
				// maintain LRU list
				mapContainer.insertItem(item);
				// maintain time stamp list
				if (timeStamp != 0) {
					timeStampListMap.put(item.getStrTimeStamp(), key);
				}
				response.setStatus(Statuses.SUCCESS);
				response.setDataLen(0);
				response.setCas(casInItem);
				response.setData(Unpooled.EMPTY_BUFFER);
			}
			else {
				// Not passes CAS check.
				response.setStatus(Statuses.CAS_FAILURE);
				response.setDataLen(0);
				response.setCas(casInItem);
				response.setData(Unpooled.EMPTY_BUFFER);
			}
		}
		else if (Commands.DELETE.equals(command)) {
			// DELETE command
			// If the DELETE command has CAS check and the key doesn't exist 
			// in the server(may expired or deleted by other client or thread), then take 
			// this situation as KEY_NONEXISTS, not CAS_FAILURE.
			if (map.containsKey(key) && !checkExpiration(key)) {
				// hit.
				hitRatioCollector.hit();
				String casInItem = map.get(key).getCas();
				if (casInReq == null || casInItem.equals(casInReq)) {
					// Passed CAS check.
					// release pooled buffer
					mapContainer.releaseBufByKey(key);
					// maintain LRU list
					mapContainer.removeItemByKey(key);
					String strTimeStamp = map.get(key).getStrTimeStamp();
					if (! strTimeStamp.equals("0")) {
						// maintain time stamp list
						timeStampListMap.remove(strTimeStamp);
					}
					map.remove(key);
					response.setStatus(Statuses.SUCCESS);
					response.setDataLen(0);
					response.setData(Unpooled.EMPTY_BUFFER);
				}
				else {
					// Not passes CAS check.
					response.setStatus(Statuses.CAS_FAILURE);
					response.setDataLen(0);
					response.setCas(casInItem);
					response.setData(Unpooled.EMPTY_BUFFER);
				}
			}
			else {
				// miss.
				hitRatioCollector.miss();
				response.setStatus(Statuses.KEY_NONEXISTS);
				response.setDataLen(0);
				response.setData(Unpooled.EMPTY_BUFFER);
			}
		}
		else if (Commands.CAS.equals(command)) {
			// CAS command
			// Note that if the CAS command also has the CAS check value 
			// in its request POJO, then ignore it.
			if (map.containsKey(key) && !checkExpiration(key)) {
				// hit.
				hitRatioCollector.hit();
				SimpleItem item = (SimpleItem) map.get(key);
				// maintain LRU list
				mapContainer.moveItem(item);
				String casInItem = item.getCas();
				response.setStatus(Statuses.SUCCESS);
				response.setDataLen(0);
				response.setCas(casInItem);
				response.setData(Unpooled.EMPTY_BUFFER);
			}
			else {
				// miss.
				hitRatioCollector.miss();
				response.setStatus(Statuses.KEY_NONEXISTS);
				response.setDataLen(0);
				response.setData(Unpooled.EMPTY_BUFFER);
			}
		}
		else {
			response.setStatus(Statuses.BAD_REQUEST);
			response.setDataLen(0);
			response.setData(Unpooled.EMPTY_BUFFER);
		}
		if (isSafeMode) {
			channel.writeAndFlush(response);
		}
	}
	
	/**
	 * Determine the item mapped by the given key expired or not.
	 * If so, the key-item tuple and the strTimeStamp-key tuple will be deleted.
	 * Note that the parameter key is supposed to be contained in the map.
	 * 
	 * @param key 
	 * @return True if the mapped item expired, otherwise false.
	 * @author sunhe
	 * @date 2015年3月27日 上午11:47:10
	 */
	private boolean checkExpiration(String key) {
		SimpleItem item = (SimpleItem) map.get(key);
		long timeStamp = item.getTimeStamp();
		if (timeStamp == 0) {
			return false;
		}
		else if (timeStamp <= TimeStampFormatter.getCurrentTimeStamp()) {
			// release pooled buffer
			mapContainer.release(item.getData());
			// maintain LRU list
			mapContainer.removeItem(item);
			// maintain time stamp list
			timeStampListMap.remove(item.getStrTimeStamp());
			map.remove(key);
			return true;
		}
		else {
			return false;
		}
	}
	
}



