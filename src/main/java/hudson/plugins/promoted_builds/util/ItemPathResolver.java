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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.ExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.plugins.promoted_builds.parameters.PromotedBuildParameterDefinition;
import java.util.StringTokenizer;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

//TODO: Replace by methods in the Jenkins core when they become available
/**
 * Implements an engine, which allows to resolve {@link Item}s by their paths.
 * The engine supports both relative and absolute addressing.
 * @author Oleg Nenashev
 * @since 2.22
 */
public class ItemPathResolver {
       
    /**
     * Optional configuration, which enables the legacy behavior in 
     * {@link #getByPath(java.lang.String, hudson.model.Item, java.lang.Class)}
     */
    @Restricted(NoExternalUse.class)
    private static boolean ENABLE_LEGACY_RESOLUTION_AGAINST_ROOT =
            Boolean.getBoolean(ItemPathResolver.class+".enableResolutionAgainstRoot");
    
    /**
     * Check if the legacy path resolution mode is enabled.
     * The resolution uses available {@link ResolverManager}s and falls back to
     * {@link #ENABLE_LEGACY_RESOLUTION_AGAINST_ROOT} if there is no decision.
     * @return True if the legacy resolution engine is enabled
     */
    public static boolean isEnableLegacyResolutionAgainstRoot() {
        final Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            ExtensionList<ResolverManager> extensions = jenkins.getExtensionList(ResolverManager.class);
            for (ResolverManager manager : extensions) {
                Boolean enableLegacyItemPathResolutionMode = manager.isEnableLegacyItemPathResolutionMode();
                if (enableLegacyItemPathResolutionMode != null) {
                    return enableLegacyItemPathResolutionMode;
                }
            }
        }
        
        return ENABLE_LEGACY_RESOLUTION_AGAINST_ROOT;
    }
    
    /**
     * Gets an {@link Item} of the specified type by absolute or relative path.
     * <p>
     * The implementation retains the original behavior in {@link PromotedBuildParameterDefinition}, 
     * but this method also provides a support of multi-level addressing including special markups
     * for the relative addressing.
     * </p>
     * Effectively, the resolution order is following:
     * <ul>
     *   <li><b>Optional</b> Legacy behavior, which can be enabled by {@link #ENABLE_LEGACY_RESOLUTION_AGAINST_ROOT}. 
     *       If an item for the name exists on the top Jenkins level, it will be returned</li>
     *   <li>If the path starts with &quot;/&quot;, a global addressing will be used</li>
     *   <li>If the path starts with &quot;./&quot; or &quot;../&quot;, a relative addressing will be used</li>
     *   <li>If there is no prefix, a relative addressing will be tried. If it
     *       fails, the method falls back to a global one</li>
     * </ul>
     * For the relative and absolute addressing the engine supports &quot;.&quot; and
     * &quot;..&quot; markers within the path.
     * The first one points to the current element, the second one - to the upper element. 
     * If the search cannot get a new top element (e.g. reached the root), the method returns {@code null}.
     * 
     * @param <T> Type of the {@link Item} to be retrieved
     * @param path Path string to the item. 
     * @param baseItem Base {@link Item} for the relative addressing. If null,
     *      this addressing approach will be skipped
     * @param type Type of the {@link Item} to be retrieved 
     * @return Found {@link Item}. Null if it has not been found by all addressing modes
     *  or the type differs.
     */
    @CheckForNull
    @SuppressWarnings("unchecked")
    @Restricted(NoExternalUse.class)
    public static <T extends Item> T getByPath(@NonNull String path, 
            @CheckForNull Item baseItem, @NonNull Class<T> type) {
        final Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return null;
        }
        
        // Legacy behavior
        if (isEnableLegacyResolutionAgainstRoot()) {
            TopLevelItem topLevelItem = jenkins.getItem(path);
            if (topLevelItem != null && type.isAssignableFrom(topLevelItem.getClass())) {
                return (T)topLevelItem;
            }
        }
        
        // Explicit global addressing
        if (path.startsWith("/")) {
            return findPath(jenkins, path.substring(1), type);
        }
        
        // Try the relative addressing if possible
        if (baseItem != null) {
            final ItemGroup<?> relativeRoot = baseItem instanceof ItemGroup<?>
                    ? (ItemGroup<?>)baseItem : baseItem.getParent();
            final T item = findPath(relativeRoot, path, type);
            if (item != null) {
                return item;
            }
        }
        
        // Fallback to the default behavior (addressing from the Jenkins root)
        return findPath(jenkins, path, type);
    }
    
    @CheckForNull
    @SuppressWarnings("unchecked")
    private static <T extends Item> T findPath(@CheckForNull ItemGroup base, 
            @NonNull String path, @NonNull Class<T> type) {
        Item item = findPath(base, path);
        if (item != null && type.isAssignableFrom(item.getClass())) {
            return (T) item;
        }
        return null;
    }
    
    @CheckForNull
    private static Item findPath(@CheckForNull ItemGroup base, @NonNull String path) {
        @CheckForNull ItemGroup<?> pointer = base;
        final StringTokenizer t = new StringTokenizer(path, "/");
        while(pointer != null && t.hasMoreTokens()) {
            String current = t.nextToken();
            if (current.equals("..")) {
                if (pointer instanceof Item) {
                    Item currentItem = (Item)pointer;
                    pointer = currentItem.getParent();
                } else {
                    pointer = null; // Cannot go upstairs
                }
            } else if (current.equals(".")) {
                // Do nothing, we stay on the same level
            } else {
                // Resolve the level beneath
                final Item item = pointer.getItem(current);
                if (!t.hasMoreTokens()) {
                    // Last token => we consider it as a required item
                    return item;
                }
                if (item instanceof ItemGroup<?>) {
                    pointer = (ItemGroup<?>) item;
                } else {
                    pointer = null; // Wrong path, we got to item before finishing the requested path
                }
            }
        }
        
        // Cannot retrieve the path => exit with null
        if (pointer instanceof Item) {
            return (Item)pointer;
        }
        return null;
    }
    
    /**
     * External manager, which allows the alter behavior on-demand.
     * Currently this {@link ExtensionPoint} is designed for the internal use only
     * @since 2.22
     */
    @Restricted(NoExternalUse.class)
    public static abstract class ResolverManager implements ExtensionPoint {
        
        /**
         * Alters the item path resolution mode.
         * @return true if the manager made a decision. null by default
         */
        @CheckForNull
        public Boolean isEnableLegacyItemPathResolutionMode() {
            return null;
        }  
    }
}
