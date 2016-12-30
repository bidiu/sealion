package io.sunhe.sealion.client.hash;

import io.sunhe.sealion.client.SeaLionServerNode;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import net.spy.memcached.DefaultHashAlgorithm;

/**
 * Should be thread-safe.
 * Note that although the nodes and hashLoop are all synchronized by themselves, 
 * but the two fields' state must be changed simultaneously, so some extra synchronization may be needed.
 * 
 * @author sunhe
 * @date 2015年3月19日 上午8:13:35
 */
public class HashBalancer {
	
	private static final int WEIGHT_NORMAL_FACTOR = 1;
	private static final double WEIGHT_HIGH_FACTOR = 1.5;
	private static final double WEIGHT_LOW_FACTOR = 0.5;

	private int defaultNumOfCopyPerServer;
	
	private ConcurrentHashMap<String, SeaLionServerNode> nodes;
	
	/**
	 * Should be thread-safe guarded by itself.
	 */
	private SortedMap<Integer, SeaLionServerNode> hashLoop;
	
	public HashBalancer(ConcurrentHashMap<String, SeaLionServerNode> nodes) {
		if (nodes.size() > 10) {
			defaultNumOfCopyPerServer = 40;
		}
		else if (nodes.size() > 0) {
			defaultNumOfCopyPerServer = 400;
		}
		else {
			throw new IllegalArgumentException("There must be at least 1 server node.");
		}
		this.nodes = nodes;
		hashLoop = Collections.synchronizedSortedMap(new TreeMap<Integer, SeaLionServerNode>());
		hashServerNodes();
	}
	
	/**
	 * Hash all server nodes to the hash loop.
	 * 
	 * @author sunhe
	 * @date 2015年3月19日 上午8:57:21
	 */
	private synchronized void hashServerNodes() {
		hashLoop.clear();
		Set<String> keySet = nodes.keySet();
		for (String key : keySet) {
			hashServerNode(nodes.get(key));
		}
	}
	
	/**
	 * Hash a server node to the hash loop.
	 * 
	 * @param node
	 * @author sunhe
	 * @date 2015年3月19日 上午9:00:51
	 */
	private synchronized void hashServerNode(SeaLionServerNode node) {
		int numOfCopyPerServer = (int) (defaultNumOfCopyPerServer * getFactorValue(node.getWeight()));
		if (numOfCopyPerServer % 4 != 0) {
			throw new IllegalStateException(
					"The value of the variable numOfCopyPerServer is in invariant state: " + numOfCopyPerServer);
		}
		for (int i = 0; i < numOfCopyPerServer / 4; i++) {
			byte[] md5 = DefaultHashAlgorithm.computeMd5(node.getStrSocketAddress() + "-" + i);
			for (int j = 0; j < 4; j++) {
				int key = ((int) (md5[3+j*4] & 0xFF) << 24) 
						| ((int) (md5[2+j*4] & 0xFF) << 16) 
						| ((int) (md5[1+j*4] & 0xFF) << 8) 
						| ((int) (md5[j*4] & 0xFF));
				hashLoop.put(key, node);
			}
		}
	}
	
	/**
	 * Get the node's corresponding weight factor value with the given weight, which 
	 * may be 0, 1 or 2.
	 * 
	 * @param weight 
	 * 			The weight of the server node, may be 0, 1 or 2.
	 * @return The factor value corresponding to the weight.
	 * @author sunhe
	 * @date 2015年3月19日 下午1:31:08
	 */
	private double getFactorValue(int weight) {
		if (weight == SeaLionServerNode.WEIGHT_NORMAL) {
			return WEIGHT_NORMAL_FACTOR;
		}
		else if (weight == SeaLionServerNode.WEIGHT_LOW) {
			return WEIGHT_LOW_FACTOR;
		}
		else if (weight == SeaLionServerNode.WEIGHT_HIGH) {
			return WEIGHT_HIGH_FACTOR;
		}
		else {
			throw new IllegalArgumentException("Invalid weight: " + weight);
		}
	}
	
	/**
	 * Get the a list of all server nodes, and user can't change the node
	 * list through the returned list.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年3月19日 下午1:35:36
	 */
	public Map<String,SeaLionServerNode> getServerNodes() {
		return Collections.unmodifiableMap(nodes);
	}
	
	/**
	 * Add a server node dynamically.
	 * 
	 * @param node
	 * @return True if operation is successful, otherwise false.
	 * @author sunhe
	 * @date 2015年3月19日 下午2:16:18
	 */
	public synchronized boolean addServerNode(SeaLionServerNode node) {
		if (! nodes.containsKey(node.getStrSocketAddress())) {
			nodes.put(node.getStrSocketAddress(), node);
			hashServerNode(node);
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Remove a server node dynamically.
	 * 
	 * @param node
	 * @return
	 * @author sunhe
	 * @date 2015年3月19日 下午2:17:18
	 */
	public synchronized boolean removeServerNode(SeaLionServerNode node) {
		if (nodes.containsKey(node.getStrSocketAddress())) {
			nodes.remove(node.getStrSocketAddress());
			int numOfCopyPerServer = (int) (defaultNumOfCopyPerServer * getFactorValue(node.getWeight()));
			if (numOfCopyPerServer % 4 != 0) {
				throw new IllegalStateException(
						"The value of the variable numOfCopyPerServer is in invariant state: " + numOfCopyPerServer);
			}
			for (int i = 0; i < numOfCopyPerServer / 4; i++) {
				byte[] md5 = DefaultHashAlgorithm.computeMd5(node + "-" + i);
				for (int j = 0; j < 4; j++) {
					int key = ((int) (md5[3+j*4] & 0xFF) << 24) 
							| ((int) (md5[2+j*4] & 0xFF) << 16) 
							| ((int) (md5[1+j*4] & 0xFF) << 8) 
							| ((int) (md5[j*4] & 0xFF));
					hashLoop.remove(key);
				}
			}
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Get the corresponding server node according to the given key.
	 * 
	 * @param key The data's key assigned by user.
	 * @return the corresponding remote server node.
	 * @author sunhe
	 * @date 2015年3月19日 下午2:22:41
	 */
	public synchronized SeaLionServerNode getServerNodeByKey(String key) {
		byte[] md5 = DefaultHashAlgorithm.computeMd5(key);
		int numKey = ((int) (md5[3] & 0xFF) << 24) 
				| ((int) (md5[2] & 0xFF) << 16) 
				| ((int) (md5[1] & 0xFF) << 8) 
				| ((int) (md5[0] & 0xFF));
		if (! hashLoop.containsKey(numKey)) {
			SortedMap<Integer, SeaLionServerNode> tailMap = hashLoop.tailMap(numKey);
			if (tailMap.isEmpty()) {
				numKey = hashLoop.firstKey();
			}
			else {
				numKey = tailMap.firstKey();
			}
		}
		return hashLoop.get(numKey);
	}
	
}
