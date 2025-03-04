package hudson.plugins.promoted_builds;

import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import org.jenkinsci.plugins.configfiles.GlobalConfigFiles;
import org.jenkinsci.plugins.configfiles.builder.ConfigFileBuildStep;
import org.jenkinsci.plugins.configfiles.buildwrapper.ManagedFile;
import org.jenkinsci.plugins.configfiles.custom.CustomConfig;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@WithJenkins
class PromotionConfigFilesTest {

    private static final String CONFIG_ID = "ConfigFilesTestId";

    @Test
    void testPromotionConfigFilesAreRetrievedFromParentJobContext(JenkinsRule r) throws Exception {
        GlobalConfigFiles store = r.getInstance().getExtensionList(GlobalConfigFiles.class).get(GlobalConfigFiles.class);
        assertTrue(store.getConfigs().isEmpty());

        CustomConfig config = new CustomConfig(CONFIG_ID, "name", "comment", "content");
        store.save(config);

        FreeStyleProject p = r.createFreeStyleProject();

        // promote if the downstream passes
        JobPropertyImpl promotion = new JobPropertyImpl(p);
        p.addProperty(promotion);

        PromotionProcess promo1 = promotion.addProcess("promo1");
        promo1.conditions.add(new SelfPromotionCondition(false));
        List<ManagedFile> managedFiles = new ArrayList<>();
        managedFiles.add(new ManagedFile(CONFIG_ID));
        promo1.getBuildSteps().add(new ConfigFileBuildStep(managedFiles));

        r.assertBuildStatusSuccess(p.scheduleBuild2(0));
        // internally, the promotion is still an asynchronous process. It just happens
        // right away after the build is complete.
        Thread.sleep(1000);

        Promotion pb = promo1.getBuilds().iterator().next();
        assertEquals(Result.SUCCESS, pb.getResult());
    }
}
