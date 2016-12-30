package io.sunhe.sealion.util;

import java.text.DateFormat;
import java.util.Date;

/**
 * Should be thread-safe.
 * 
 * @author sunhe
 * @date 2015年3月25日 下午7:46:42
 */
public class TimeStampFormatter {
	
	private static final ThreadLocal<DateFormat> perThreadDateFormat = new ThreadLocal<DateFormat>() {
		
		@Override
		protected DateFormat initialValue() {
			return DateFormat.getDateTimeInstance();
		}
		
	};
	
	public static String fromDateToStrDateTime(Date date) {
		DateFormat dateFormat = perThreadDateFormat.get();
		return dateFormat.format(date);
	}
	
	public static String fromTimeStampToStrDateTime(long timeStamp) {
		Date date = new Date(timeStamp);
		return fromDateToStrDateTime(date);
	}
	
	public static long fromMinuteToTimeStamp(int minute) {
		return new Date(new Date().getTime() + minute * 60 * 1000).getTime();
	}
	
	public static long fromSecondToTimeStamp(int second) {
		return new Date(new Date().getTime() + second * 1000).getTime();
	}
	
	public static long getCurrentTimeStamp() {
		return System.currentTimeMillis();
	}
	
}
