package io.sunhe.sealion.util;

import java.util.Random;

/**
 * Thread-safe
 * 
 * @author sunhe
 * @date 2015年3月17日 下午4:23:30
 */
public class OpaqueGenerator {

	private static final String STR = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	
	private static final Random RAND = new Random();
	
	public static String generate() {
		return generate(4);
	}
	
	public static String generate(int len) {
		StringBuffer sf = new StringBuffer(len);
		for (int i = 0; i < len; i++) {
			sf.append(STR.charAt(RAND.nextInt(62)));
		}
		return sf.toString();
	}
	
}
