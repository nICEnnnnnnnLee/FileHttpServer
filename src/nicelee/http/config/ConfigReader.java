package nicelee.http.config;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
//import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigReader {
	final static Pattern patternConfig = Pattern.compile("^[ ]*([0-9|a-z|A-Z|.|_]+)[ ]*=[ ]*([^ ]+.*$)");
	
	
	/**
	 *  返回key 所对应的value; 如无设置,返回默认值
	 * @param property
	 * @return
	 */
	public static int getInt(String property) {
		try {
			return Integer.parseInt(System.getProperty(property));
		}catch (Exception e) {
			if("nicelee.server.port".equals(property)) {
				return 6667;
			}else if("nicelee.server.fixedPoolSize".equals(property)) {
				return 30;
			}else{
				return 0;
			}
		}
	}
	
	/**
	 *  返回key 所对应的value; 如无默认设置,返回null
	 * @param property
	 * @return
	 */
	public static String getString(String property) {
		String value = System.getProperty(property);
		if( value == null) {
			if("nicelee.server.port".equals(property)) {
				return "6667";
			}else if("nicelee.server.fixedPoolSize".equals(property)) {
				return "30";
			}else if("nicelee.server.source".equals(property)) {
				return ".";
			}			
		}
		return value;
	}
	
	public static void initConfigs() {
		BufferedReader buReader = null;
		System.out.println("----Config init begin...----");
		try {
			buReader = new BufferedReader(new FileReader("app.config"));
			String config;
			while ((config = buReader.readLine()) != null) {
				Matcher matcher = patternConfig.matcher(config);
				if (matcher.find()) {
					System.setProperty(matcher.group(1), matcher.group(2).trim());
					System.out.printf("  key-->value:  %s --> %s\r\n",matcher.group(1),matcher.group(2));
				}
			}
		} catch (IOException e) {
			//e.printStackTrace();
		} finally {
			try {
				buReader.close();
			} catch (Exception e) {
			}
		}
		System.out.println("----Config ini end...----");
	}
/* 配置直接放入System.properties
	public static HashMap<String, String> configs = new HashMap<>();
	public static HashMap<String, String> initConfigs() {
		BufferedReader buReader = null;
		try {
			buReader = new BufferedReader(new FileReader("config.application"));
			String config;
			while ((config = buReader.readLine()) != null) {
				Matcher matcher = patternConfig.matcher(config);
				if (matcher.find()) {
					configs.put(matcher.group(1), matcher.group(2).trim());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				buReader.close();
			} catch (Exception e) {
			}
		}
		return configs;
	}
*/
}
