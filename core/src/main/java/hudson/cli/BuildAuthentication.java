package hudson.cli;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.Run;

/**
 * This builds/checks a token that is only valid while a build is running
 * 
 * @author huybrechts
 * 
 */
public class BuildAuthentication {

	public static void buildEnvVars(EnvVars envVars, AbstractBuild build) {
		String id = build.getExternalizableId();
		long expiryTime = Long.MAX_VALUE;
		String cookie = new Cookie(id, expiryTime).encode(Hudson.getInstance()
				.getSecretKey());
		envVars.put("HUDSON_CLI_COOKIE", cookie);
	}

	public static boolean isValid(String cookieText) {
		try {
			Cookie cookie = Cookie.decode(Hudson.getInstance()
					.getSecretKey(), cookieText);

			AbstractBuild r = (AbstractBuild) Run
					.fromExternalizableId(cookie.user);
			if (!r.isBuilding()) {
				return false;
			}

			if (Long.MAX_VALUE != cookie.tokenExpiryTime) {
				return false;
			}

			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
