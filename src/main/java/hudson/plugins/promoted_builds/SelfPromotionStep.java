package hudson.plugins.promoted_builds;

import com.google.common.collect.ImmutableSet;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Set;


public class SelfPromotionStep extends Step {
    @CheckForNull
    private String job;

    // @Param(name="job")
    @DataBoundConstructor
    public SelfPromotionStep(String job){

        this.job = StringUtils.trimToNull(job);
    }

    public String getJob(){

        return job;
    }
    public void setJob(String Job){

        this.job = StringUtils.trimToNull(job);
    }
    // Confusion!!!!!!
    /*public StepExecution start(StepContext context) throws Exception{
        return new Execution(context,this);
    }**/

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName(){
            return "selfPromote";
        }

        @Override
        public String getDisplayName(){
            return "Self Promotion";}

        @Override
        public Set<Class<?>> getRequiredContext(){
            return ImmutableSet.of(Run.class, TaskListener.class);
            }
        }
        // Confusion
       /* public static class Execution extends SynchronousStepExecution<Void>{
            Execution(@Nonnull StepContext context, @Nonnull SelfPromotionStep step){
                super(context);
                this.step = step;
            }*/

       private static final long serialVersionUID = 1L;
        }
    }
}
