package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.ProminentProjectAction;

import java.util.List;

/**
 * For customizing project top-level GUI.
 * @author Kohsuke Kawaguchi
 */
public class PromotedProjectAction implements ProminentProjectAction {
    public final AbstractProject<?,?> owner;
    private final JobPropertyImpl property;

    public PromotedProjectAction(AbstractProject<?, ?> owner, JobPropertyImpl property) {
        this.owner = owner;
        this.property = property;
    }

    public List<PromotionProcess> getProcesses() {
        return property.getActiveItems();
    }

    public AbstractBuild<?,?> getLatest(PromotionProcess p) {
        return getLatest(p.getName());
    }

    /**
     * Finds the last promoted build under the given criteria.
     */
    public AbstractBuild<?,?> getLatest(String name) {
        for( AbstractBuild<?,?> build : owner.getBuilds() ) {
            PromotedBuildAction a = build.getAction(PromotedBuildAction.class);
            if(a!=null && a.contains(name))
                return build;
        }
        return null;
    }

    public String getIconFileName() {
        return "star.gif";
    }

    public String getDisplayName() {
        return "Promotion Status";
    }

    public String getUrlName() {
        return "promotion";
    }
}
