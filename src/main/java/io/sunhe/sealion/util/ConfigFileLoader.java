package io.sunhe.sealion.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration file loader.
 * 
 * @author sunhe
 * @date 2015年3月19日 下午4:12:21
 */
public class ConfigFileLoader {

	/**
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException
	 * @author sunhe
	 * @date 2015年3月19日 下午4:16:46
	 */
	public static Properties getConfigFileProperties(String fileName) throws IOException {
		BufferedReader reader = new BufferedReader(
				new FileReader(fileName));
		Properties properties = new Properties();
		properties.load(reader);
		return properties;
	}
	
}
