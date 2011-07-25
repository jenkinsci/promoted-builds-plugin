/*
 * The MIT License
 *
 * Copyright (c) 2011, CloudBees, Inc.
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

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotionBadge;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.PromotionConditionDescriptor;
import hudson.plugins.promoted_builds.PromotionProcess;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

/**
 * {@link PromotionCondition} that promotes a build as soon as it's done.
 *
 * @author Kohsuke Kawaguchi
 */
public class SelfPromotionCondition extends PromotionCondition {
	private boolean onSuccessOnly;
	
    @DataBoundConstructor
    public SelfPromotionCondition() {
    	this(false);
    }
    
    public SelfPromotionCondition(boolean onSuccessOnly) {
    	this.onSuccessOnly = onSuccessOnly;
    }

    @Override
    public PromotionBadge isMet(PromotionProcess promotionProcess, AbstractBuild<?, ?> build) {
    	if (onSuccessOnly && build.getResult() != Result.SUCCESS) {
    		return null;
    	}
        return new SelfPromotionBadge();
    }
    
    public boolean isOnSuccessOnly() {
    	return onSuccessOnly;
    }

    /**
     * {@link RunListener} to pick up completions of a build.
     *
     * <p>
     * This is a single instance that receives all the events everywhere in the system.
     * @author Kohsuke Kawaguchi
     */
    @Extension
    public static final class RunListenerImpl extends RunListener<AbstractBuild<?,?>> {
        public RunListenerImpl() {
            super((Class)AbstractBuild.class);
        }

        @Override
        public void onCompleted(AbstractBuild<?,?> build, TaskListener listener) {
            JobPropertyImpl jp = build.getProject().getProperty(JobPropertyImpl.class);
            if(jp!=null) {
                for (PromotionProcess p : jp.getItems()) {
                    for (PromotionCondition cond : p.conditions) {
                        if (cond instanceof SelfPromotionCondition) {
                            try {
                                p.considerPromotion(build);
                                break; // move on to the next process
                            } catch (IOException e) {
                                e.printStackTrace(listener.error("Failed to promote a build"));
                            }
                        }
                    }
                }
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends PromotionConditionDescriptor {
        public boolean isApplicable(AbstractProject<?,?> item) {
            return true;
        }

        public String getDisplayName() {
            return Messages.SelfPromotionCondition_DisplayName();
        }
        
        @Override
        public PromotionCondition newInstance(StaplerRequest req,
        		JSONObject formData)
        		throws hudson.model.Descriptor.FormException {
        	return new SelfPromotionCondition(formData.getBoolean("onSuccessOnly"));
        }
    }
}
