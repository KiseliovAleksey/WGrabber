package com.app.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.app.client.TimeInterval;
import com.wt.shared.util.FileUtil;

public class Settings {

	private static final Log log = LogFactory.getLog(Settings.class);
	
	private static final String CONFIG_FILE = "app.properties";
	private static final String CONFIG = "conf/" + CONFIG_FILE;
	private static String PROD_CONFIG = "../" + CONFIG;
	
	public static final String SERVER = "server";
	public static final String PORT = "port";
	public static final String LOGIN = "login";
	public static final String PASSWORD = "password";
	public static final String WORKDIR = "workdir";
	public static final String MEMO = "memo";
	public static final String MEMO_TEXT = "memo_text";
	public static final String HIDEIMAGE = "hideimage";
	public static final String REMINDER = "reminder";
	public static final String LOCALE = "locale";
	public static final String SHOW_SERVER_FAILS = "show_server_fails";
	public static final String SHOW_SCREENSHOT_FAILS = "show_screenshot_fails";
	public static final String DEBUG = "debug";
	
	private String path = null;
	private Properties pr = null;
	
	public Settings() {
		
		String dir = FileUtil.mergePath(
				FileUtil.getUserPreferencesDir(),
				"conf");
		dir.replace("\\", "/");
		FileUtil.ensureDirectoryExists(dir);
		if (new File(dir).exists()) {
			PROD_CONFIG = dir + CONFIG_FILE;
		}
		
		this.path = CONFIG;
		if (!new File(CONFIG).exists()) {
			this.path = PROD_CONFIG;
			log.info("Config setted to prod");
		}
		else {
			log.info("Config finded at dev");
		}
		pr = new Properties();
		try {
			reload();
		}
		catch (Throwable t) {
			dir = FileUtil.mergePath(
					FileUtil.getUserPreferencesDir(),
					"data");
			dir.replace("\\", "/");
			pr.setProperty(SERVER, "http://w-timer.com");
			pr.setProperty(PORT, "8884");
			pr.setProperty(LOGIN, "");
			pr.setProperty(PASSWORD, "");
			pr.setProperty(WORKDIR, dir);
			pr.setProperty(MEMO, TimeInterval.I30MIN.getId() + "");
			pr.setProperty(HIDEIMAGE, TimeInterval.I16SEC.getId() + "");
			pr.setProperty(REMINDER, TimeInterval.I10MIN.getId() + "");
			pr.setProperty(LOCALE, "en");
			pr.setProperty(MEMO_TEXT, "");
			pr.setProperty(SHOW_SERVER_FAILS, "true");
			pr.setProperty(SHOW_SCREENSHOT_FAILS, "true");
			try {
				update();
			} catch (IOException e) {
				log.error(e, e);
			}
		}
	}
	
	public void reload() throws IOException {
		InputStream inputStream = new FileInputStream(path);
		pr.load(new InputStreamReader(inputStream, Charset.defaultCharset()));
	}
	
	public void update() throws IOException {
		String params = "";
		for (Object key: pr.keySet()) {
			params += key.toString();
			params += "=";
			params += unicodeFormat(pr.getProperty(key.toString()));
			params += "\n";
		}
		if (!new File(path).exists()) {
			if (!new File(path).getParentFile().exists()) {
				new File(path).getParentFile().mkdirs();
			}
		}
		try (FileOutputStream fs = new FileOutputStream(new File(path))) {
			fs.write(params.getBytes());
		}
		catch(IOException ex) {
			log.error("Failed to save settings!", ex);
		}
	}
	
	public String getServer() {
		return pr.getProperty(SERVER);
	}
	
	public void setServer(String value) {
		pr.setProperty(SERVER, value);
	}
	
	public String getDebugKey() {
		return pr.getProperty(DEBUG);
	}
	
	public void setDebugKey(String value) {
		pr.setProperty(DEBUG, value + "");
	}
	
	public Boolean isShowScreenshotFails() {
		try {
			String prop = pr.getProperty(SHOW_SCREENSHOT_FAILS);
			return prop != null ?
				Boolean.valueOf(prop) : true;
		}
		catch(Throwable t) {
			return true;
		}
	}
	
	public void setShowScreenshotFails(Boolean value) {
		pr.setProperty(SHOW_SCREENSHOT_FAILS, value + "");
	}
	
	public Boolean isShowServerFails() {
		try {
			String prop = pr.getProperty(SHOW_SERVER_FAILS);
			return prop != null ?
				Boolean.valueOf(prop) : true;
		}
		catch(Throwable t) {
			return true;
		}
	}
	
	public void setShowServerFails(Boolean value) {
		pr.setProperty(SHOW_SERVER_FAILS, value + "");
	}
	
	public Integer getPort() {
		try {
			return Integer.parseInt(pr.getProperty(PORT));
		}
		catch(Throwable t) {
			return 8881;
		}
	}
	
	public void setPort(Integer value) {
		pr.setProperty(PORT, value + "");
	}
	
	public String getLogin() {
		return pr.getProperty(LOGIN);
	}
	
	public void setLogin(String value) {
		pr.setProperty(LOGIN, value);
	}
	
	public String getPassword() {
		return pr.getProperty(PASSWORD);
	}
	
	public void setPassword(String value) {
		pr.setProperty(PASSWORD, value);
	}
	
	public String getWorkdir() {
		String dir = pr.getProperty(WORKDIR);
		dir = dir.replace("\\", "/");
		return dir;
	}
	
	public void setWorkdir(String value) {
		pr.setProperty(WORKDIR, value);
	}
	
	public TimeInterval getMemo() {
		try {
			TimeInterval res = TimeInterval.lookup(
					Integer.parseInt(pr.getProperty(MEMO)));
			return res == null ? TimeInterval.I30MIN : res;
		}
		catch(Throwable t) {
			return TimeInterval.I30MIN;
		}
	}
	
	public void setMemo(TimeInterval value) {
		pr.setProperty(MEMO, value.getId() + "");
		try {
			update();
		} catch (IOException e) {
			log.error(e, e);
		}
	}
	
	public TimeInterval getHideImage() {
		try {
			TimeInterval res = TimeInterval.lookup(
					Integer.parseInt(pr.getProperty(HIDEIMAGE)));
			return res == null ? TimeInterval.I10SEC : res;
		}
		catch(Throwable t) {
			return TimeInterval.I10SEC;
		}
	}
	
	public void setHideImage(TimeInterval value) {
		pr.setProperty(HIDEIMAGE, value.getId() + "");
	}
	
	public TimeInterval getReminder() {
		try {
			TimeInterval res = TimeInterval.lookup(
					Integer.parseInt(pr.getProperty(REMINDER)));
			return res == null ? TimeInterval.I10MIN : res;
		}
		catch(Throwable t) {
			return TimeInterval.I10MIN;
		}
	}
	
	public void setReminder(TimeInterval value) {
		pr.setProperty(REMINDER, value.getId() + "");
		try {
			update();
		} catch (IOException e) {
			log.error(e, e);
		}
	}
	
	public String getPropertiesPath() {
		return path;
	}

	public String getLocale() {
		return pr.getProperty(LOCALE);
	}
	
	public void setLocale(String locale) {
		pr.setProperty(LOCALE, locale);
	}
	
	public String getMemoText() {
		try {
			return pr.getProperty(MEMO_TEXT);
		}
		catch(Throwable t) {
			return null;
		}
	}
	
	public void setMemoText(String memo) {
		pr.setProperty(MEMO_TEXT, memo);
		try {
			update();
		} catch (IOException e) {
			log.error(e, e);
		}
	}
	
	public String unicodeFormat(String str) {
		StringBuilder builder = new StringBuilder();
		final char[] hexdigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
				'9', 'A', 'B', 'C', 'D', 'E', 'F' };
		char cur;
		char[] hexcode = new char[4];
		for (int pos = 0; pos < str.length(); ++pos) {
			cur = str.charAt(pos);
			switch (cur) {
			case '\t':
				builder.append("\\t");
				break;
			case '\b':
				builder.append("\\b");
				break;
			case '\f':
				builder.append("\\f");
				break;
			case '\r':
				builder.append("\\r");
				break;
			case '\n':
				builder.append("\\n");
				break;
			case ' ':
				if (pos == 0) {
					builder.append("\\ ");
				} else {
					builder.append(" ");
				}
				break;
			case '=':
			case ':':
			case '!':
				builder.append("\\");
				builder.append(cur);
				break;
			default:
				if (cur < 0x20 || cur > 0x7E) {
					hexcode[3] = hexdigits[(cur >> 0) & 0x000F];
					hexcode[2] = hexdigits[(cur >> 4) & 0x000F];
					hexcode[1] = hexdigits[(cur >> 8) & 0x000F];
					hexcode[0] = hexdigits[(cur >> 12) & 0x000F];
					builder.append("\\u");
					builder.append(hexcode);
				} else {
					builder.append(cur);
				}
			}
		}
		return builder.toString();
	}
}
