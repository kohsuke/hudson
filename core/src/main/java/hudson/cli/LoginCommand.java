package hudson.cli;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.remoting.Callable;
import hudson.remoting.Channel;

import java.io.IOException;
import java.io.Serializable;

@Extension
public class LoginCommand extends CLICommand implements Serializable {

	private static final long serialVersionUID = 1L;

	@Override
	public String getShortDescription() {
		return "Logs in to Hudson for future CLI commands";
	}

	@Override
	protected int run() throws Exception {
		if (Hudson.getAuthentication() == Hudson.ANONYMOUS) {
			stderr.println("Please provide credentials");
			stderr.println();
			stderr.println("java -jar cli.jar -u <user> -p <password> login");
			return -1;
		}
		
		// TODO parameterize this
		long tokenExpiryTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000;
		
		final String cookie = new Cookie(Hudson.getAuthentication().getName(),
				tokenExpiryTime).encode(Hudson.getInstance().getSecretKey());

		channel.call(new Callable<Void, IOException>() {
			public Void call() throws IOException {
				String url = (String) Channel.current().getProperty("url");
				CookieJar cookieJar = (CookieJar) Channel.current().getProperty(CookieJar.class.getName());
				cookieJar.addCookie(url, cookie);
				
				return null;
			}
		});
		return 0;
	}

}
