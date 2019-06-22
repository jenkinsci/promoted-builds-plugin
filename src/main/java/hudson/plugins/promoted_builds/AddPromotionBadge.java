package hudson.plugins.promoted_builds;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.*;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.io.PrintStream;

public class AddPromotionBadge extends Recorder implements SimpleBuildStep {

    private String boxName = "Promotion Badges";
    private boolean gold;
    private boolean silver;

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @DataBoundConstructor
    public AddPromotionBadge(String boxName, boolean gold, boolean silver){
        this.boxName = boxName;
        this.gold = gold;
        this.silver = silver;
    }

    public String getBoxName(){
        return boxName;
    }

    @DataBoundSetter
    public void setBoxName(String boxName){
        this.boxName = boxName;
    }


    public boolean getGold(){
        return gold;
    }

    @DataBoundSetter
    public void setGold(boolean gold){
        this.gold = gold;
    }

    public boolean getSilver(){
        return silver;
    }

    @DataBoundSetter
    public void setSilver(boolean silver){
        this.silver = silver;
    }

    public BuildStepMonitor getRequiredMonitorService(){
        return BuildStepMonitor.NONE;
    }


    @Override
    public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, final TaskListener listener)
            throws InterruptedException, IOException {

        PrintStream logger = listener.getLogger();
        logger.println("Started initialization");

        Result buildResult = build.getResult();

        if (Result.SUCCESS.equals(buildResult)) {
            logger.println("The pipeline build is successful!!");

            if(gold == true){
                // How to assign actual badge icons??
              }

        }

        String temp;

        if(build instanceof AbstractBuild){
            EnvVars vars = build.getEnvironment(listener);
            vars.overrideAll(((AbstractBuild)build).getBuildVariables());
            temp = vars.expand(boxName);
        }else{
            temp = boxName;
        }

        logger.println("Starting the pipeline build");
        logger.println("Will assign badges once the pipeline build is successful in promotion");

    }


    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher>{

        private DescriptorImpl(){
            super(AddPromotionBadge.class);
        }

        public String getDisplayName(){
            return "Add Promotion Badges";
        }


        @Override
        public boolean isApplicable(Class<? extends AbstractProject> a){
            return true;
        }

    }




}
