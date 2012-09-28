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
    /** The name of the promotion process. */
    private String promotionProcessName;

    /** URL to the promotion. */
    private String promotionBuildUrl;

    /** The build number of the promotion */
    private int promotionBuildNumber;

    /* package */ PromotionCause(final Promotion promotion, final Run promotedBuild) {
        super(promotedBuild);
        
        promotionProcessName = promotion.getParent().getName();
        promotionBuildUrl = promotion.getUrl();
        promotionBuildNumber = promotion.getNumber();
    }
    
    // {{{ getShortDescription
    /** {@inheritDoc} */
    @Override
    public String getShortDescription() {
        return Messages.PromotionCause_ShortDescription(
            promotionProcessName + " #" + promotionBuildNumber,
            getUpstreamProject(),
            Integer.toString(getUpstreamBuild())
        );
    }
    // }}}
    
    // {{{ print
    /** {@inheritDoc} */
    @Override
    public void print(final TaskListener listener) {
        listener.getLogger().println(
            Messages.PromotionCause_ShortDescription(
                HyperlinkNote.encodeTo('/' + promotionBuildUrl,  promotionProcessName + " #" + promotionBuildNumber),
                HyperlinkNote.encodeTo('/' + getUpstreamUrl(), getUpstreamProject()),
                HyperlinkNote.encodeTo('/' + getUpstreamUrl() + getUpstreamBuild(), Integer.toString(getUpstreamBuild()))
            )
        );
    }
    // }}}
}
