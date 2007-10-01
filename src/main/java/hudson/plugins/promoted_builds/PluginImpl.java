package hudson.plugins.promoted_builds;

import hudson.Plugin;
import hudson.model.Jobs;
import hudson.tasks.BuildStep;

/**
 * @author Kohsuke Kawaguchi
 * @plugin
 */
public class PluginImpl extends Plugin {
    @Override
    public void start() throws Exception {
        new RunListenerImpl().register();
        Jobs.PROPERTIES.add(JobPropertyImpl.DescriptorImpl.INSTANCE);
    }
}
