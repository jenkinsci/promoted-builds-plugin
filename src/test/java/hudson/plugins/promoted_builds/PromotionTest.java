/*
 * The MIT License
 *
 * Copyright 2017 CloudBees, Inc.
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

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class PromotionTest {

    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void testRebuildPromotionThrowsException() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject("proj1");

        JobPropertyImpl promotion = new JobPropertyImpl(p);
        p.addProperty(promotion);

        PromotionProcess promo1 = promotion.addProcess("promo1");
        promo1.conditions.add(new SelfPromotionCondition(false));

        FreeStyleBuild b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
        // internally, the promotion is still an asynchronous process. It just happens
        // right away after the build is complete.
        Thread.sleep(1000);

        Promotion pb = promo1.getBuilds().getLastBuild();
        assertSame(pb.getTargetBuildOrFail(), b);

        JenkinsRule.WebClient wc = r.createWebClient();
        wc.goTo(pb.getUrl()); // spot-check that promotion itself is accessible

        try {
            HtmlPage page = wc.getPage(wc.addCrumb(new WebRequest(new URL(r.getURL(), pb.getUrl() + "/rebuild")
                    , HttpMethod.POST)));
            fail("rebuilding a promotion directly should fail");
        } catch (FailingHttpStatusCodeException x) {
            assertEquals("wrong status code", 404, x.getStatusCode());
            //TODO(oleg_nenashev): Another error will be returned since 2.107. As long as URL is rejected, we do not really care much
            // assertNotEquals("unexpected content", -1, x.getResponse().getContentAsString().indexOf("Promotions may not be rebuilt directly"));
        }
    }

    @Test
    @Issue("JENKINS-59600")
    public void testPromotionLog() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject("proj1");

        JobPropertyImpl promotion = new JobPropertyImpl(p);
        p.addProperty(promotion);

        PromotionProcess promo1 = promotion.addProcess("promo1");
        promo1.getBuildSteps().add(Functions.isWindows() ? new BatchFile("echo ABCDEFGH") : new Shell("echo ABCDEFGH"));
        promo1.conditions.add(new SelfPromotionCondition(false));

        FreeStyleBuild b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
        // internally, the promotion is still an asynchronous process. It just happens
        // right away after the build is complete.
        Thread.sleep(1000);

        Promotion pb = promo1.getBuilds().getLastBuild();
        assertSame(pb.getTargetBuildOrFail(), b);

        JenkinsRule.WebClient wc = r.createWebClient();
        final Page page = wc.goTo(pb.getUrl() + "consoleText", "text/plain");// spot-check that promotion itself is accessible
        assertThat(pb.getUrl() + "/consoleText + is not a promotion log", page.getWebResponse().getContentAsString(), CoreMatchers.containsString("ABCDEFGH"));
    }

}
