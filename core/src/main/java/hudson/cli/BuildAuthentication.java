package hudson.cli;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Run;

public class BuildAuthentication {

	public static void buildEnvVars(EnvVars envVars, AbstractBuild build) {
		String id = build.getExternalizableId();
		long expiryTime = Long.MAX_VALUE;
		String cookie = LoginCommand.encodeCookie(id, expiryTime);
		envVars.put("HUDSON_CLI_COOKIE", cookie);
	}
	
	public static boolean isValid(String[] cookieTokens) {
		
		String id = cookieTokens[0];
		try {
			AbstractBuild r = (AbstractBuild) Run.fromExternalizableId(id);
			if (!r.isBuilding()) {
				return false;
			}
		} catch (Exception  e) {
			return false;
		}
		
		final String expiryTime = cookieTokens[1];
		if (!Long.toString(Long.MAX_VALUE).equals(expiryTime)) {
			return false;
		}
		
		if (!cookieTokens[2].equals(LoginCommand.encodeCookie(id, Long.MAX_VALUE))) {
			return false;
		}
		
		return true;
	}
	
}
