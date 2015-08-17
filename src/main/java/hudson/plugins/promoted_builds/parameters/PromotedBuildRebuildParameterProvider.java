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
package hudson.plugins.promoted_builds.parameters;

import com.sonyericsson.rebuild.RebuildParameterPage;
import com.sonyericsson.rebuild.RebuildParameterProvider;
import hudson.Extension;
import hudson.model.ParameterValue;

/**
 * @author Mads Mohr Christensen
 */
@Extension(optional = true)
public class PromotedBuildRebuildParameterProvider extends RebuildParameterProvider {

    /**
     * Provide a view for specified {@link PromotedBuildParameterValue}.
     * <p/>
     * Return null if cannot handle specified {@link PromotedBuildParameterValue}.
     *
     * @param value a value to be shown in a rebuild page.
     * @return page for the parameter value. null for parameter values cannot be handled.
     */
    @Override
    public RebuildParameterPage getRebuildPage(ParameterValue value) {
        if (!(value instanceof PromotedBuildParameterValue)) {
            return null;
        }
        return new RebuildParameterPage(PromotedBuildParameterValue.class, "value.jelly");
    }
}
