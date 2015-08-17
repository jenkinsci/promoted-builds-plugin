package hudson.plugins.promoted_builds;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.Run;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;

import java.util.ArrayList;
import java.util.List;

public class PromotionStatusColumn extends ListViewColumn {

    @DataBoundConstructor
    public PromotionStatusColumn() {
        super();
    }

    public List<String> getPromotionIcons(Job job) {
        List<String> icons = new ArrayList<String>();
        Run b = job.getLastBuild();
        PromotedBuildAction a = b.getAction(PromotedBuildAction.class);
        for(Status s: a.getPromotions() ) {
            icons.add(s.getIcon("16x16"));
	}
        return icons;
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {

        public DescriptorImpl() {
        }

        @Override
        public String getDisplayName() {
            return Messages.PromotionStatusColumn_DisplayName();
        }

        public boolean shownByDefault() {
            return false;
        }
    }

}
