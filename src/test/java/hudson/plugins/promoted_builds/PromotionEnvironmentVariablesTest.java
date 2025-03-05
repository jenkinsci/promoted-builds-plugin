package hudson.plugins.promoted_builds;

import hudson.EnvVars;
import hudson.model.Action;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.plugins.promoted_builds.conditions.ManualCondition;

import java.util.ArrayList;

import org.acegisecurity.context.SecurityContextHolder;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jvnet.hudson.test.MockFolder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Jonathan Zimmerman
 */

@WithJenkins
class PromotionEnvironmentVariablesTest {

    @Test
    void shouldSetJobAndJobFullNames(JenkinsRule r) throws Exception {
        // Assemble
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        User u = User.get("foo");
        u.setFullName("Foobar");

        SecurityContextHolder.getContext().setAuthentication(u.impersonate());

        MockFolder parent = r.createFolder("Folder");
        FreeStyleProject project = parent.createProject(FreeStyleProject.class, "Project");

        JobPropertyImpl promotionProperty = new JobPropertyImpl(project);
        PromotionProcess promotionProcess = promotionProperty.addProcess("promo");
        promotionProcess.conditions.clear();
        promotionProcess.conditions.add(new ManualCondition());
        Action approvalAction = new ManualCondition.ManualApproval(promotionProcess.getName(), new ArrayList<>());

        // Act
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        build.setDisplayName("1234");
        build.addAction(approvalAction);
        build.save();

        Promotion promotion = promotionProcess.considerPromotion2(build).get();
        EnvVars env = promotion.getEnvironment(TaskListener.NULL);

        // Assert
        assertEquals("Folder/Project", env.get("PROMOTED_JOB_FULL_NAME"));
        assertEquals("Project", env.get("PROMOTED_JOB_NAME"));
        assertEquals("Foobar", env.get("PROMOTED_USER_NAME"));
        assertEquals("foo", env.get("PROMOTED_USER_ID"));
        assertEquals("1234", env.get("PROMOTED_DISPLAY_NAME"));

        project.delete();
        parent.delete();
    }

}
