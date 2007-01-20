package hudson.plugins.promoted_builds;

import hudson.Plugin;
import hudson.model.Items;

/**
 * @author Kohsuke Kawaguchi
 * @plugin
 */
public class PluginImpl extends Plugin {
    public void start() throws Exception {
        Items.LIST.add(PromotedJob.DESCRIPTOR);
    }
}
