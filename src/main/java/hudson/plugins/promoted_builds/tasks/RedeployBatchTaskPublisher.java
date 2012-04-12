package hudson.plugins.promoted_builds.tasks;

import hudson.Extension;
import hudson.maven.MavenModuleSetBuild;
import hudson.maven.RedeployPublisher;
import hudson.maven.reporters.MavenAbstractArtifactRecord;
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
        // TODO remove copy and paste with delegation once core dependency is at least 1.448
        // return super.getMavenBuild(((Promotion) build).getTarget());
        build = ((Promotion) build).getTarget();
        return (build instanceof MavenModuleSetBuild)
                ? (MavenModuleSetBuild) build
                : null;
    }

    @Extension
    public static final class DescriptorImpl extends RedeployPublisher.DescriptorImpl {
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return jobType== PromotionProcess.class;
        }

        @Override
        public RedeployPublisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(RedeployBatchTaskPublisher.class,formData);
        }
    }
}
