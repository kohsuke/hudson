package hudson.cli;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.remoting.Callable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;

@Extension
public class LoginCommand extends CLICommand implements Serializable {

	private static final long serialVersionUID = 1L;

	@Override
	public String getShortDescription() {
		return "Logs in to Hudson for future CLI commands";
	}

	@Override
	protected int run() throws Exception {
		long tokenExpiryTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
		final String cookie = encodeCookie(
				Hudson.getAuthentication().getName(), tokenExpiryTime);

		channel.call(new Callable<Void, IOException>() {
			public Void call() throws IOException {
				File cookieFile = new File(System.getProperty("user.home"),
						".hudson/cookie.txt");
				cookieFile.getParentFile().mkdirs();
				Properties p = new Properties();
				p.put("cookie", cookie);
				FileWriter writer = new FileWriter(cookieFile);
				try {
					p.store(writer, "");
				} finally {
					writer.close();
				}
				return null;
			}
		});
		return 0;
	}

	public static String encodeCookie(String userName, long tokenExpiryTime) {
		String signature = DigestUtils.md5Hex(userName + ":" + tokenExpiryTime
				+ ":" + "N/A" + ":" + Hudson.getInstance().getSecretKey());
		String cookie = userName + ":" + tokenExpiryTime + ":" + signature;
		return cookie;
	}

}
