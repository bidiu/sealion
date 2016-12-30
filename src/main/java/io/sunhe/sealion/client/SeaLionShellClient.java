package io.sunhe.sealion.client;

import io.sunhe.sealion.protocol.Commands;
import io.sunhe.sealion.util.ConfigFileLoader;
import io.sunhe.sealion.util.TimeStampFormatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * SeaLion server's shell client. 
 * Just for presentation, so don't concern too much possible situation. 
 * 
 * The format of the configuration file (note that invalid file format 
 * could cause this program crush):
 * 		All server address strings are delimited by white space. 
 * 
 * 		The server address string format should be (host|ip)[:port][:weight]. 
 * 		The weight is the server's weight, user can configured it according the 
 * 		server's capability or something else.
 * 
 * 		Note: 
 * 		In order to assign the weight, you must assign the port in the same time.
 * 
 * 		For more particular example just see the sample file in the root directory of 
 * 		release version folder: sealion_cli.sh
 * 
 * @author sunhe
 * @date 2015年5月23日 下午8:10:04
 */
public class SeaLionShellClient {
	
	public static final String DEFAULT_SERVER_ADDR = "127.0.0.1:1113";
	
	private SeaLionClient seaLionClient;
	
	private BufferedReader in;
	
	private Pattern timeStampPattern = Pattern.compile("\\d+\\D+");
	
	public SeaLionShellClient(SeaLionClient seaLionClient) {
		this.seaLionClient = seaLionClient;
		in = new BufferedReader(
				new InputStreamReader(System.in));
	}
	
	/**
	 * Start shell client. 
	 * 
	 * @author sunhe
	 * @throws IOException 
	 * @date 2015年5月23日 下午9:13:14
	 */
	public void start() throws IOException {
		while (true) {
			printPrompt();
			String line = in.readLine();
			process(line);
		}
	}
	
	/**
	 * Processing the input line.
	 * 
	 * @param line
	 * @author sunhe
	 * @date 2015年5月23日 下午9:29:49
	 */
	private void process(String line) {
		String key = null, value = null, cas = null;
		long timeStamp = 0L;
		boolean isSuccessful;
		String[] words = line.split("\\s+");
		if (words.length == 0) {
			return;
		}
		words[0] = words[0].toUpperCase();
		if (Commands.SET.equals(words[0])) {
			// SET
			if (words.length != 3 && words.length != 5 && words.length != 7) {
				printErrorInfo();
				return;
			}
			key = words[1];
			value = words[2];
			if (words.length >= 5) {
				// EXPIRED ..s
				if ("EXPIRED".equals(words[3].toUpperCase())) {
					if (validateTimeStamp(words[4])) {
						timeStamp = getTimeStamp(words[4]);
					}
					else {
						printErrorInfo();
						return;
					}
				}
				else {
					printErrorInfo();
					return;
				}
			}
			if (words.length == 7) {
				// CAS ....
				if ("CAS".equals(words[5].toUpperCase())) {
					cas = words[6];
				}
				else {
					printErrorInfo();
					return;
				}
			}
			if (cas == null) {
				isSuccessful = seaLionClient.setString(key, value, timeStamp);
			}
			else {
				isSuccessful = seaLionClient.setString(key, value, timeStamp, cas);
			}
			if (isSuccessful) {
				println("SUCCESS");
			}
			else {
				println("FAILED");
			}
		}
		else if (Commands.GET.equals(words[0])) {
			// GET
			if (words.length != 2 && words.length != 4) {
				printErrorInfo();
				return;
			}
			key = words[1];
			if (words.length == 4) {
				// CAS ....
				if ("CAS".equals(words[2].toUpperCase())) {
					cas = words[3];
				}
				else {
					printErrorInfo();
					return;
				}
			}
			if (cas == null) {
				value = seaLionClient.getString(key);
			}
			else {
				value = seaLionClient.getString(key, cas);
			}
			println(value);
		}
		else if (Commands.DELETE.equals(words[0])) {
			// DELETE
			if (words.length != 2 && words.length != 4) {
				printErrorInfo();
				return;
			}
			key = words[1];
			if (words.length == 4) {
				// CAS ....
				if ("CAS".equals(words[2].toUpperCase())) {
					cas = words[3];
				}
				else {
					printErrorInfo();
				}
			}
			if (cas == null) {
				isSuccessful = seaLionClient.deleteValue(key);
			}
			else {
				isSuccessful = seaLionClient.deleteValue(key, cas);
			}
			if (isSuccessful) {
				println("SUCCESS");
			}
			else {
				println("FAILED");
			}
		}
		else {
			printErrorInfo();
		}
	}
	
	/**
	 * Validate the format of time stamp.
	 * The format should like that :
	 * 		1200s
	 * 		20m
	 * 
	 * @param word
	 * @return
	 * @author sunhe
	 * @date 2015年5月23日 下午9:53:06
	 */
	private boolean validateTimeStamp(String word) {
		if (! timeStampPattern.matcher(word).matches()) {
			return false;
		}
		word = word.replaceAll("\\d+", "");
		if ("s".equals(word) || "m".equals(word)) {
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Get the time stamp.
	 * 
	 * @param word Should be a valid time stamp string.
	 * @return
	 * @author sunhe
	 * @date 2015年5月23日 下午10:07:08
	 */
	private long getTimeStamp(String word) {
		int rawTimeStamp = getTimeStampValue(word);
		String unit = getTimeStampUnit(word);
		if ("s".equals(unit)) {
			return TimeStampFormatter.fromSecondToTimeStamp(rawTimeStamp);
		}
		else {
			return TimeStampFormatter.fromMinuteToTimeStamp(rawTimeStamp);
		}
	}
	
	/**
	 * Get the time stamp value either in minute unit or second.
	 * 
	 * @param word Should be a valid time stamp string.
	 * @return
	 * @author sunhe
	 * @date 2015年5月23日 下午9:56:26
	 */
	private int getTimeStampValue(String word) {
		word = word.replaceAll("\\D+", "");
		return Integer.parseInt(word);
	}
	
	/**
	 * @param word Should be a valid time stamp string.
	 * @return s or m
	 * @author sunhe
	 * @date 2015年5月23日 下午10:02:58
	 */
	private String getTimeStampUnit(String word) {
		return word.replaceAll("\\d+", "");
	}
	
	/**
	 * Prompt error information on screen.
	 * 
	 * @author sunhe
	 * @date 2015年5月23日 下午9:38:48
	 */
	private void printErrorInfo() {
		println("Invalid input.");
	}
	
	private void printPrompt() {
		System.out.print("<< ");
	}
	
	private void println(String str) {
		System.out.println(">> " + str);
	}

	public static void main(String[] args) throws IOException {
		SeaLionShellClientParams params = new SeaLionShellClientParams(args);
		String serverAddr = null;
		if (params.getF() == null) {
			serverAddr = DEFAULT_SERVER_ADDR;
		}
		else {
			Properties properties = null;
			try {
				properties = ConfigFileLoader.getConfigFileProperties(params.getF());
			} catch (IOException e) {
				System.err.println("Can not locate configuration file.");
				return;
			}
			serverAddr = properties.getProperty("servers", DEFAULT_SERVER_ADDR);
		}
		SeaLionClient seaLionClient = new SeaLionClient(serverAddr);
		new SeaLionShellClient(seaLionClient).start();
	}
	
}

