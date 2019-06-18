package hudson.plugins.promoted_builds.conditions;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.ParameterValue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.plugins.promoted_builds.PipelinePromotionCondition;
import hudson.plugins.promoted_builds.PipelinePromotionProcess;
import hudson.plugins.promoted_builds.PromotionBadge;
import org.omg.Dynamic.Parameter;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PipelineSelfPromotionCondition extends PipelinePromotionCondition {

    private final List<ParameterValue> parameters;
    private final boolean evenIfUnstable;

    public PipelineSelfPromotionCondition(@Nonnull List<ParameterValue> parameters, boolean evenIfUnstable){
      this.parameters = parameters;
      this.evenIfUnstable = evenIfUnstable;
    }


    String localJobName;



    @Override
    public PromotionBadge isMet(PipelinePromotionProcess promotionProcess, Run<?,?> run){

        localJobName = run.getParent().getFullName();


        if(!run.isBuilding()){
            Result r = run.getResult();
            if((r == Result.SUCCESS) || (evenIfUnstable && r == Result.UNSTABLE){
                return new SelfPromotionBadge();
            }

        }
        return null;
    }

    /**  Singleton Design: private static List<ParameterValue> parameters;
     *                         public static List<ParameterValue> getParameters(){
     *                             return parameters;
     *                         }
     * Note: parameters contain (Job name, Build Number)
     */
    @Override
    public List<ParameterValue> getParameters(){

        return Collections.unmodifiableList(parameters);
    }


   /** @CheckForNull
    public ParameterValue getPromotionDecision(String localJobName){
        for(ParameterValue p : parameters){
            if(localJobName.equals(p.getJobName())){
                return p;
            }
        }
        return null;
    }
    */


   //Confusion as to how do i call ".considerPromotion2" without the "conditions" from the UI and by instead using the ParameterValue
   @Extension
    public static final class RunListenerImpl extends RunListener<Run<?,?>> {
        public RunListenerImpl() {
            super((Class)Run.class);
        }

        @Override
        public void onCompleted(Run<?,?> build, TaskListener listener, EnvVars vars) {
            for(ParameterValue p : parameters){
                if (p instanceof Run) {
                            try {
                                p.considerPromotion2(build);
                                break; // move on to the next process
                            } catch (IOException e) {
                                e.printStackTrace(listener.error("Failed to promote a build"));
                            }
                        }



        }





}
