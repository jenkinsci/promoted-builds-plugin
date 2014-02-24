package hudson.plugins.promoted_builds;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.AbstractItem;
import hudson.model.Action;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.ParameterValue;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.plugins.promoted_builds.conditions.ManualCondition;
import hudson.util.DescribableList;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import jenkins.model.Jenkins;
import org.junit.Test;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;
import static org.junit.Assert.*;

/**
 * @author Jonathan Zimmerman
 */

public class PromotionEnvironmentVariablesTest {
    
    @Rule public JenkinsRule r = new JenkinsRule();
    
    @Test
    public void shouldSetJobAndJobFullNames() throws Descriptor.FormException, IOException, InterruptedException, ExecutionException {
        // Assemble
        MockFolder parent = new MockFolder(r.jenkins, "Folder");
        parent.save();
        FreeStyleProject project = new FreeStyleProject(parent, "Project");
        project.save();
        
        JobPropertyImpl promotionProperty = new JobPropertyImpl(project);
        PromotionProcess promotionProcess = new PromotionProcess(promotionProperty, "promo");
        promotionProcess.conditions.clear();
        promotionProcess.conditions.add(new ManualCondition());
        Action approvalAction = new ManualCondition.ManualApproval(promotionProcess.getName(), new ArrayList<ParameterValue>());
        
        // Act
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        build.addAction(approvalAction);
        build.save();
        
        Promotion promotion = promotionProcess.considerPromotion2(build).get();
        EnvVars env = promotion.getEnvironment(TaskListener.NULL);
        
        String rootUrl = r.jenkins.getRootUrl();
        
        // Assert
        assertEquals("Folder/Project", env.get("PROMOTED_JOB_FULL_NAME"));
        assertEquals("Project", env.get("PROMOTED_JOB_NAME"));
	assertEquals("anonymous", env.get("PROMOTED_USER_NAME"));       
 
        project.delete();
        parent.delete();
    }
    
    private static class MockFolder extends AbstractItem implements TopLevelItem, ItemGroup<TopLevelItem> {

        private DescribableList<TopLevelItem, TopLevelItemDescriptor> items;
        
        public MockFolder(ItemGroup parent, String name) {
            super(parent, name);
            
            if (items == null) {
                items = new DescribableList(this);
            }
        }

        public TopLevelItemDescriptor getDescriptor() {
            return Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class);
        }

        public Collection<TopLevelItem> getItems() {
            return items.toList();
        }

        public String getUrlChildPrefix() {
            return "job";
        }

        public TopLevelItem getItem(String name) {
            for (TopLevelItem item : getItems()) {
                if (item.getName().equals(name)) {
                    return item;
                }
            }
            
            return null;
        }

        public File getRootDirFor(TopLevelItem child) {
            return new File(getParent().getRootDir(), name + "/jobs/" + child.getName());
        }

        public void onRenamed(TopLevelItem item, String oldName, String newName) throws IOException {
        }

        public void onDeleted(TopLevelItem item) throws IOException {
        }
     
        @Override
        public Collection<? extends Job> getAllJobs() {
            List<Job> jobs = Collections.emptyList();
            
            for (TopLevelItem item : getItems()) {
                if (item instanceof Job) {
                    jobs.add((Job)item);
                }
            }
            
            return jobs;
        }
        
        @Extension
        public static final class DescriptorImpl extends TopLevelItemDescriptor {

            @Override
            public String getDisplayName() {
                return "";
            }

            @Override
            public TopLevelItem newInstance(ItemGroup parent, String name) {
                return new MockFolder(parent, name);
            }
        }
    }
}
