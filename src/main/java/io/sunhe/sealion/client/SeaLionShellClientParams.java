package io.sunhe.sealion.client;

/**
 * Represents the console parameters of shell client.
 * 
 * Note that after instantiated the field of this class may be null, 
 * signifying that the parameter's value is not given. 
 * This class will check the basic validation of the console parameters.
 * 
 * Thread-safe.
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
 * @date 2015年5月23日 下午8:18:36
 */
public class SeaLionShellClientParams {

	/**
	 * -f file
	 */
	private String f;
	
	/**
	 * @param args The console arguments.
	 * @author sunhe
	 * @date 2015年5月23日 下午8:23:59
	 */
	public SeaLionShellClientParams(String[] args) {
		if (args.length % 2 != 0) {
			throwException();
		}
		for (int i = 0; i < args.length; i += 2) {
			String paramName = args[i];
			String paramValue = args[i+1];
			if ("-f".equals(paramName)) {
				f = paramValue;
			}
			else {
				throwException();
			}
		}
	}
	
	/**
	 * Throw the IllegalArgumentException exception.
	 * 
	 * @author sunhe
	 * @date 2015年5月3日 下午10:34:41
	 */
	private void throwException() {
		throw new IllegalArgumentException("The given console paramters are invalid");
	}

	public String getF() {
		return f;
	}
	
}
