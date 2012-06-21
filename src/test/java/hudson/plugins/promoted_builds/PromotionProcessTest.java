package hudson.plugins.promoted_builds;

import hudson.model.FreeStyleProject;
import hudson.model.Items;
import hudson.model.Result;
import hudson.model.FreeStyleBuild;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildTrigger;
import hudson.tasks.Fingerprinter;
import hudson.tasks.Recorder;
import hudson.tasks.Shell;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import net.sf.json.JSONObject;
import org.jvnet.hudson.test.HudsonTestCase;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Kohsuke Kawaguchi
 */
public class PromotionProcessTest extends HudsonTestCase {
    public void test1() throws Exception {
        FreeStyleProject up = createFreeStyleProject("up");
        FreeStyleProject down = createFreeStyleProject();

        List<Recorder> recorders = Arrays.asList(
                new ArtifactArchiver("a.jar", null, false),
                new Fingerprinter("", true));

        // upstream job
        up.getBuildersList().add(new Shell("date > a.jar"));
        up.getPublishersList().replaceBy(recorders);

        // promote if the downstream passes
        JobPropertyImpl promotion = new JobPropertyImpl(up);
        up.addProperty(promotion);
        PromotionProcess proc = promotion.addProcess("promo");
        proc.conditions.add(new DownstreamPassCondition(down.getName()));

        // this is the test job
        String baseUrl = new WebClient().getContextPath() + "job/up/lastSuccessfulBuild";
        down.getBuildersList().add(new Shell(
            "wget -N "+baseUrl+"/artifact/a.jar \\\n"+
            "  || curl "+baseUrl+"/artifact/a.jar > a.jar\n"+
            "expr $BUILD_NUMBER % 2 - 1\n"  // expr exits with non-zero status if result is zero
        ));
        down.getPublishersList().replaceBy(recorders);

        // not yet promoted while the downstream is failing
        FreeStyleBuild up1 = assertBuildStatusSuccess(up.scheduleBuild2(0).get());
        assertBuildStatus(Result.FAILURE,down.scheduleBuild2(0).get());
        Thread.sleep(1000); // give it a time to not promote
        assertEquals(0,proc.getBuilds().size());

        // a successful downstream build promotes upstream
        assertBuildStatusSuccess(down.scheduleBuild2(0).get());
        Thread.sleep(1000); // give it a time to promote
        assertEquals(1,proc.getBuilds().size());

        {// verify that it promoted the right stuff
            Promotion pb = proc.getBuilds().get(0);
            assertSame(pb.getTarget(),up1);
            PromotedBuildAction badge = (PromotedBuildAction) up1.getBadgeActions().get(0);
            assertTrue(badge.contains(proc));
        }

        // make sure the UI persists the setup
        configRoundtrip(up);

    }

    /**
     * Tests a promotion induced by the pseudo upstream/downstream cause relationship
     */
    public void testPromotionWithoutFingerprint() throws Exception {
        FreeStyleProject up = createFreeStyleProject("up");
        FreeStyleProject down = createFreeStyleProject();

        // promote if the downstream passes
        JobPropertyImpl promotion = new JobPropertyImpl(up);
        up.addProperty(promotion);
        PromotionProcess proc = promotion.addProcess("promo");
        proc.conditions.add(new DownstreamPassCondition(down.getName()));

        // trigger downstream automatically to create relationship
        up.getPublishersList().add(new BuildTrigger(down.getName(), Result.SUCCESS));
        hudson.rebuildDependencyGraph();
        
        // this is the downstream job
        down.getBuildersList().add(new Shell(
            "expr $BUILD_NUMBER % 2 - 1\n"  // expr exits with non-zero status if result is zero
        ));

        // not yet promoted while the downstream is failing
        FreeStyleBuild up1 = assertBuildStatusSuccess(up.scheduleBuild2(0).get());
        waitForCompletion(down,1);
        assertEquals(0,proc.getBuilds().size());

        // do it one more time and this time it should work
        FreeStyleBuild up2 = assertBuildStatusSuccess(up.scheduleBuild2(0).get());
        waitForCompletion(down,2);
        assertEquals(1,proc.getBuilds().size());

        {// verify that it promoted the right stuff
            Promotion pb = proc.getBuilds().get(0);
            assertSame(pb.getTarget(),up2);
            PromotedBuildAction badge = (PromotedBuildAction) up2.getBadgeActions().get(0);
            assertTrue(badge.contains(proc));
        }
    }

    private void waitForCompletion(FreeStyleProject down, int n) throws InterruptedException {
        // wait for the build completion
        while (down.getBuildByNumber(n)==null)
            Thread.sleep(1000);
        while (down.getBuildByNumber(n).isBuilding())
            Thread.sleep(1000);
        Thread.sleep(1000); // give it a time to not promote
    }

    public void testCaptureXml() throws Exception {
        executeOnServer(new Callable<Object>() {
            public Object call() throws Exception {
                JSONObject o = new JSONObject()
                        .accumulate("name", "foo")
                        .accumulate("icon", "star-gold")
                        .accumulate("conditions",new JSONObject()
                            .accumulate("hudson-plugins-promoted_builds-conditions-SelfPromotionCondition",
                                    new JSONObject().accumulate("evenIfUnstable", false)));
                PromotionProcess p = PromotionProcess.fromJson(Stapler.getCurrentRequest(), o);
                assertEquals("foo", p.getName());
                assertEquals("star-gold", p.getIcon());
                assertEquals(1, p.conditions.size());
                assertNotNull(p.conditions.get(SelfPromotionCondition.class));
                System.out.println(Items.XSTREAM2.toXML(p));
                return null;
            }
        });
    }
}
