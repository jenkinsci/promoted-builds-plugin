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
import org.kohsuke.stapler.StaplerRequest2;

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
        return super.getMavenBuild(((Promotion) build).getTargetBuildOrFail());
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends RedeployPublisher.DescriptorImpl {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return jobType== PromotionProcess.class;
        }

        @Override
        public RedeployPublisher newInstance(StaplerRequest2 req, JSONObject formData) throws FormException {
            // Javadoc of Descriptor.newInstance() states that req is always non-null, but legacy
            // requirements cause it to retain the Nullable annotation
            // Silence spotbugs by handling null
            if (req == null) {
                throw new FormException("Unexpected null req passed to newInstance", "unknown field");
            }
            return req.bindJSON(RedeployBatchTaskPublisher.class,formData);
        }
    }
}
