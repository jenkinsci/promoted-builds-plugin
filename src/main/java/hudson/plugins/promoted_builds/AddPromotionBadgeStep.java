package hudson.plugins.promoted_builds;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

// A test step to check whether the promotion was successful or not.
// Later this step would be replaced by triggering a new "Promotion job"
public class AddPromotionBadgeStep extends Step{

    private static final Logger LOGGER = Logger.getLogger("The Job was successfully Promoted");

    // "job" AND "buildNumber" will be passed from other promotion Conditions
    @CheckForNull
    private String job;
    private int buildNumber;

    @DataBoundConstructor
    public AddPromotionBadgeStep(String job, int buildNumber){
        this.job = StringUtils.trimToNull(job);
        this.buildNumber = buildNumber;
    }

    public String getJob(){
        return job;
    }
    public void setJob(String job){
        this.job = StringUtils.trimToNull(job);
    }
    public int getBuildNumber(){
        return buildNumber;
    }
    public void setBuildNumber(int buildNumber){
        this.buildNumber = buildNumber;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception{
        return new Execution(context,getJob(),getBuildNumber());
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor{

        @Override
        public String getFunctionName(){
            return "addTestBadge";
        }
        @Override
        public String getDisplayName(){
            return "Add Test Promotion Step";
        }

        @Override
        public Set<Class<?>> getRequiredContext(){
            return ImmutableSet.of(Run.class,TaskListener.class);
        }
    }

    public static class Execution extends AbstractSynchronousStepExecution<Void>{

        //Escapes Serialization
        private transient final String job;
        private transient final int buildNumber;

        Execution(@Nonnull StepContext context,@Nonnull String job,@Nonnull int buildNumber){


                super(context);
                this.job = StringUtils.trimToNull(job);
                this.buildNumber = buildNumber;
        }

        @Override
        protected Void run() throws Exception{
            LOGGER.log(Level.FINER, "A dummy Step for Spot Promotion Check" );
            return null;
        }

        private static final long serialVersionUID = 1L;
    }

}