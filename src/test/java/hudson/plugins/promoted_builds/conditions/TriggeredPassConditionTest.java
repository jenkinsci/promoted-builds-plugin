/*
 * The MIT License
 * 
 * Copyright (c) 2013 IKEDA Yasuyuki
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.promoted_builds.conditions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jenkins.model.Jenkins;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.conditions.TriggeredPassCondition.Badge;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.BuildTrigger;
import hudson.tasks.Fingerprinter;

import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.SleepBuilder;

/**
 *
 */
public class TriggeredPassConditionTest extends HudsonTestCase {
    public void testTriggeredPassCondition() {
        {
            TriggeredPassCondition target = new TriggeredPassCondition(
                    "test1,test2",
                    false,
                    false
            );
            assertEquals("test1,test2", target.getExcludeProjectNames());
            assertFalse(target.isEvenIfUnstable());
            assertFalse(target.isOnlyDirectTriggered());
        }
        
        {
            TriggeredPassCondition target = new TriggeredPassCondition(
                    "",
                    true,
                    false
            );
            assertEquals("", target.getExcludeProjectNames());
            assertTrue(target.isEvenIfUnstable());
            assertFalse(target.isOnlyDirectTriggered());
        }
        
        {
            TriggeredPassCondition target = new TriggeredPassCondition(
                    null,
                    false,
                    true
            );
            assertNull(target.getExcludeProjectNames());
            assertFalse(target.isEvenIfUnstable());
            assertTrue(target.isOnlyDirectTriggered());
        }
    }
    
    public void testPromotion() throws Exception {
        FreeStyleProject up = createFreeStyleProject();
        FreeStyleProject down1 = createFreeStyleProject();
        
        String artifactContent = String.format("%d", System.currentTimeMillis());
        
        up.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        up.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        up.getPublishersList().add(new BuildTrigger(down1.getFullName(), Result.SUCCESS));
        
        down1.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        down1.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        
        JobPropertyImpl prop = new JobPropertyImpl(up);
        PromotionProcess pp = prop.addProcess("TriggeredPass");
        pp.conditions.add(new TriggeredPassCondition(null, false, false));
        up.addProperty(prop);
        
        up.save();
        down1.save();
        Jenkins.getInstance().rebuildDependencyGraph();
        
        assertBuildStatusSuccess(up.scheduleBuild2(0));
        waitUntilNoActivityUpTo(60000);
        assertBuildStatusSuccess(down1.getLastBuild());
        
        FreeStyleBuild b = up.getLastBuild();
        PromotedBuildAction action = b.getAction(PromotedBuildAction.class);
        assertNotNull(action);
        assertTrue(action.contains("TriggeredPass"));
    }
    
    public void testPromotionMulti() throws Exception {
        FreeStyleProject up = createFreeStyleProject();
        FreeStyleProject down1 = createFreeStyleProject();
        FreeStyleProject down2 = createFreeStyleProject();
        
        String artifactContent = String.format("%d", System.currentTimeMillis());
        
        up.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        up.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        up.getPublishersList().add(new BuildTrigger(String.format("%s,%s", down1.getFullName(), down2.getFullName()), Result.SUCCESS));
        
        down1.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        down1.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        
        down2.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        down2.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        
        JobPropertyImpl prop = new JobPropertyImpl(up);
        PromotionProcess pp = prop.addProcess("TriggeredPass");
        pp.conditions.add(new TriggeredPassCondition(null, false, false));
        up.addProperty(prop);
        
        up.save();
        down1.save();
        down2.save();
        Jenkins.getInstance().rebuildDependencyGraph();
        
        assertBuildStatusSuccess(up.scheduleBuild2(0));
        waitUntilNoActivityUpTo(60000);
        assertBuildStatusSuccess(down1.getLastBuild());
        assertBuildStatusSuccess(down2.getLastBuild());
        
        FreeStyleBuild b = up.getLastBuild();
        PromotedBuildAction action = b.getAction(PromotedBuildAction.class);
        assertNotNull(action);
        assertTrue(action.contains("TriggeredPass"));
        
        List<Badge> badges = Util.filter(action.getPromotion("TriggeredPass").getBadges(), Badge.class);
        assertNotNull(badges);
        assertEquals(1, badges.size());
        assertEquals(2, badges.get(0).getBuilds().size());
    }
    
    public void testPromotionExclude() throws Exception {
        FreeStyleProject up = createFreeStyleProject();
        FreeStyleProject down1 = createFreeStyleProject();
        FreeStyleProject down2 = createFreeStyleProject();
        
        String artifactContent = String.format("%d", System.currentTimeMillis());
        
        up.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        up.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        up.getPublishersList().add(new BuildTrigger(String.format("%s,%s", down1.getFullName(), down2.getFullName()), Result.SUCCESS));
        
        down1.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        down1.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        
        down2.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        down2.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        
        JobPropertyImpl prop = new JobPropertyImpl(up);
        PromotionProcess pp = prop.addProcess("TriggeredPass");
        pp.conditions.add(new TriggeredPassCondition(down1.getFullName(), false, false));
        up.addProperty(prop);
        
        up.save();
        down1.save();
        down2.save();
        Jenkins.getInstance().rebuildDependencyGraph();
        
        assertBuildStatusSuccess(up.scheduleBuild2(0));
        waitUntilNoActivityUpTo(60000);
        assertBuildStatusSuccess(down1.getLastBuild());
        assertBuildStatusSuccess(down2.getLastBuild());
        
        FreeStyleBuild b = up.getLastBuild();
        PromotedBuildAction action = b.getAction(PromotedBuildAction.class);
        assertNotNull(action);
        assertTrue(action.contains("TriggeredPass"));
        
        List<Badge> badges = Util.filter(action.getPromotion("TriggeredPass").getBadges(), Badge.class);
        assertNotNull(badges);
        assertEquals(1, badges.size());
        assertEquals(1, badges.get(0).getBuilds().size());
        assertTrue(badges.get(0).getBuilds().get(0).is(down2));
    }
    
    public void testPromotionMultiFailed() throws Exception {
        FreeStyleProject up = createFreeStyleProject();
        FreeStyleProject down1 = createFreeStyleProject();
        FreeStyleProject down2 = createFreeStyleProject();
        
        String artifactContent = String.format("%d", System.currentTimeMillis());
        
        up.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        up.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        up.getPublishersList().add(new BuildTrigger(String.format("%s,%s", down1.getFullName(), down2.getFullName()), Result.SUCCESS));
        
        down1.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        down1.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        
        down2.getBuildersList().add(new FailureBuilder());
        down2.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        down2.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        
        JobPropertyImpl prop = new JobPropertyImpl(up);
        PromotionProcess pp = prop.addProcess("TriggeredPass");
        pp.conditions.add(new TriggeredPassCondition(null, false, false));
        up.addProperty(prop);
        
        up.save();
        down1.save();
        down2.save();
        Jenkins.getInstance().rebuildDependencyGraph();
        
        assertBuildStatusSuccess(up.scheduleBuild2(0));
        waitUntilNoActivityUpTo(60000);
        assertBuildStatusSuccess(down1.getLastBuild());
        assertBuildStatus(Result.FAILURE, down2.getLastBuild());
        
        FreeStyleBuild b = up.getLastBuild();
        PromotedBuildAction action = b.getAction(PromotedBuildAction.class);
        assertNotNull(action);
        assertFalse(action.contains("TriggeredPass"));
    }
    
    public void testPromotionWithoutFingerprint() throws Exception {
        FreeStyleProject up = createFreeStyleProject();
        FreeStyleProject down1 = createFreeStyleProject();
        
        String artifactContent = String.format("%d", System.currentTimeMillis());
        
        up.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        up.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        up.getPublishersList().add(new BuildTrigger(down1.getFullName(), Result.SUCCESS));
        
        down1.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        
        JobPropertyImpl prop = new JobPropertyImpl(up);
        PromotionProcess pp = prop.addProcess("TriggeredPass");
        pp.conditions.add(new TriggeredPassCondition(null, false, false));
        up.addProperty(prop);
        
        up.save();
        down1.save();
        Jenkins.getInstance().rebuildDependencyGraph();
        
        assertBuildStatusSuccess(up.scheduleBuild2(0));
        waitUntilNoActivityUpTo(60000);
        assertBuildStatusSuccess(down1.getLastBuild());
        
        FreeStyleBuild b = up.getLastBuild();
        PromotedBuildAction action = b.getAction(PromotedBuildAction.class);
        assertNotNull(action);
        assertFalse(action.contains("TriggeredPass"));
    }
    
    public void testPromotionUnstable() throws Exception {
        FreeStyleProject up = createFreeStyleProject();
        FreeStyleProject down1 = createFreeStyleProject();
        
        String artifactContent = String.format("%d", System.currentTimeMillis());
        
        up.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        up.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        up.getPublishersList().add(new BuildTrigger(down1.getFullName(), Result.SUCCESS));
        
        down1.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        down1.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        down1.getPublishersList().add(new UnstableRecorder());
        
        JobPropertyImpl prop = new JobPropertyImpl(up);
        PromotionProcess pp = prop.addProcess("TriggeredPass");
        pp.conditions.add(new TriggeredPassCondition(null, false, false));
        up.addProperty(prop);
        
        up.save();
        down1.save();
        Jenkins.getInstance().rebuildDependencyGraph();
        
        assertBuildStatusSuccess(up.scheduleBuild2(0));
        waitUntilNoActivityUpTo(60000);
        assertBuildStatus(Result.UNSTABLE, down1.getLastBuild());
        
        FreeStyleBuild b = up.getLastBuild();
        PromotedBuildAction action = b.getAction(PromotedBuildAction.class);
        assertNotNull(action);
        // failed for build is unstable.
        assertFalse(action.contains("TriggeredPass"));
        
        artifactContent = String.format("2nd-%d", System.currentTimeMillis());
        up.getBuildersList().clear();
        up.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        down1.getBuildersList().clear();
        down1.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        
        prop = up.getProperty(JobPropertyImpl.class);
        pp = prop.getItem("TriggeredPass");
        pp.conditions.clear();
        pp.conditions.add(new TriggeredPassCondition(null, true, false));
        up.save();
        
        assertBuildStatusSuccess(up.scheduleBuild2(0));
        waitUntilNoActivityUpTo(60000);
        assertBuildStatus(Result.UNSTABLE, down1.getLastBuild());
        
        b = up.getLastBuild();
        action = b.getAction(PromotedBuildAction.class);
        assertNotNull(action);
        assertTrue(action.contains("TriggeredPass"));
    }
    
    public void testRetryAllowPromotion() throws Exception {
        FreeStyleProject up = createFreeStyleProject();
        FreeStyleProject down1 = createFreeStyleProject();
        
        String artifactContent = String.format("%d", System.currentTimeMillis());
        
        up.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        up.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        up.getPublishersList().add(new BuildTrigger(down1.getFullName(), Result.SUCCESS));
        
        down1.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        down1.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        down1.getPublishersList().add(new UnstableRecorder());
        
        JobPropertyImpl prop = new JobPropertyImpl(up);
        PromotionProcess pp = prop.addProcess("TriggeredPass");
        pp.conditions.add(new TriggeredPassCondition(null, false, false));
        up.addProperty(prop);
        
        up.save();
        down1.save();
        Jenkins.getInstance().rebuildDependencyGraph();
        
        assertBuildStatusSuccess(up.scheduleBuild2(0));
        waitUntilNoActivityUpTo(60000);
        assertBuildStatus(Result.UNSTABLE, down1.getLastBuild());
        
        FreeStyleBuild b = up.getLastBuild();
        PromotedBuildAction action = b.getAction(PromotedBuildAction.class);
        assertNotNull(action);
        // failed for build is unstable.
        assertFalse(action.contains("TriggeredPass"));
        
        down1.getPublishersList().clear();
        down1.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        down1.save();
        
        assertBuildStatusSuccess(down1.scheduleBuild2(0));
        waitUntilNoActivityUpTo(60000);
        
        assertTrue(action.contains("TriggeredPass"));
    }
    
    public void testOnlyDirectTriggered() throws Exception {
        FreeStyleProject up = createFreeStyleProject();
        FreeStyleProject down1 = createFreeStyleProject();
        
        String artifactContent = String.format("%d", System.currentTimeMillis());
        
        up.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        up.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        up.getPublishersList().add(new BuildTrigger(down1.getFullName(), Result.SUCCESS));
        
        down1.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        down1.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        down1.getPublishersList().add(new UnstableRecorder());
        
        JobPropertyImpl prop = new JobPropertyImpl(up);
        PromotionProcess pp = prop.addProcess("TriggeredPass");
        pp.conditions.add(new TriggeredPassCondition(null, false, true));
        up.addProperty(prop);
        
        up.save();
        down1.save();
        Jenkins.getInstance().rebuildDependencyGraph();
        
        assertBuildStatusSuccess(up.scheduleBuild2(0));
        waitUntilNoActivityUpTo(60000);
        assertBuildStatus(Result.UNSTABLE, down1.getLastBuild());
        
        FreeStyleBuild b = up.getLastBuild();
        PromotedBuildAction action = b.getAction(PromotedBuildAction.class);
        assertNotNull(action);
        // failed for build is unstable.
        assertFalse(action.contains("TriggeredPass"));
        
        down1.getPublishersList().clear();
        down1.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        down1.save();
        
        assertBuildStatusSuccess(down1.scheduleBuild2(0));
        waitUntilNoActivityUpTo(60000);
        
        // failed for the succeeded build is not directly triggered.
        assertFalse(action.contains("TriggeredPass"));
    }
    
    public void testManyTriggers() throws Exception {
        final int TRIGGERED_NUM = 10;
        
        FreeStyleProject up = createFreeStyleProject();
        List<FreeStyleProject> downList = new ArrayList<FreeStyleProject>(TRIGGERED_NUM);
        String artifactContent = String.format("%d", System.currentTimeMillis());
        
        for (int i = 0; i < TRIGGERED_NUM; ++i) {
            FreeStyleProject down = createFreeStyleProject();
            downList.add(down);
            down.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
            down.getBuildersList().add(new SleepBuilder(5000));
            down.getPublishersList().add(new Fingerprinter("artifact.txt", false));
            down.save();
        }
        
        up.getBuildersList().add(new FakeArtifactBuilder("artifact.txt", artifactContent));
        up.getPublishersList().add(new Fingerprinter("artifact.txt", false));
        up.getPublishersList().add(new BuildTrigger(downList, Result.SUCCESS));
        
        JobPropertyImpl prop = new JobPropertyImpl(up);
        PromotionProcess pp = prop.addProcess("TriggeredPass");
        pp.conditions.add(new TriggeredPassCondition(null, false, true));
        up.addProperty(prop);
        
        up.save();
        Jenkins.getInstance().rebuildDependencyGraph();
        
        assertBuildStatusSuccess(up.scheduleBuild2(0));
        waitUntilNoActivityUpTo(600000);
        for (FreeStyleProject down: downList) {
            assertBuildStatusSuccess(down.getLastBuild());
        }
        
        FreeStyleBuild b = up.getLastBuild();
        PromotedBuildAction action = b.getAction(PromotedBuildAction.class);
        assertNotNull(action);
        assertTrue(action.contains("TriggeredPass"));
        
        List<Badge> badges = Util.filter(action.getPromotion("TriggeredPass").getBadges(), Badge.class);
        assertNotNull(badges);
        assertEquals(1, badges.size());
        assertEquals(TRIGGERED_NUM, badges.get(0).getBuilds().size());
    }
    
    public static class FakeArtifactBuilder extends TestBuilder {
        private String filename;
        private String content;
        
        public FakeArtifactBuilder(String filename, String content) {
            this.filename = filename;
            this.content = content;
        }
        
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            FilePath file = build.getWorkspace().child(filename);
            file.write(content, "UTF-8");
            return true;
        }
    }
    
    public static class UnstableRecorder extends Recorder {
        @Override
        public BuildStepMonitor getRequiredMonitorService() {
            return BuildStepMonitor.NONE;
        }
        
        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            build.setResult(Result.UNSTABLE);
            return true;
        }
        
        public BuildStepDescriptor<Publisher> getDescriptor() {
            // throw new UnsupportedOperationException();
            return new BuildStepDescriptor<Publisher>() {
                @SuppressWarnings("rawtypes")
                @Override
                public boolean isApplicable(Class<? extends AbstractProject> jobType) {
                    return true;
                }
                
                @Override
                public String getDisplayName() {
                    return "Bogus";
                }
            };
        }
    }
}
