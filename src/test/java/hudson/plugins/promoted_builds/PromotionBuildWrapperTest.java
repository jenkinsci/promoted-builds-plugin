package hudson.plugins.promoted_builds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.TestBuildWrapper;

import hudson.Functions;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;

/**
 * Tests for build wrapper functionality in promotion.
 * @author patrick.schlebusch
 */
public class PromotionBuildWrapperTest {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    @Test
    public void testApplyBuildWrapper() throws Exception {        
        FreeStyleProject project = j.createFreeStyleProject();
        JobPropertyImpl jobProperty = new JobPropertyImpl(project);
        project.addProperty(jobProperty);
        
        PromotionProcess promotion = jobProperty.addProcess("test");
        
        TestBuildWrapper buildWrapper = new TestBuildWrapper();
        promotion.getBuildWrappersList().add(buildWrapper);
        promotion.getBuildSteps().add(Functions.isWindows()
                ? new BatchFile("echo Hello, world")
                : new Shell("echo Hello, world"));
        
        // trigger build
        FreeStyleBuild build = j.assertBuildStatusSuccess(project.scheduleBuild2(0).get());
        // trigger promotion
        promotion.promote(build, new Cause.UserIdCause(), new ManualPromotionBadge());
        Thread.sleep(1000);
        
        assertEquals(1,promotion.getBuilds().size());
        Promotion promotionBuild = promotion.getBuilds().get(0);
        assertSame(promotionBuild.getTargetBuildOrFail(), build);
        assertEquals(Result.SUCCESS, buildWrapper.buildResultInTearDown);
    }

}
