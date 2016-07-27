package hudson.plugins.promoted_builds.util;

import hudson.model.listeners.ItemListener;

/**
 * Utility class to call ArtifactArchiver.Migrator ItemListener to make this code compatible with jenkins 1.575+
 */
public class ItemListenerHelper {

    /**
     * Executes all available ItemListeners
     */
    public static void fireItemListeners() {
        for (ItemListener l : ItemListener.all()) {
            l.onLoaded();
        }
    }
}
