package hudson.cli;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;


/**
 * A cookie jar contains cookies.
 * 
 * @author tom
 */
public class CookieJar {

	private Properties properties = new Properties();

	private final File cookieFile;
	
	public CookieJar(File cookieFile) throws IOException {
		this.cookieFile = cookieFile;
		if (cookieFile.exists()) {
			Reader r = new FileReader(cookieFile);
			try {
				properties.load(r);
			} finally {
				r.close();
			}
		} else {
			cookieFile.getParentFile().mkdirs();
		}
	}
	
	public void addCookie(String url, String cookie) throws IOException {
		properties.setProperty(url, cookie);
		
		save();
	}

	private void save() throws IOException {
		// write properties
		FileWriter writer = new FileWriter(cookieFile);
		try {
			properties.store(writer, "Hudson Cookie Jar");
		} finally {
			writer.close();
		}
	}

	public String getCookie(String url) {
		return properties.getProperty(url);
	}

	public void removeCookie(String url) throws IOException {
		properties.remove(url);
		save();
	}



}
