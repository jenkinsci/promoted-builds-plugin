package hudson.plugins.promoted_builds;

import hudson.Functions;
import hudson.model.FreeStyleProject;
import hudson.model.Items;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.model.FreeStyleBuild;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BatchFile;
import hudson.tasks.BuildTrigger;
import hudson.tasks.Fingerprinter;
import hudson.tasks.Recorder;
import hudson.tasks.Shell;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import net.sf.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.Stapler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static hudson.plugins.promoted_builds.util.ItemListenerHelper.fireItemListeners;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Kohsuke Kawaguchi
 */
@WithJenkins
class PromotionProcessTest {

    @Test
    void test1(JenkinsRule j) throws Exception {
        FreeStyleProject up = j.createFreeStyleProject("up");
        FreeStyleProject down = j.createFreeStyleProject();

        Recorder r1 = new ArtifactArchiver("a.jar", null, false);
        Recorder r2 = new Fingerprinter("", true);
        List<Recorder> recorders = Arrays.asList(r1, r2);

        // upstream job
        up.getBuildersList().add(Functions.isWindows()
                ? new BatchFile("date /t > a.jar")
                : new Shell("date > a.jar"));
        up.getPublishersList().replaceBy(recorders);

        // promote if the downstream passes
        JobPropertyImpl promotion = new JobPropertyImpl(up);
        up.addProperty(promotion);
        PromotionProcess proc = promotion.addProcess("promo");
        proc.conditions.add(new DownstreamPassCondition(down.getName()));

        // this is the test job
        String baseUrl = j.createWebClient().getContextPath() + "job/up/lastSuccessfulBuild";
        String artifactUrl = baseUrl + "/artifact/a.jar";
        down.getBuildersList().add(Functions.isWindows()
                ? new BatchFile("curl --remote-name "+artifactUrl+"\r\n"+
                        "set /a \"exitCode=BUILD_NUMBER%%2\"\r\n"+
                        "exit /b %exitCode%")
                : new Shell("wget "+artifactUrl+" || curl --remote-name "+artifactUrl+"\n"+
                        "expr $BUILD_NUMBER % 2 - 1") // expr exits with non-zero status if result is zero
        );
        down.getPublishersList().replaceBy(recorders);

        // fire ItemListeners, this includes ArtifactArchiver,Migrator to make this test compatible with jenkins 1.575+
        fireItemListeners();

        // not yet promoted while the downstream is failing
        FreeStyleBuild up1 = j.assertBuildStatusSuccess(up.scheduleBuild2(0).get());
        j.assertBuildStatus(Result.FAILURE,down.scheduleBuild2(0).get());
        Thread.sleep(1000); // give it a time to not promote
        assertEquals(0,proc.getBuilds().size());

        // a successful downstream build promotes upstream
        j.assertBuildStatusSuccess(down.scheduleBuild2(0).get());
        Thread.sleep(1000); // give it a time to promote
        assertEquals(1,proc.getBuilds().size());

        {// verify that it promoted the right stuff
            Promotion pb = proc.getBuilds().get(0);
            assertSame(pb.getTargetBuildOrFail(),up1);
            PromotedBuildAction badge = (PromotedBuildAction) up1.getBadgeActions().get(0);
            assertTrue(badge.contains(proc));
        }

        // make sure the UI persists the setup
        j.configRoundtrip(up);
    }

    /**
     * Tests a promotion induced by the pseudo upstream/downstream cause relationship
     */
    @Test
    void testPromotionWithoutFingerprint(JenkinsRule j) throws Exception {
        FreeStyleProject up = j.createFreeStyleProject("up");
        FreeStyleProject down = j.createFreeStyleProject();

        // promote if the downstream passes
        JobPropertyImpl promotion = new JobPropertyImpl(up);
        up.addProperty(promotion);
        PromotionProcess proc = promotion.addProcess("promo");
        proc.conditions.add(new DownstreamPassCondition(down.getName()));

        // trigger downstream automatically to create relationship
        up.getPublishersList().add(new BuildTrigger(down.getName(), Result.SUCCESS));
        j.jenkins.rebuildDependencyGraph();

        // this is the downstream job
        down.getBuildersList().add(Functions.isWindows()
                ? new BatchFile("""
										set /a "exitCode=BUILD_NUMBER%%2"
										exit /b %exitCode%
										""")
                : new Shell("expr $BUILD_NUMBER % 2 - 1")  // expr exits with non-zero status if result is zero
        );

        // not yet promoted while the downstream is failing
        FreeStyleBuild up1 = j.assertBuildStatusSuccess(up.scheduleBuild2(0).get());
        waitForCompletion(j, down,1);
        assertEquals(0,proc.getBuilds().size());

        // do it one more time and this time it should work
        FreeStyleBuild up2 = j.assertBuildStatusSuccess(up.scheduleBuild2(0).get());
        waitForCompletion(j, down,2);
        assertEquals(1,proc.getBuilds().size());

        {// verify that it promoted the right stuff
            Promotion pb = proc.getBuilds().get(0);
            assertSame(pb.getTargetBuildOrFail(),up2);
            PromotedBuildAction badge = (PromotedBuildAction) up2.getBadgeActions().get(0);
            assertTrue(badge.contains(proc));
        }
    }

    private void waitForCompletion(JenkinsRule j, FreeStyleProject down, int n) throws Exception {
        // wait for the build completion
        while (down.getBuildByNumber(n)==null)
            Thread.sleep(100);
        j.waitUntilNoActivity();
        assertFalse(down.getBuildByNumber(n).isBuilding());
    }

    @Test
    void testCaptureXml(JenkinsRule j) throws Exception {
        j.executeOnServer(() -> {
            JSONObject o = new JSONObject()
                    .accumulate("name", "foo")
                    .accumulate("isVisible", "")
                    .accumulate("icon", "star-gold")
                    .accumulate("conditions",new JSONObject()
                        .accumulate("hudson-plugins-promoted_builds-conditions-SelfPromotionCondition",
                                new JSONObject().accumulate("evenIfUnstable", false)));
            PromotionProcess p = PromotionProcess.fromJson(Stapler.getCurrentRequest2(), o);
            assertEquals("foo", p.getName());
            assertEquals("star-gold", p.getIcon());
            assertEquals(1, p.conditions.size());
            assertEquals(0, p.getBuildWrappers().size());
            assertNotNull(p.conditions.get(SelfPromotionCondition.class));
            System.out.println(Items.XSTREAM2.toXML(p));
            return null;
        });
    }

    @Test
    void testIsVisibleByDefault(JenkinsRule j) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject("project");
        JobPropertyImpl jobProperty = new JobPropertyImpl(project);
        project.addProperty(jobProperty);
        PromotionProcess promotionProcess = jobProperty.addProcess( "Promotion");
        assertTrue(promotionProcess.isVisible());
    }

    @Test
    void testIsVisibleFalseReturnsNotVisible(JenkinsRule j) throws Exception{
        FreeStyleProject project = j.createFreeStyleProject("project");
        JobPropertyImpl jobProperty = new JobPropertyImpl(project);
        project.addProperty(jobProperty);
        PromotionProcess promotionProcess = jobProperty.addProcess( "Promotion");
        promotionProcess.isVisible = "false";
        assertFalse(promotionProcess.isVisible());
    }

    @Test
    void testIsVisibleTrueReturnsVisible(JenkinsRule j) throws Exception{
        FreeStyleProject project = j.createFreeStyleProject("project");
        JobPropertyImpl jobProperty = new JobPropertyImpl(project);
        project.addProperty(jobProperty);
        PromotionProcess promotionProcess = jobProperty.addProcess( "Promotion");
        promotionProcess.isVisible = "true";
        assertTrue(promotionProcess.isVisible());
    }

    @Test
    void testIsVisibleResolvesDefaultParameterValue(JenkinsRule j) throws Exception{
        FreeStyleProject project = j.createFreeStyleProject("project");
        final List<ParameterDefinition> parameters = new ArrayList<>();
        ParametersDefinitionProperty parametersProperty = new ParametersDefinitionProperty(parameters);
        parameters.add(new StringParameterDefinition("Visibility", "false"));
        project.addProperty(parametersProperty);
        JobPropertyImpl jobProperty = new JobPropertyImpl(project);
        project.addProperty(jobProperty);
        PromotionProcess promotionProcess = jobProperty.addProcess( "Promotion");
        promotionProcess.isVisible = "${Visibility}";
        assertFalse(promotionProcess.isVisible());
    }

    @Test
    void testIsVisibleResolvesDefaultParameterValueIndirectly(JenkinsRule j) throws Exception{
        FreeStyleProject project = j.createFreeStyleProject("project");
        final List<ParameterDefinition> parameters = new ArrayList<>();
        ParametersDefinitionProperty parametersProperty = new ParametersDefinitionProperty(parameters);
        parameters.add(new StringParameterDefinition("IndirectVisibility", "false"));
        parameters.add(new StringParameterDefinition("Visibility", "${IndirectVisibility}"));
        project.addProperty(parametersProperty);
        JobPropertyImpl jobProperty = new JobPropertyImpl(project);
        project.addProperty(jobProperty);
        PromotionProcess promotionProcess = jobProperty.addProcess( "Promotion");
        promotionProcess.isVisible = "${Visibility}";
        assertFalse(promotionProcess.isVisible());
    }
}
