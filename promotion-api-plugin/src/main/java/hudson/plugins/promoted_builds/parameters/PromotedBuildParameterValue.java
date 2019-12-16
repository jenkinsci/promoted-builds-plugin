/*
 * The MIT License
 *
 * Copyright (c) 2103, Peter Hayes
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
package hudson.plugins.promoted_builds.parameters;

import hudson.model.RunParameterValue;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Represents an instance of a parameter that relates to a specific build that
 * met the promotion critera.
 * 
 * @author Pete Hayes
 */
public class PromotedBuildParameterValue extends RunParameterValue {
    private String promotionProcessName;

    @DataBoundConstructor
    public PromotedBuildParameterValue(String name, String runId, String description) {
        super(name, runId, description);
    }

    public PromotedBuildParameterValue(String name, String runId) {
        super(name, runId);
    }

    public String getPromotionProcessName() {
        return promotionProcessName;
    }

    public void setPromotionProcessName(String promotionProcessName) {
        this.promotionProcessName = promotionProcessName;
    }

    @Override
    public String getShortDescription() {
    	return "(PromotedBuildParameterValue) " + getName() + "='" + getRunId() + "'";
    }
}
