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
				File cookieFile = new File(System.getProperty("user.home"),
						".hudson/cookie.txt");
				if (!cookieFile.exists()) {
					return 0;
				}
				if (cookieFile.delete()) {
					return 0;
				}
				stderr.println("could not delete " + cookieFile);
				
				return -1;
				
			}
		});
	}

}
