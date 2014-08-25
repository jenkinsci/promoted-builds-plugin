/*
 * The MIT License
 *
 * Copyright (c) 2011, cjo9900 <cjo.johnson@gmail.com>
 * Copyright (c) 2010, alanharder <mindless@dev.java.net>
 * Copyright (c) 2009, huybrechts <huybrechts@dev.java.net>
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

import hudson.model.Result;

public enum ResultCondition {

    SUCCESS("Stable") {
        boolean isMet(Result result) {
            return result == Result.SUCCESS;
        }
    },
    UNSTABLE("Unstable") {
        boolean isMet(Result result) {
            return result == Result.UNSTABLE;
        }
    },
    UNSTABLE_OR_BETTER("Stable or unstable but not failed") {
        boolean isMet(Result result) {
            return result.isBetterOrEqualTo(Result.UNSTABLE);
        }
    },
    UNSTABLE_OR_WORSE("Unstable or failed but not stable") {
        boolean isMet(Result result) {
            return result.isWorseOrEqualTo(Result.UNSTABLE);
        }
    },
    FAILED("Failed") {
        boolean isMet(Result result) {
            return result == Result.FAILURE;
        }
    },
    ALWAYS("Complete (always trigger)") {
        boolean isMet(Result result) {
            return true;
        }
    };

    private ResultCondition(String displayName) {
        this.displayName = displayName;
    }

    private final String displayName;

    public String getDisplayName() {
        return displayName;
    }

    abstract boolean isMet(Result result);

}
