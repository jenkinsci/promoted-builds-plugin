package hudson.plugins.promoted_builds;

import hudson.model.ParameterValue;
import hudson.model.Run;

import javax.annotation.Nonnull;
import java.util.List;

public interface PromotionPipeline {
    //Run which we try to promote
    @Nonnull
    Run<?,?> getPromotedRun();

    //Execution which does the promotion
    @Nonnull
    Run<?,?> getPromotionRun();

    List<ParameterValue> getParameterValues();
}
