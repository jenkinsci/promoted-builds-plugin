/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc.
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

import hudson.model.FreeStyleProject;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import hudson.tasks.JavadocArchiver;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import static com.gargoylesoftware.htmlunit.html.HtmlFormUtil.submit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Kohsuke Kawaguchi
 */
public class ConfigurationRoundtripTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    /**
     * Configuration roundtrip test to detect data loss.
     */
    @Test
    public void testRoundtrip() throws Exception {
        FreeStyleProject down = j.createFreeStyleProject();

        FreeStyleProject p = j.createFreeStyleProject();
        JobPropertyImpl pp = new JobPropertyImpl(p);
        p.addProperty(pp);

        PromotionProcess proc = pp.addProcess("test");
        assertEquals(1,pp.getItems().size());
        proc.conditions.add(new DownstreamPassCondition(down.getName()));
        proc.getBuildSteps().add(new JavadocArchiver("somedir",true));
        proc.icon = "star-blue";

        // round trip
        JenkinsRule.WebClient wc = j.createWebClient();
        submit(wc.getPage(p,"configure").getFormByName("config"));

        // assert that the configuration is still intact
        pp = p.getProperty(JobPropertyImpl.class);
        assertEquals(1,pp.getItems().size());
        proc = pp.getItem("test");
        assertEquals(1,proc.conditions.toList().size());
        DownstreamPassCondition dcp = proc.conditions.get(DownstreamPassCondition.class);
        assertEquals(dcp.getJobs(),down.getName());
        assertEquals(1,proc.getBuildSteps().size());
        JavadocArchiver ja = (JavadocArchiver)proc.getBuildSteps().get(0);
        assertEquals("somedir",ja.getJavadocDir());
        assertTrue(ja.isKeepAll());
        assertEquals("star-blue", proc.icon);
    }

    @LocalData
    @Issue("JENKINS-17341")
    @Test
    public void testLoad() throws Exception {
        FreeStyleProject jProj = j.jenkins.getItemByFullName("j", FreeStyleProject.class);
        assertNotNull(jProj);
        Promotion p = jProj.getProperty(JobPropertyImpl.class).getItem("OK").getBuildByNumber(1);
        assertNotNull(p);
    }

}
