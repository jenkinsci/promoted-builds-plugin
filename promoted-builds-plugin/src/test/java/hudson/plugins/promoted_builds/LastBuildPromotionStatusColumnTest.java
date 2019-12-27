/*
 * The MIT License
 *
 * Copyright (c) 2015 Oleg Nenashev.
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

import hudson.model.Cause;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ListView;
import java.io.IOException;
import java.util.List;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Tests for {@link LastBuildPromotionStatusColumn}.
 * @author Oleg Nenashev
 */
public class LastBuildPromotionStatusColumnTest {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    private ListView view;
    private LastBuildPromotionStatusColumn column;
    
    @Before
    public void initView() throws IOException {
        view = new ListView("testView", j.jenkins);
        column = new LastBuildPromotionStatusColumn();
        view.getColumns().add(column);
        j.jenkins.addView(view);
    }
    
    @Test
    public void shouldDisplayStars() throws Exception {
        // Create project
        FreeStyleProject p = j.createFreeStyleProject();
        JobPropertyImpl base = new JobPropertyImpl(p);
        p.addProperty(base);
        PromotionProcess foo = base.addProcess("foo");
        foo.icon = "star-blue";
        view.add(p);
        
        // Promote a build
        FreeStyleBuild b1 = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        foo.promote(b1, new Cause.UserIdCause(), new ManualPromotionBadge());
        
        // Check column contents
        LastBuildPromotionStatusColumn retrieved = 
                view.getColumns().get(LastBuildPromotionStatusColumn.class);
        assertEquals("Columns should be same", column, retrieved);
        List<String> promotionIcons = retrieved.getPromotionIcons(p);
        assertEquals("Expected only 1 promotion icon", 1, promotionIcons.size());
        assertTrue("Promotion should assign the blue star", promotionIcons.get(0).contains("star-blue"));
    }
}
