/*
 * The MIT License
 *
 * Copyright (c) 2014, Mads Mohr Christensen
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
package hudson.plugins.promoted_builds;

import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import hudson.model.*;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import hudson.plugins.promoted_builds.parameters.PromotedBuildParameterDefinition;
import hudson.plugins.promoted_builds.parameters.PromotedBuildParameterValue;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * @author Mads Mohr Christensen
 */
public class PromotedBuildRebuildParameterProviderTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testRebuild() throws Exception {
        // job with promotion process
        FreeStyleProject p1 = j.createFreeStyleProject("promojob");

        // setup promotion process
        JobPropertyImpl promotion = new JobPropertyImpl(p1);
        p1.addProperty(promotion);
        PromotionProcess proc = promotion.addProcess("promo");
        proc.conditions.add(new SelfPromotionCondition(false));

        // build it
        FreeStyleBuild b1 = j.assertBuildStatusSuccess(p1.scheduleBuild2(0));
        j.waitUntilNoActivity();

        // verify that promotion happened
        Assert.assertSame(proc.getBuilds().getLastBuild().getTargetBuildOrFail(), b1);

        // job with parameter
        FreeStyleProject p2 = j.createFreeStyleProject("paramjob");

        // add promoted build param
        p2.addProperty(new ParametersDefinitionProperty(
                new PromotedBuildParameterDefinition("var", "promojob", "promo", "promoted build param to test rebuild")
        ));

        // build with parameter
        FreeStyleBuild b2 = j.assertBuildStatusSuccess(p2.scheduleBuild2(0));

        // validate presence of parameter
        ParametersAction a1 = b2.getAction(ParametersAction.class);
        Assert.assertNotNull(a1);
        Assert.assertFalse(a1.getParameters().isEmpty());
        ParameterValue v1 = a1.getParameter("var");
        Assert.assertTrue(v1 instanceof PromotedBuildParameterValue);
        PromotedBuildParameterValue pbpv1 = (PromotedBuildParameterValue) v1;
        Assert.assertEquals(b1.getNumber(), pbpv1.getRun().getNumber());

        // rebuild it
        JenkinsRule.WebClient wc = j.createWebClient();
        HtmlPage buildPage = wc.getPage(b2);
        HtmlPage rebuildConfigPage = buildPage.getAnchorByText("Rebuild").click();
        j.submit(rebuildConfigPage.getFormByName("config"));
        j.waitUntilNoActivity();

        // validate presence of parameter
        FreeStyleBuild rebuild = p2.getLastBuild();
        j.assertBuildStatusSuccess(rebuild);
        Assert.assertNotEquals(b2.getNumber(), rebuild.getNumber());
        ParametersAction a2 = rebuild.getAction(ParametersAction.class);
        Assert.assertNotNull(a2);
        Assert.assertFalse(a2.getParameters().isEmpty());
        ParameterValue v2 = a2.getParameter("var");
        Assert.assertTrue(v2 instanceof PromotedBuildParameterValue);
        PromotedBuildParameterValue pbpv2 = (PromotedBuildParameterValue) v2;
        Assert.assertEquals(b1.getNumber(), pbpv2.getRun().getNumber());
    }
}
