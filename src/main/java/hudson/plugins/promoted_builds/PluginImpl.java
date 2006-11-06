package hudson.plugins.promoted_builds;

import hudson.model.Jobs;
import hudson.Plugin;

/**
 * @author Kohsuke Kawaguchi
 * @plugin
 */
public class PluginImpl extends Plugin {
    public void start() throws Exception {
        Jobs.JOBS.add(PromotedJob.DESCRIPTOR);
    }
}
