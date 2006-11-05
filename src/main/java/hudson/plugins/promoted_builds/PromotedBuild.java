package hudson.plugins.promoted_builds;

import hudson.model.Run;

import java.io.File;
import java.io.IOException;

/**
 * {@link Run} for {@link PromotedJob}.
 *
 */
public class PromotedBuild extends Run<PromotedJob,PromotedBuild> {
    /**
     * Creates a new promoted build and assigns an unique number.
     */
    protected PromotedBuild(PromotedJob job) throws IOException {
        super(job);
    }

    /**
     * Loads a job from the file.
     */
    protected PromotedBuild(PromotedJob project, File buildDir) throws IOException {
        super(project, buildDir);
    }
}
