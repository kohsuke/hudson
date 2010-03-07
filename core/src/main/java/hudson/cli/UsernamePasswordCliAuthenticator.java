/**
 * 
 */
package hudson.cli;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Hudson;
import hudson.remoting.Callable;
import hudson.security.ACL;
import hudson.security.CliAuthenticator;
import hudson.security.SecurityRealm;

import java.io.Console;
import java.io.IOException;
import java.io.InterruptedIOException;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.AuthenticationServiceException;
import org.acegisecurity.BadCredentialsException;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.userdetails.UserDetails;
import org.kohsuke.args4j.Option;

public class UsernamePasswordCliAuthenticator extends CliAuthenticator {
	@Option(name = "--username", usage = "User name to authenticate yourself to Hudson")
	public String userName;
	@Option(name = "--password", usage = "Password for authentication. Note that passing a password in arguments is insecure.")
	public String password;
	@Option(name = "--password-file", usage = "File that contains the password")
	public String passwordFile;

	private final CLICommand command;
	private final SecurityRealm realm;

	public UsernamePasswordCliAuthenticator(SecurityRealm realm,
			CLICommand command) {
		this.command = command;
		this.realm = realm;
	}

	public Authentication authenticate() throws AuthenticationException {
		if (userName != null) {
			if (passwordFile != null) {
				try {
					password = new FilePath(command.channel, passwordFile)
							.readToString().trim();
				} catch (IOException e) {
					throw new BadCredentialsException("Failed to read "
							+ passwordFile, e);
				}
			}

			if (password == null) {
				try {
					password = command.channel.call(new GetPassword());
				} catch (IOException e) {
					throw new AuthenticationServiceException(
							"error while reading password", e);
				} catch (RuntimeException e) {
					throw new AuthenticationServiceException(
							"error while reading password", e);
				} catch (InterruptedException e) {
					throw new AuthenticationServiceException(
							"error while reading password", e);
				}
			}

			return realm.getSecurityComponents().manager.authenticate(new UsernamePasswordAuthenticationToken(userName, password));
		}

		try {
			// check for stored cookie
			String cred = LoginCommand.getCredentials(command.channel);
			if (cred != null) {
				UserDetails details = realm.getSecurityComponents().userDetails.loadUserByUsername(cred);
				return new UsernamePasswordAuthenticationToken(details.getUsername(), details.getPassword(), details.getAuthorities());
			}
		} catch (IOException e) {
			// ignore this, fallthrough
		} catch (InterruptedException e) {
			throw new AuthenticationServiceException(
					"interrupted", e);
		}

		try {
			EnvVars envVars = EnvVars.getRemote(command.channel);
			// If running inside a build, run as SYSTEM.
			// Dangerous, but don't care. Don't use this in a public Hudson !
			if (BuildAuthentication.isValid(envVars)) {
				return ACL.SYSTEM;
			}

		} catch (IOException e) {
			throw new AuthenticationServiceException(
					"error while reading password", e);
		} catch (InterruptedException e) {
			throw new AuthenticationServiceException(
					"error while reading password", e);
		}

		return Hudson.ANONYMOUS; // no authentication parameter. run as
		// anonymous

	}

	private static class GetPassword implements
			Callable<String, RuntimeException> {

		private static final long serialVersionUID = 1L;

		@Override
		public String call() throws RuntimeException {
			Console console = System.console();
			if (console == null) {
				return null;
			}

			char[] passwd = console.readPassword("[%s]", "Password:");
			return new String(passwd);
		}

	}
}