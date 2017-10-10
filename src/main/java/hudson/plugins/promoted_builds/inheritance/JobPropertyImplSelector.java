
package hudson.plugins.promoted_builds.inheritance;

import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.JobProperty;

import hudson.plugins.project_inheritance.projects.InheritanceProject;
import hudson.plugins.project_inheritance.projects.inheritance.InheritanceSelector;

import hudson.plugins.promoted_builds.JobPropertyImpl;

/**
 *  
 * @author Jacek Tomaka
 * @since TODO
 */
@Extension(optional=true)
public class JobPropertyImplSelector extends InheritanceSelector<JobProperty<?>> {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(JobPropertyImplSelector.class.getName());

    @Override
    public boolean isApplicableFor(Class<?> clazz){
        return JobProperty.class.isAssignableFrom(clazz);
    }

    @Override
    public InheritanceSelector.MODE getModeFor(Class<?> clazz){
        if (JobPropertyImpl.class.isAssignableFrom(clazz)) return MODE.USE_LAST;
        return MODE.NOT_RESPONSIBLE;
    }

    @Override
    public String getObjectIdentifier(JobProperty<?> obj){
        if ( obj!=null && JobPropertyImpl.class.getName().equals(obj.getClass().getName())){
            return JobPropertyImplSelector.class.getName();
        }
        return null;
    }

    @Override
    public JobPropertyImpl merge(JobProperty<?> prior, JobProperty<?> latter, InheritanceProject caller){
        return null;
    }

    @Override
    public JobProperty<?> handleSingleton(JobProperty<?> jobProperty, InheritanceProject caller){
        if (jobProperty == null || caller == null) return jobProperty;
        if (caller.isAbstract) return jobProperty;

        if (!JobPropertyImpl.class.isAssignableFrom(jobProperty.getClass())) return jobProperty;
        

        JobPropertyImpl jobPropertyImpl = (JobPropertyImpl)jobProperty;

        try {
            JobPropertyImpl newJobProperty = new JobPropertyImpl(jobPropertyImpl, caller);
            return newJobProperty;
        } catch (Exception ex){
            logger.log(Level.WARNING, "Error during hacking up JobPropertyImpl", ex );
        }
        return jobProperty;
    }
}

