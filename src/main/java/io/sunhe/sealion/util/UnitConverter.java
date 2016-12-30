package io.sunhe.sealion.util;

/**
 * Unit converter.
 * 
 * @author sunhe
 * @date 2015年4月15日 上午8:48:46
 */
public class UnitConverter {

	public static int fromByteToMegabyte(long from) {
		if (from < 0) {
			throw new IllegalArgumentException();
		}
		return (int) (from / 1024 / 1024);
	}
	
	public static long fromMegabyteToByte(int from) {
		if (from < 0) {
			throw new IllegalArgumentException();
		}
		return from * 1024 * 1024;
	}
	
	public static int fromFloatToPercentage(double from) {
		if (from < 0) {
			throw new IllegalArgumentException();
		}
		return (int) (from * 100);
	}
	
}
