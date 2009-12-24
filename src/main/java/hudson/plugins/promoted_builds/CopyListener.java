package hudson.plugins.promoted_builds;

import hudson.Extension;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.listeners.ItemListener;
import java.io.File;
import java.io.FileFilter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * When a job is copied, copy over promotion definitions as well.
 *
 * @author Alan.Harder@Sun.com
 */
@Extension
public class CopyListener extends ItemListener {

    /**
     * Copy promotion definitions from existing job.
     */
    @Override
    public void onCopied(Item src, Item item) {
        JobPropertyImpl prop;
        if (src instanceof Job && (prop =
                (JobPropertyImpl)((Job)src).getProperty(JobPropertyImpl.class)) != null) {
            File[] subdirs = ((JobPropertyImpl)prop).getRootDir().listFiles(new FileFilter() {
                public boolean accept(File child) {
                    return child.isDirectory();
                }
            });
            if (subdirs != null) {
                prop = (JobPropertyImpl)((Job)item).getProperty(JobPropertyImpl.class);
                for (File subdir : subdirs) try {
                    Util.copyFile(new File(subdir, "config.xml"),
                                  new File(prop.getRootDirFor(subdir.getName()), "config.xml"));
                } catch (Exception e) {
                    Logger.getLogger(CopyListener.class.getName()).log(Level.WARNING,
                        "Failed to copy/load promotion " + subdir + " into new job", e);
                }
                // Trigger loading of these files
                prop.setOwner(prop.getOwner());
            }
        }
    }
}
