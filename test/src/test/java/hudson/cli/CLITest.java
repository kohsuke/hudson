package hudson.cli;

import hudson.Proc;
import hudson.Launcher.LocalLauncher;
import hudson.security.FullControlOnceLoggedInAuthorizationStrategy;
import hudson.security.HudsonPrivateSecurityRealm;
import hudson.util.IOUtils;
import hudson.util.StreamTaskListener;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.codehaus.plexus.util.StringOutputStream;
import org.jvnet.hudson.test.HudsonTestCase;

public class CLITest extends HudsonTestCase {

	private File cli;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		URL url = new URL(getURL(), "jnlpJars/hudson-cli.jar");
		cli = File.createTempFile("cli-", ".jar");
		IOUtils.copy(url.openStream(), cli);

	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		
		cli.delete();
	}

	private String runCommand(final String... args) throws IOException,
			InterruptedException {
		ArrayList<String> list = new ArrayList<String>() {
			{
				add("java");
				add("-jar");
				add(cli.getAbsolutePath());
				add("-s");
				add(getURL().toExternalForm());
				for (String arg : args)
					add(arg);
			}
		};
		
		
		StringOutputStream out = new StringOutputStream();
		LocalLauncher launcher = new LocalLauncher(new StreamTaskListener(
				System.out));
		Proc proc = launcher.launch().cmds(list).stdout(out).pwd(".")
				.start();
		proc.join();
		
		String result = out.toString();
		System.out.println(result);
		return result;
	}

	public void testHelp() throws Exception {
		String output = runCommand("help");
		assertTrue("unexpected output", output.contains("build"));
	}
	
	public void testSecuredButNotLoggedIn() throws Exception {
		hudson.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());
		createFreeStyleProject("foo");
		String output = runCommand("build", "foo");
		assertTrue("not getting an AccessDeniedException", output.contains("AccessDeniedException"));
	}

	public void testLogin() throws Exception {
		createFreeStyleProject("foo");
		
		HudsonPrivateSecurityRealm realm = new HudsonPrivateSecurityRealm(false);
		realm.createAccount("admin", "nimda");
		hudson.setSecurityRealm(realm);
		
		hudson.setAuthorizationStrategy(new FullControlOnceLoggedInAuthorizationStrategy());

		String output = runCommand("-u","admin","-p","nimda", "login");
		output = runCommand("who-am-i");
		assertEquals("expected who-am-i = admin", "admin", output.trim());
		
		runCommand("logout");
		
		output = runCommand("who-am-i");
		assertEquals("expected who-am-i = anonymous", "anonymous", output.trim());
	}
}
