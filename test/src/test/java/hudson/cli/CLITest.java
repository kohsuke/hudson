package hudson.cli;

import hudson.Proc;
import hudson.Launcher.LocalLauncher;
import hudson.util.IOUtils;
import hudson.util.StreamTaskListener;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

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

	private void runCommand(String... args) throws IOException, InterruptedException {
		ArrayList<String> list =new ArrayList<String>(Arrays.asList("java","-jar",cli.getAbsolutePath()));
		for (String arg: args) {
			list.add(arg);
		}
		LocalLauncher launcher = new LocalLauncher(new StreamTaskListener(
				System.out));
		Proc proc = launcher.launch().cmds(list).stdout(System.out).pwd(".")
				.start();
		proc.join();
	}

	public void testHelp() throws Exception {
		runCommand("-s", getURL().toExternalForm(), "help");
	}

}
