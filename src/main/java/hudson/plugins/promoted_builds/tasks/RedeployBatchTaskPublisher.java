package hudson.plugins.promoted_builds.tasks;

import hudson.maven.RedeployPublisher;
import hudson.maven.reporters.MavenAbstractArtifactRecord;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
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

    @Override
    protected MavenAbstractArtifactRecord getAction(AbstractBuild<?,?> build) {
        return ((Promotion)(AbstractBuild)build).getTarget().getAction(MavenAbstractArtifactRecord.class);
    }

    @Override
    public BuildStepDescriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends RedeployPublisher.DescriptorImpl {
        public DescriptorImpl() {
            super(RedeployPublisher.class);
        }

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
