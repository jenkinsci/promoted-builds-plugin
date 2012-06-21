package hudson.plugins.promoted_builds;

import hudson.model.ItemGroup;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * We use this {@link ItemGroup} to create a dangling {@link PromotionProcess}.
 *
 * @author Kohsuke Kawaguchi
 */
class FakeParent implements ItemGroup {
    private final File rootDir;

    public FakeParent(File rootDir) {
        this.rootDir = rootDir;
    }

    public String getFullName() {
        return null;
    }

    public String getFullDisplayName() {
        return null;
    }

    public Collection getItems() {
        return null;
    }

    public String getUrl() {
        return null;
    }

    public String getUrlChildPrefix() {
        return null;
    }

    public hudson.model.Item getItem(String name) {
        return null;
    }

    public File getRootDirFor(hudson.model.Item child) {
        return rootDir;
    }

    public void onRenamed(hudson.model.Item item, String oldName, String newName) throws IOException {
    }

    public void onDeleted(hudson.model.Item item) throws IOException {
    }

    public String getDisplayName() {
        return null;
    }

    public File getRootDir() {
        return rootDir;
    }

    public void save() throws IOException {
    }
}
