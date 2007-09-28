package hudson.plugins.promoted_builds;

import hudson.Plugin;
import hudson.tasks.BuildStep;

/**
 * @author Kohsuke Kawaguchi
 * @plugin
 */
public class PluginImpl extends Plugin {
    public void start() throws Exception {
        BuildStep.PUBLISHERS.addRecorder(PublisherImpl.DescriptorImpl.INSTANCE);
    }
}
