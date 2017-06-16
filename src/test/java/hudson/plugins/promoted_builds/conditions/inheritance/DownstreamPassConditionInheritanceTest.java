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
package hudson.plugins.promoted_builds.conditions.inheritance;

import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;

import hudson.model.Result;
import hudson.plugins.project_inheritance.projects.InheritanceBuild;
import hudson.plugins.project_inheritance.projects.InheritanceProject.IMode;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.Status;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import hudson.plugins.promoted_builds.inheritance.helpers.InheritanceProjectRule;
import hudson.plugins.promoted_builds.inheritance.helpers.InheritanceProjectsPair;
import hudson.tasks.BuildTrigger;
import org.jvnet.hudson.test.Issue;

public final class DownstreamPassConditionInheritanceTest {

    @Rule
    public InheritanceProjectRule j =  new InheritanceProjectRule();

    @Test
    @Issue("JENKINS-7739")
    public void shouldEvaluateUpstreamRecursively() throws Exception {
        final InheritanceProjectsPair pair1 = j.createInheritanceProjectDerivedWithBase();
        final InheritanceProjectsPair pair2 = j.createInheritanceProjectDerivedWithBase();
        final InheritanceProjectsPair pair3 = j.createInheritanceProjectDerivedWithBase();


        final JobPropertyImpl property = new JobPropertyImpl(pair1.getBase());
        pair1.getBase().addProperty(property);

        final PromotionProcess process = property.addProcess("promotion");
        process.conditions.add(new DownstreamPassCondition(pair3.getDerived().getFullName()));

        pair1.getDerived().getPublishersList().add(new BuildTrigger(pair2.getDerived().getFullName(), Result.SUCCESS));
        pair2.getDerived().getPublishersList().add(new BuildTrigger(pair3.getDerived().getFullName(), Result.SUCCESS));
        j.jenkins.rebuildDependencyGraph();

        final InheritanceBuild run1 = j.buildAndAssertSuccess(pair1.getDerived());
        j.assertBuildStatusSuccess(run1);
        j.waitUntilNoActivity();
        j.assertBuildStatusSuccess(pair2.getDerived().getLastBuild());
        j.waitUntilNoActivity();
        final InheritanceBuild run3 = j.assertBuildStatusSuccess(pair3.getDerived().getLastBuild());
        j.waitUntilNoActivity();

        //We cannot assume that the process will contain builds because the process added to base project is different to the one in derived. 
        JobPropertyImpl jobProperty = pair1.getDerived().getProperty(JobPropertyImpl.class, 
              /*Forcing inheritance as temporary hack for inheritance plugin 1.53 
                because that version of the plugin uses inheritance only for certain predefined cases: 
                -specific methods on the call stack
                -url paths.
                This has been changed as pull request https://github.com/i-m-c/jenkins-inheritance-plugin/pull/40
               */
                IMode.INHERIT_FORCED); 
         
        assertNotNull("derived jobProperty is null", jobProperty);
        PromotionProcess processDerived = jobProperty.getItem("promotion");
        
        assertEquals("fingerprint relation", run3.getUpstreamRelationship(pair1.getDerived()), -1);
        assertFalse("no promotion process", processDerived.getBuilds().isEmpty());

        final PromotedBuildAction action = run1.getAction(PromotedBuildAction.class);
        assertNotNull("no promoted action", action);

        final Status promotion = action.getPromotion("promotion");
        assertNotNull("promotion not found", promotion);
        assertTrue("promotion not successful", promotion.isPromotionSuccessful());
    }

}
