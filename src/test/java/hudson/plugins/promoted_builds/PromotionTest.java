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
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

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
        assertSame(pb.getTarget(), b);

        try {
            r.createWebClient().goTo("job/"+p.getName()+"/1/promotion/"+promo1.getName()+"/promotionBuild/"+pb.getNumber()+"/rebuild");
            fail("rebuilding a promotion directly should fail");
        } catch (FailingHttpStatusCodeException x) {
            assertEquals("wrong status code", 404, x.getStatusCode());
            assertNotEquals("unexpected content", -1, x.getResponse().getContentAsString().indexOf("Promotions may not be rebuilt directly"));
        }
    }

}
