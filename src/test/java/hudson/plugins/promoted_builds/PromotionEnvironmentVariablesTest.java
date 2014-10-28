package hudson.plugins.promoted_builds;

import hudson.EnvVars;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterValue;
import hudson.model.TaskListener;
import hudson.plugins.promoted_builds.conditions.ManualCondition;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import static org.junit.Assert.*;
import org.jvnet.hudson.test.MockFolder;

/**
 * @author Jonathan Zimmerman
 */

public class PromotionEnvironmentVariablesTest {
    
    @Rule public JenkinsRule r = new JenkinsRule();
    
    @Test
    public void shouldSetJobAndJobFullNames() throws Descriptor.FormException, IOException, InterruptedException, ExecutionException {
        // Assemble
        MockFolder parent = r.createFolder("Folder");
        FreeStyleProject project = parent.createProject(FreeStyleProject.class, "Project");
        
        JobPropertyImpl promotionProperty = new JobPropertyImpl(project);
        PromotionProcess promotionProcess = promotionProperty.addProcess("promo");
        promotionProcess.conditions.clear();
        promotionProcess.conditions.add(new ManualCondition());
        Action approvalAction = new ManualCondition.ManualApproval(promotionProcess.getName(), new ArrayList<ParameterValue>());
        
        // Act
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        build.setDisplayName("1234");
        build.addAction(approvalAction);
        build.save();
        
        Promotion promotion = promotionProcess.considerPromotion2(build).get();
        EnvVars env = promotion.getEnvironment(TaskListener.NULL);
        
        String rootUrl = r.jenkins.getRootUrl();
        
        // Assert
        assertEquals("Folder/Project", env.get("PROMOTED_JOB_FULL_NAME"));
        assertEquals("Project", env.get("PROMOTED_JOB_NAME"));
        assertEquals("SYSTEM", env.get("PROMOTED_USER_NAME"));
        assertEquals("SYSTEM", env.get("PROMOTED_USER_ID"));
        assertEquals("1234", env.get("PROMOTED_DISPLAY_NAME"));

        project.delete();
        parent.delete();
    }
    
}
