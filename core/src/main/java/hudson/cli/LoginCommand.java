package hudson.cli;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.remoting.Callable;
import hudson.remoting.Channel;
import hudson.security.ACL;
import hudson.util.Secret;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.acegisecurity.Authentication;
import org.kohsuke.args4j.Option;

@Extension
public class LoginCommand extends CLICommand {

	@Option(name = "--username", usage = "User name to authenticate yourself to Hudson")
	public String userName;
	@Option(name = "--password", usage = "Password for authentication. Note that passing a password in arguments is insecure.")
	public String password;
	@Option(name = "--password-file", usage = "File that contains the password")
	public String passwordFile;
	
	@Override
	public String getShortDescription() {
		return "Login to Hudson for future CLI commands";
	}

	@Override
	protected int run() throws Exception {
		System.out.println(userName + " " + password);
		
		Hudson h = Hudson.getInstance();
		
		Authentication auth = h.getAuthentication();
		if (!auth.isAuthenticated()) {
			stderr.println("Must provide credentials");
			return -1;
		}
		
		if (auth == ACL.SYSTEM) {
			stderr.println("Cannot login as system");
			return -1;
		}
		
		final String hudsonURL = h.getRootUrl();
		final String secret = Secret.fromString(Hudson.getAuthentication().getName()).getEncryptedValue();
		
		channel.call(new SaveCredentials(hudsonURL, secret));
		
		return 0;
	}
	
	public static String getCredentials(Channel channel) throws IOException, InterruptedException {
		String secret = channel.call(new GetCredentials(Hudson.getInstance().getRootUrl()));
		if (secret == null) {
			return null;
		} else {
			String user = Secret.decrypt(secret).toString();
			return user;
		}
	}
	
	public static class GetCredentials implements Callable<String,IOException> {

		private final String url;

		public GetCredentials(String url) {
			this.url = url;
		}
		
		@Override
		public String call() throws IOException {
			File cookieFile = new File(System.getProperty("user.home"), ".hudson/cookies.txt");
			Properties properties = new Properties();
			if (cookieFile.exists()) {
				Reader r = new FileReader(cookieFile);
				try {
					properties.load(r);
					return properties.getProperty(url);
				} finally {
					r.close();
				}
			}
			
			return null;
		}
		
	}

	private static class SaveCredentials implements Callable<Void,IOException> {
		
		public SaveCredentials(String url, String cookie) {
			super();
			this.url = url;
			this.cookie = cookie;
		}

		private final String url;
		private final String cookie;

		@Override
		public Void call() throws IOException {
			File cookieFile = new File(System.getProperty("user.home"), ".hudson/cookies.txt");
			Properties properties = new Properties();
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
			
			properties.setProperty(url, cookie);

			// write properties
			FileWriter writer = new FileWriter(cookieFile);
			try {
				properties.store(writer, "Hudson Cookie Jar");
			} finally {
				writer.close();
			}
			
			return null;
		}
		
	}
	
}
