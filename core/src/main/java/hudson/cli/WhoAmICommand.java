package hudson.cli;

import hudson.Extension;
import hudson.model.Hudson;

@Extension
public class WhoAmICommand extends CLICommand {

	@Override
	public String getShortDescription() {
		return "Prints the name of the logged-in user";
	}

	@Override
	protected int run() throws Exception {
		stdout.println(Hudson.getAuthentication().getName());
		return 0;
	}

}
