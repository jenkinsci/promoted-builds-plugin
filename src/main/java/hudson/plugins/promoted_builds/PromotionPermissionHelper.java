/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
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

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.promoted_builds.conditions.ManualCondition;
import hudson.security.AccessDeniedException2;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Used to ensure consistent permission checks for approvals of {@link ManualCondition}, re-executions of 
 * {@link Status}, and force promotions of {@link PromotionProcess}.
 */
@Restricted(NoExternalUse.class)
public class PromotionPermissionHelper {

    public static void checkPermission(@Nonnull AbstractProject<?,?> target, @Nonnull AbstractBuild<?,?> build, @CheckForNull ManualCondition associatedCondition) {
        if (!hasPermission(target, build, associatedCondition)) {
            // TODO: Give a more accurate error message if the user has Promotion.PROMOTE but is not in the list of approvers.
            throw new AccessDeniedException2(Jenkins.getAuthentication(), Promotion.PROMOTE);
        }
    }

    public static boolean hasPermission(@Nonnull AbstractProject<?,?> target, @Nonnull AbstractBuild<?,?> build, @CheckForNull ManualCondition associatedCondition) {
        if (associatedCondition == null) {
            return target.hasPermission(Promotion.PROMOTE);
        } else if (associatedCondition.getUsersAsSet(build).isEmpty()) {
            return target.hasPermission(Promotion.PROMOTE);
        } else if (associatedCondition.isInUsersList(build) || associatedCondition.isInGroupList(build)) {
            // Explicitly listed users do not need Promotion/Promote permissions.
            return true;
        } else {
            // Administrators can promote even if they are not included in the list of manual approvers.
            return target.hasPermission(Jenkins.ADMINISTER);
        }
    }
}
