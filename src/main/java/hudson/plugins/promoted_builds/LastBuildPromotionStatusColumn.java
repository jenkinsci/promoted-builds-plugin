package hudson.plugins.promoted_builds;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ListView;
import hudson.model.Run;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a last build promotion summary column for {@link ListView}s.
 * @since 2.22
 */
public class LastBuildPromotionStatusColumn extends ListViewColumn {

    @DataBoundConstructor
    public LastBuildPromotionStatusColumn() {
        super();
    }

    public List<String> getPromotionIcons(final Item item) {
        List<String> icons = new ArrayList<String>();
        if (item instanceof Job<?, ?>) {
            final Job<?, ?> job = (Job<?, ?>) item;
            final Run<?, ?> b = job.getLastBuild();
            PromotedBuildAction a = b != null ? b.getAction(PromotedBuildAction.class) : null;
            if (a != null) {
                for (Status s : a.getPromotions()) {
                    PromotionProcess process = s.getProcess();
                    if (process !=null && process.isVisible()){
                        icons.add(s.getIcon());
                    }
                }
            }
        }
        return icons;
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {

        public DescriptorImpl() {
        }

        @Override
        public String getDisplayName() {
            return Messages.LastBuildPromotionStatusColumn_DisplayName();
        }

        @Override
        public boolean shownByDefault() {
            return false;
        }
    }

}
