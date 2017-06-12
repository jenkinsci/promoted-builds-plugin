/*
 * The MIT License
 *
 * Copyright 2015 Franta Mejta
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

import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.Status;
import hudson.tasks.BuildTrigger;

public final class DownstreamPassConditionTest {

    @Rule
    public JenkinsRule j =  new JenkinsRule();

    @Test
    @Issue("JENKINS-7739")
    public void shouldEvaluateUpstreamRecursively() throws Exception {
        final FreeStyleProject job1 = j.createFreeStyleProject("job1");
        final FreeStyleProject job2 = j.createFreeStyleProject("job2");
        final FreeStyleProject job3 = j.createFreeStyleProject("job3");

        final JobPropertyImpl property = new JobPropertyImpl(job1);
        job1.addProperty(property);

        final PromotionProcess process = property.addProcess("promotion");
        process.conditions.add(new DownstreamPassCondition(job3.getFullName()));

        job1.getPublishersList().add(new BuildTrigger(job2.getFullName(), Result.SUCCESS));
        job2.getPublishersList().add(new BuildTrigger(job3.getFullName(), Result.SUCCESS));
        j.jenkins.rebuildDependencyGraph();

        final FreeStyleBuild run1 = j.buildAndAssertSuccess(job1);
        j.waitUntilNoActivity();
        j.assertBuildStatusSuccess(job2.getLastBuild());
        j.waitUntilNoActivity();
        final FreeStyleBuild run3 = j.assertBuildStatusSuccess(job3.getLastBuild());
        j.waitUntilNoActivity();

        assertEquals("fingerprint relation", run3.getUpstreamRelationship(job1), -1);
        assertFalse("no promotion process", process.getBuilds().isEmpty());

        final PromotedBuildAction action = run1.getAction(PromotedBuildAction.class);
        assertNotNull("no promoted action", action);

        final Status promotion = action.getPromotion("promotion");
        assertNotNull("promotion not found", promotion);
        assertTrue("promotion not successful", promotion.isPromotionSuccessful());
    }

}
