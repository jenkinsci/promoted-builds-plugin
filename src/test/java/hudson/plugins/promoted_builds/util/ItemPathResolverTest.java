/*
 * The MIT License
 *
 * Copyright (c) 2015 CloudBees, Inc.
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
package hudson.plugins.promoted_builds.util;

import hudson.model.FreeStyleProject;
import hudson.model.Item;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;

import static org.junit.Assert.*;
import org.junit.Before;
import org.jvnet.hudson.test.TestExtension;

/**
 * Tests for {@link ItemPathResolver}.
 * @author Oleg Nenashev
 */
public class ItemPathResolverTest {
    
    @Rule
    public final JenkinsRule rule = new JenkinsRule();
    
    private MockFolder folderA, folderB, folderC;
    private FreeStyleProject projectInTop, projectInC1, projectInC2;
    
    @Before
    public void setUpFolders() throws Exception {
        folderA = rule.createFolder("a");
        folderB = folderA.createProject(MockFolder.class, "b");
        folderC = folderB.createProject(MockFolder.class, "c");
        
        projectInTop = rule.createFreeStyleProject( "prj");
        projectInC1 = folderC.createProject(FreeStyleProject.class, "prjInC1");
        projectInC2 = folderC.createProject(FreeStyleProject.class, "prjInC2");
    }
      
    @Test
    public void shouldRetainTheLegacyBehaviorIfEnabled() throws Exception {
        assertsPath("prj", null, projectInTop);
        
        // FOO exists on both top level and within the folder
        final MockFolder folder = rule.createFolder("F");
        final FreeStyleProject prjInRoot = rule.createFreeStyleProject("FOO");
        final FreeStyleProject prjInFolder = folder.createProject(FreeStyleProject.class, "FOO");
        
        // Raw addressing with different roots, should always point to the root
        assertsPath("FOO", null, prjInRoot);
        assertsPath("FOO", prjInRoot, prjInRoot);
        assertsPath("FOO", folder, prjInRoot);
        assertsPath("FOO", prjInFolder, prjInRoot);
    }
    
    @Test
    public void shouldProvideNewBehaviorByDefault() throws Exception {
        assertsPath("prj", null, projectInTop);
        
        // FOO exists on both top level and within the folder
        final MockFolder folder = rule.createFolder("F");
        final FreeStyleProject prjInRoot = rule.createFreeStyleProject("FOO");
        final FreeStyleProject prjInFolder = folder.createProject(FreeStyleProject.class, "FOO");
        
        // Raw addressing with different roots, should always point to the root
        assertsPath("FOO", null, prjInRoot);
        assertsPath("FOO", prjInRoot, prjInRoot);
        assertsPath("FOO", folder, prjInFolder);
        assertsPath("FOO", prjInFolder, prjInFolder);
    }
    
    @Test
    public void shouldSupportBasicAddressing() throws Exception {
        assertsPath("a", null, folderA);
    }
    
    @Test
    public void shouldSupportAbsoluteAddressing() throws Exception {
        assertsPath("/a/b", null, folderB);
        assertsPath("/a/b/c", null, folderC);
        assertsPath("/prj", null, projectInTop);     
        assertsPath("/a/b/c/prjInC1", null, projectInC1);
        
        // wrong scenario
        assertsPathIsNull("/a/c", null, FreeStyleProject.class);
    }
    
    @Test
    public void shouldSupportAbsoluteAddressingWithRelativeBase() throws Exception {
        assertsPath("/a/b", folderB, folderB);
        assertsPath("/a/b/c", folderC, folderC);
        assertsPath("/prj", folderA, projectInTop);
        assertsPath("/a/b/c/prjInC1", folderA, projectInC1);
        
        // wrong scenario
        assertsPathIsNull("/a/c", folderA, FreeStyleProject.class);
    }
    
    @Test
    public void shouldSupportRelative() throws Exception {
        assertsPath("./b", folderA, folderB);
        assertsPath("./b/c", folderA, folderC);
        assertsPath("./b/./c", folderA, folderC);
        assertsPath("..", folderB, folderA);
        assertsPath("./b/..", folderA, folderA);
        assertsPath("./b/c/..", folderA, folderB);
        
        // Path overflow
        assertsPathIsNull("../../..", folderC, Item.class);
    }
    
    @Test
    public void shouldSupportRelativeWithoutPrefix() throws Exception {
        assertsPath("b", folderA, folderB);
        assertsPath("b/c", folderA, folderC);
        assertsPath("b/./c", folderA, folderC);
        assertsPath("b/..", folderA, folderA);
        assertsPath("b/c/..", folderA, folderB);
    }
    
    @Test
    public void shouldSupportRelativeAddressingFromItems() throws Exception {
        assertsPath("./prjInC2", projectInC1, projectInC2);
        assertsPath(".", projectInC1, folderC);
        assertsPath("..", projectInC1, folderB);
        
        // wrong scenario
        assertsPathIsNull("/a/c", null, FreeStyleProject.class);
    }
    
    private <T extends Item> void assertsPath(@NonNull String path, @CheckForNull Item base, 
            Item expected, @NonNull Class<T> clazz) throws Exception {
        final Item res = ItemPathResolver.getByPath(path, base, clazz);
        if (expected != res) {
            StringBuilder errorDetails = new StringBuilder("Wrong result for clase='")
                    .append(clazz).append("', path='").append(path).append("'");
            if (base != null) {
                errorDetails.append(" with base element '").append(base.getFullName()).append("'");
            } else {
                errorDetails.append(" with NULL base element");
            }
            assertEquals(errorDetails.toString(), expected, res);
        }
    }
    
    @Test
    public void shouldProperlyHandleEqualNames() throws Exception {
        final MockFolder folder = rule.createFolder("FOO");
        final FreeStyleProject prj = folder.createProject(FreeStyleProject.class, "FOO");
        assertsPath("/FOO/FOO", null, prj);
        assertsPath("FOO/FOO", null, prj);
        
        // Addressing starts from the folder
        assertsPath("FOO", folder, prj);
        assertsPath("./FOO", folder, prj);
        assertsPath(".", folder, folder);
        assertsPath("../FOO", folder, folder);
        assertsPath("../FOO/FOO", folder, prj);
        
        // Addressing starts from the parent folder
        assertsPath("FOO", prj, prj);
        assertsPath("./FOO", prj, prj);
        assertsPath(".", prj, folder);  
        assertsPath("../FOO", prj, folder);
        assertsPath("../FOO/FOO", prj, prj);
    }
    
    @Test
    public void shouldCorrectlyResolveItemsWithEqualNames() throws Exception {
        
        // FOO exists on both top level and within the folder
        final MockFolder folder = rule.createFolder("F");
        final FreeStyleProject prjInRoot = rule.createFreeStyleProject("FOO");
        final FreeStyleProject prjInFolder = folder.createProject(FreeStyleProject.class, "FOO");
        
        // Additional spot-checks for the absolute addressing
        assertsPath("F/FOO", null, prjInFolder);
        assertsPath("/F/FOO", null, prjInFolder);
        assertsPath("/FOO", null, prjInRoot);
    }
    
    private <T extends Item> void assertsPath(@NonNull String path, @CheckForNull Item base, 
            @NonNull T expected) throws Exception {
        assertsPath(path, base, expected, expected.getClass());
    }
    
    private <T extends Item> void assertsPathIsNull(@NonNull String path, @CheckForNull Item base, 
            @NonNull Class<T> clazz) throws Exception {
        assertsPath(path, base, null, clazz);
    }
    
    @TestExtension("shouldRetainTheLegacyBehaviorIfEnabled")
    public static class TestResolverManager extends ItemPathResolver.ResolverManager {

        @Override
        public Boolean isEnableLegacyItemPathResolutionMode() {
            return true;
        }      
    }
}
