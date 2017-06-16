/*
 * The MIT License
 *
 * Copyright 2014 Jesse Glick.
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

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebRequest;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.Permission;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import jenkins.model.Jenkins;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

/** Verifies use of REST API to manipulate promotions. */
@Issue("JENKINS-8963")
public class RemoteApiTest {

    @Rule public JenkinsRule r = new JenkinsRule();

    @Test public void getAndModify() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject("p");
        JobPropertyImpl promotion = new JobPropertyImpl(p);
        p.addProperty(promotion);
        PromotionProcess proc = promotion.addProcess("promo");
        proc.conditions.add(new SelfPromotionCondition(true));
        JenkinsRule.WebClient wc = r.createWebClient();
        String xml = wc.goToXml("job/p/promotion/process/promo/config.xml").getContent();
        assertTrue(xml, xml.contains("SelfPromotionCondition"));
        assertTrue(xml, xml.contains("<evenIfUnstable>true</evenIfUnstable>"));
        WebRequest req = new WebRequest(wc.createCrumbedUrl("job/p/promotion/process/promo/config.xml"), HttpMethod.POST);
        req.setEncodingType(null);
        req.setRequestBody(xml.replace("<evenIfUnstable>true</evenIfUnstable>", "<evenIfUnstable>false</evenIfUnstable>"));
        assertTrue(proc.conditions.get(SelfPromotionCondition.class).isEvenIfUnstable());
        wc.getPage(req);
        assertFalse(proc.conditions.get(SelfPromotionCondition.class).isEvenIfUnstable());
    }

    @Test public void acl() throws Exception {
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy pmas = new ProjectMatrixAuthorizationStrategy();
        r.jenkins.setAuthorizationStrategy(pmas);
        pmas.add(Jenkins.READ, "anonymous");
        FreeStyleProject p = r.createFreeStyleProject("p");
        Map<Permission,Set<String>> perms = new HashMap<Permission,Set<String>>();
        perms.put(Item.READ, Collections.singleton("alice"));
        perms.put(Item.CONFIGURE, Collections.singleton("alice"));
        perms.put(Item.DISCOVER, Collections.singleton("bob"));
        p.addProperty(new AuthorizationMatrixProperty(perms));
        JobPropertyImpl promotion = new JobPropertyImpl(p);
        p.addProperty(promotion);
        promotion.addProcess("promo").save();
        JenkinsRule.WebClient wc = r.createWebClient();
        wc.assertFails("job/p/promotion/process/promo/config.xml", HttpURLConnection.HTTP_NOT_FOUND);
        wc.login("bob");
        wc.assertFails("job/p/promotion/process/promo/config.xml", HttpURLConnection.HTTP_FORBIDDEN);
        wc.login("alice");
        wc.goToXml("job/p/config.xml");
        wc.goToXml("job/p/promotion/process/promo/config.xml");
    }

    @Test public void delete() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject("p");
        JobPropertyImpl promotion = new JobPropertyImpl(p);
        p.addProperty(promotion);
        promotion.addProcess("promo").save();
        assertEquals(1, promotion.getItems().size());
        assertEquals(1, promotion.getActiveItems().size());
        JenkinsRule.WebClient wc = r.createWebClient();
        wc.getPage(wc.addCrumb(new WebRequest(new URL(r.getURL(), "job/p/promotion/process/promo/doDelete"), HttpMethod.POST)));
        assertEquals(0, promotion.getItems().size());
        assertEquals(0, promotion.getActiveItems().size());
    }

    @Test public void create() throws Exception {
        FreeStyleProject p = r.createFreeStyleProject("p");
        JobPropertyImpl promotion = new JobPropertyImpl(p);
        p.addProperty(promotion);
        assertEquals(0, promotion.getItems().size());
        assertEquals(0, promotion.getActiveItems().size());
        JenkinsRule.WebClient wc = r.createWebClient();
        WebRequest req = new WebRequest(new URL(wc.createCrumbedUrl("job/p/promotion/createProcess") + "&name=promo"), HttpMethod.POST);
        req.setEncodingType(null);
        req.setRequestBody("<hudson.plugins.promoted__builds.PromotionProcess><conditions><hudson.plugins.promoted__builds.conditions.SelfPromotionCondition><evenIfUnstable>true</evenIfUnstable></hudson.plugins.promoted__builds.conditions.SelfPromotionCondition></conditions></hudson.plugins.promoted__builds.PromotionProcess>");
        wc.getPage(req);
        assertEquals(1, promotion.getItems().size());
        assertEquals("not yet in use", 0, promotion.getActiveItems().size());
        PromotionProcess proc = promotion.getItem("promo");
        assertNotNull(proc);
        assertTrue(proc.conditions.get(SelfPromotionCondition.class).isEvenIfUnstable());
    }

}
