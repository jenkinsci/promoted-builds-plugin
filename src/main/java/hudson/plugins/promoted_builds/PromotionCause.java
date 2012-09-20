package hudson.plugins.promoted_builds;

import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.console.HyperlinkNote;

/**
 * Cause used to indicate that a build was triggered by a promotion.  Extends
 * UpstreamCause so that existing build steps can use "the upstream build that
 * triggered this build" (or something like that).
 */
public class PromotionCause extends Cause.UpstreamCause {
    /** The promotion that triggered the build. */
    private Promotion promotion;
    
    /** The build that is being promoted. */
    private Run promotedBuild;

    /** URL to the job that was promoted. */
    private String promotedBuildUrl;
    
    /** The name of the job that was promoted. */
    private String promotedBuildProject;
    
    /** The number of the build that was promoted. */
    private String promotedBuildNumber;
    
    /* package */ PromotionCause(final Promotion promotion, final Run promotedBuild) {
        super(promotedBuild);
        
        this.promotion = promotion;
        this.promotedBuild = promotedBuild;

        promotedBuildUrl = promotedBuild.getParent().getUrl();
        promotedBuildProject = promotedBuild.getParent().getFullName();
        promotedBuildNumber = Integer.toString(promotedBuild.getNumber());
    }
    
    // {{{ getShortDescription
    /** {@inheritDoc} */
    @Override
    public String getShortDescription() {
        return Messages.PromotionCause_ShortDescription(
            promotion.getParent().getName(),
            promotedBuildProject,
            promotedBuildNumber
        );
    }
    // }}}
    
    // {{{ print
    /** {@inheritDoc} */
    @Override
    public void print(final TaskListener listener) {
        listener.getLogger().println(
            Messages.PromotionCause_ShortDescription(
                HyperlinkNote.encodeTo('/' + promotion.getUrl(), promotion.getParent().getName()),
                HyperlinkNote.encodeTo('/' + promotedBuildUrl, promotedBuildProject),
                HyperlinkNote.encodeTo('/' + promotedBuildUrl + promotedBuildNumber, promotedBuildNumber)
            )
        );
    }
    // }}}
}
