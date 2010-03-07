package hudson.cli;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * This builds/checks a token that is only valid while a build is running
 * 
 * @author huybrechts
 * 
 */
public class BuildAuthentication {
	
	private static final String COOKIE_ENV = "HUDSON_CLI_COOKIE";

	public static void buildEnvVars(AbstractBuild<?,?> build, EnvVars envVars) {
		String cookie = build.getExternalizableId() + ":" + Hudson.getInstance().getSecretKey();
		String cookieHash = DigestUtils.md5Hex(cookie);
		envVars.put(COOKIE_ENV, cookieHash); 
	}

	public static boolean isValid(EnvVars envVars) {
		String cookieHash = envVars.get(COOKIE_ENV);
		String jobName = envVars.get("JOB_NAME");
		String buildNumber = envVars.get("BUILD_NUMBER");
		
		if (cookieHash == null || jobName == null || buildNumber == null) {
			return false;
		}
		
		AbstractProject<?,?> job = Hudson.getInstance().getItemByFullName(jobName, AbstractProject.class);
		if (job == null) {
			return false;
		}
		
		AbstractBuild<?,?> run;
        try {
            int n = Integer.parseInt(buildNumber);
            run = job.getBuildByNumber(n);
            if (run==null) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        
        String cookie = run.getExternalizableId() + ":" + Hudson.getInstance().getSecretKey();
        String expectedCookieHash = DigestUtils.md5Hex(cookie);
        
        if (!expectedCookieHash.equals(cookieHash)) {
        	return false;
        }
        
        if (!run.isBuilding()) {
        	return false;
        	
        }
        
        if (run.hasntStartedYet()) {
        	return false;
        }
        
        return true;
	}

}
