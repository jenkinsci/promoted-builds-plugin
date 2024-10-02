package hudson.plugins.promoted_builds.pipeline.PipelineConditions;

import hudson.Extension;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.plugins.promoted_builds.PromotionBadge;
import hudson.plugins.promoted_builds.conditions.SelfPromotionBadge;
import hudson.plugins.promoted_builds.pipeline.PipelinePromotionCondition;
import hudson.plugins.promoted_builds.pipeline.PipelinePromotionConditionDescriptor;
import hudson.plugins.promoted_builds.pipeline.PipelinePromotionProcess;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public class PipelineSelfPromotionCondition extends PipelinePromotionCondition {

    private final List<ParameterValue> parameters;
    private final boolean evenIfUnstable;

    public PipelineSelfPromotionCondition(@Nonnull List<ParameterValue> parameters, boolean evenIfUnstable){
      this.parameters = parameters;
      this.evenIfUnstable = evenIfUnstable;
    }


    private static String localJobName;



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

    /**  Singleton Design: private static List<ParameterValue> parameters;        //[We pass the parameters from the DSL this way.]
     *                         public static List<ParameterValue> getParameters(){
     *                             return parameters;
     *                         }
     * Note: parameters contain (Job name, Build Number)
     */
    @Override
    public List<ParameterValue> getParameters(){

        return Collections.unmodifiableList(parameters);
    }




   //Confusion as to how do i call ".considerPromotion2" without the "conditions" from the UI and by instead using the ParameterValue
   @Extension
    public static final class RunListenerImpl extends RunListener<Run<?,?>> {
        public RunListenerImpl() {
            super((Class)Run.class);
        }


      // My Expected approach but don't know how to call ".considerPromotion2" from here on!!
        /**
       @Override
       public void onCompleted(Run<?,?> run, TaskListener listener) {

           for(ParameterValue p : parameters){
               if (p.equals(localJobName)) {
                   try {
                       p.considerPromotion2(build);
                       break; // move on to the next process
                   } catch (IOException e) {
                       e.printStackTrace(listener.error("Failed to promote a build"));
                   }
               }

       */

        //Original Implementation
        /**
      @Override
      public void onCompleted(AbstractBuild<?,?> build, TaskListener listener) {
          JobPropertyImpl jp = build.getProject().getProperty(JobPropertyImpl.class);
          if(jp!=null) {
              for (PromotionProcess p : jp.getItems()) {
                  for (PromotionCondition cond : p.conditions) {
                      if (cond instanceof SelfPromotionCondition) {
                          try {
                              p.considerPromotion2(build);
                              break; // move on to the next process
                          } catch (IOException e) {
                              e.printStackTrace(listener.error("Failed to promote a build"));
                          }
                      }
                  }
              }
          }
      }
      */

    }

    @Extension
    public static final class DescriptorImpl extends PipelinePromotionConditionDescriptor {
        public boolean isApplicable(Job<?,?> item) {
            return true;
        }
        //Cannot resolve the return method

        public String getDisplayName() {

            return null; //Messages.SelfPromotionCondition_DisplayName();
        }

    }
}