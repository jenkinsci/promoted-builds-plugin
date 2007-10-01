package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.model.AbstractProject;
import hudson.model.listeners.RunListener;

/**
 * {@link RunListener} to pick up completions of downstream builds.
 *
 * <p>
 * This is a single instance that receives all the events everywhere in the system.
 * @author Kohsuke Kawaguchi
 */
public class RunListenerImpl extends RunListener<AbstractBuild<?,?>> {
    public RunListenerImpl() {
        super((Class)AbstractBuild.class);
    }

    @Override
    public void onCompleted(AbstractBuild<?,?> build, TaskListener listener) {
        // this is not terribly efficient, 
        for(AbstractProject<?,?> j : Hudson.getInstance().getAllItems(AbstractProject.class)) {
            JobPropertyImpl p = j.getProperty(JobPropertyImpl.class);
            if(p!=null)
                p.onCompleted(build,listener);
        }
    }
}
