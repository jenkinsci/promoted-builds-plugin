package hudson.plugins.promoted_builds;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.FreeStyleBuild;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.Fingerprinter;
import hudson.tasks.Recorder;
import hudson.tasks.Shell;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.Arrays;
import java.util.List;

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
    }
}
