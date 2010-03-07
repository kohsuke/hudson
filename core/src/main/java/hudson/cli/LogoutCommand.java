package hudson.cli;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.remoting.Callable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.acegisecurity.Authentication;

@Extension
public class LogoutCommand extends CLICommand {

	@Override
	public String getShortDescription() {
		return "Logout of Hudson";
	}

	@Override
	protected int run() throws Exception {
		Hudson h = Hudson.getInstance();
		
		Authentication auth = Hudson.getAuthentication();
		if (!auth.isAuthenticated()) {
			stderr.println("Must provide credentials");
			return -1;
		}
		
		final String hudsonURL = h.getRootUrl();
		
		channel.call(new RemoveCredentials(hudsonURL));
		
		return 0;
	}
	
	public static class RemoveCredentials implements Callable<Void,IOException> {

		private static final long serialVersionUID = 1L;
		
		private final String url;

		public RemoveCredentials(String url) {
			this.url = url;
		}
		
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

				properties.remove(url);
				
				FileWriter writer = new FileWriter(cookieFile);
				try {
					properties.store(writer, "Hudson Cookie Jar");
				} finally {
					writer.close();
				}
			}
			
			return null;
		}
		
	}
	
}
