package hudson.plugins.promoted_builds.tasks;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.RedeployPublisher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionProcess;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link RedeployPublisher} altered for batch task.
 * 
 * @author Kohsuke Kawaguchi
 */
public class RedeployBatchTaskPublisher extends RedeployPublisher {
    @DataBoundConstructor
    public RedeployBatchTaskPublisher(String id, String url, boolean uniqueVersion) {
        super(id, url, uniqueVersion);
    }

    /*@Override*/
    protected MavenModuleSetBuild getMavenBuild(AbstractBuild<?,?> build) {
        return super.getMavenBuild(((Promotion) build).getTargetBuild());
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends RedeployPublisher.DescriptorImpl {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return jobType== PromotionProcess.class;
        }

        @SuppressFBWarnings("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
        @Override
        public RedeployPublisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(RedeployBatchTaskPublisher.class,formData);
        }
    }
}
