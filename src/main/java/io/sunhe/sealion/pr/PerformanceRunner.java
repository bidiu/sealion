package io.sunhe.sealion.pr;

import io.sunhe.sealion.client.SeaLionClient;
import io.sunhe.sealion.util.ConfigFileLoader;
import io.sunhe.sealion.util.OpaqueGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author sunhe
 * @date 2015年5月28日 上午9:51:27
 */
public class PerformanceRunner {
	
	public static final String DEFAULT_SERVER_ADDR = "127.0.0.1:1113";
	
	private String serverAddr;
	
	/**
	 * console parameters
	 */
	private PerformanceRunnerParams params;
	
	/**
	 * sealion client list
	 */
	private List<SeaLionClient> clientList = new ArrayList<SeaLionClient>();
	
	private byte[] buffer;
	
	private CountDownLatch startLatch;
//	private CountDownLatch endLatch;
	
	
	public PerformanceRunner(PerformanceRunnerParams params) throws IOException {
		this.params = params;
		if (params.getFile() == null) {
			serverAddr = DEFAULT_SERVER_ADDR;
		}
		else {
			Properties properties = ConfigFileLoader.getConfigFileProperties(params.getFile());
			serverAddr = properties.getProperty("servers", DEFAULT_SERVER_ADDR);
		}
		for (int i = 0; i < params.getConn(); i++) {
			clientList.add(new SeaLionClient(serverAddr));
		}
		buffer = new byte[params.getSize()];
		startLatch = new CountDownLatch(1);
//		endLatch = new CountDownLatch(params.getConn() * params.getThread());
	}
	
	private void run() {
		for (int i = 0; i < clientList.size(); i++) {
			runPerConn(clientList.get(i));
		}
		startLatch.countDown();
//		try {
//			endLatch.await();
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		for (int i = 0; i < clientList.size(); i++) {
//			clientList.get(i).close();
//		}
	}
	
	/**
	 * For a single client (or say connection).
	 * 
	 * @param index
	 * @author sunhe
	 * @date 2015年5月28日 下午4:37:40
	 */
	private void runPerConn(final SeaLionClient client) {
		for (int i = 0; i < params.getThread(); i++) {
			new Thread(new Runnable() {
				
				public void run() {
					LinkedList<String> keyList = new LinkedList<String>();
					try {
						startLatch.await();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					// core logic for a single thread of a connection
					int targetHit = (int) (params.getReadRatio() * params.getHitRatio() * 0.01);
					while (true) {
						// read
						int hitCnt = 0;
						for (int i = 0; i < params.getReadRatio(); i++) {
							String key = null;
							// hit
							if (hitCnt <= targetHit) {
								hitCnt++;
								key = randKey(keyList);
								if (key == null) {
									key = OpaqueGenerator.generate();
									if (shouldAppendKey()) {
										appendKey(keyList, key);										
									}
								}
								if (client.getBytes(key) == null) {
									if ("safe".equals(params.getMode())) {
										client.setBytes(key, buffer, params.getExpiry());
									}
									else {
										client.setBytesUnsafe(key, buffer, params.getExpiry());
									}
								}
							}
							else {
								// miss
								key = OpaqueGenerator.generate();
								if (shouldAppendKey()) {
									appendKey(keyList, key);
								}
								if (client.getBytes(key) == null) {
									if ("safe".equals(params.getMode())) {
										client.setBytes(key, buffer, params.getExpiry());
									}
									else {
										client.setBytesUnsafe(key, buffer, params.getExpiry());
									}
								}
							}
						}
						// write
						for (int i = 0; i < 100 - params.getReadRatio(); i++) {
							String key = OpaqueGenerator.generate();
							if (shouldAppendKey()) {
								appendKey(keyList, key);
							}
							if ("safe".equals(params.getMode())) {
								client.setBytes(key, buffer, params.getExpiry());
							}
							else {
								client.setBytesUnsafe(key, buffer, params.getExpiry());
							}
						}
					}
//					endLatch.countDown();
				}
				
			}).start();
		}
	}
	
	/**
	 * Get a key from given key list at random.
	 * 
	 * @param keyList
	 * @return random key, null if given key list is empty. 
	 * @author sunhe
	 * @date 2015年5月28日 下午8:08:17
	 */
	private String randKey(LinkedList<String> keyList) {
		if (keyList.isEmpty()) {
			return null;
		}
		else {
			int index = ThreadLocalRandom.current().nextInt(keyList.size());
			return keyList.get(index);
		}
	}
	
	/**
	 * Append a key to key list, may truncate the head portion of the key list.
	 * 
	 * @author sunhe
	 * @date 2015年5月28日 下午8:19:09
	 */
	private void appendKey(LinkedList<String> keyList, String key) {
		if (keyList.size() >= 100) {
			keyList.removeFirst();
		}
		keyList.addLast(key);
	}
	
	/**
	 * Have 0.5% possibility to append a key to key list.
	 * 
	 * @return
	 * @author sunhe
	 * @date 2015年5月28日 下午8:56:07
	 */
	private boolean shouldAppendKey() {
		if (21 == ThreadLocalRandom.current().nextInt(200)) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public static void printHelp() {
		System.out.println("Paramter Directions:");
		System.out.println("\t--hitRatio     Approximate hit ratio. (0 ~ 100)");
		System.out.println("\t--readRatio    Read ratio against write. (0 ~ 100)");
		System.out.println("\t--mode         Operation mode. (safe or unsafe)");
		System.out.println("\t--size         Single data size in byte unit. (postive integer)");
		System.out.println("\t--conn         Number of connection. (postive integer)");
		System.out.println("\t--thread       Numer of thread per connection. (postive integer)");
		System.out.println("\t-h --help      Print help info. (no value)");
		System.out.println("\t-f --file      Configuration file. (file path)");
		System.out.println("\t--expiry       Expiry in minute unit. (0 or postive integer)");
	}
	
	public static void main(String[] args) {
		PerformanceRunnerParams params = new PerformanceRunnerParams(args);
		if (params.getHelp() != null) {
			printHelp();
			return;
		}
		PerformanceRunner pr = null;
		try {
			pr = new PerformanceRunner(params);
		} catch (IOException e) {
			System.err.println("Can't locate configuration file.");
			return;
		}
		
		pr.run();
		
				
	}
	
}
