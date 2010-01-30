package hudson.cli;

import hudson.Extension;
import hudson.remoting.Callable;
import hudson.remoting.Channel;

import java.io.IOException;
import java.io.Serializable;

@Extension
public class LogoutCommand extends CLICommand implements Serializable {

	private static final long serialVersionUID = 1L;

	@Override
	public String getShortDescription() {
		return "Log out of to Hudson";
	}

	@Override
	protected int run() throws Exception {
		return channel.call(new Callable<Integer, IOException>() {
			public Integer call() throws IOException {
				String url = (String) Channel.current().getProperty("url");
				CookieJar cookieJar = (CookieJar) Channel.current().getProperty(CookieJar.class.getName());
				cookieJar.removeCookie(url);
				return 0;
			}
		});
	}

}
