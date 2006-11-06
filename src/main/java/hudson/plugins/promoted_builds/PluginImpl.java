package hudson.plugins.promoted_builds;

import hudson.model.Jobs;

/**
 * @author Kohsuke Kawaguchi
 * @plugin
 */
public class PluginImpl {
    public void start() throws Exception {
        Jobs.JOBS.add(PromotedJob.DESCRIPTOR);
    }
}
