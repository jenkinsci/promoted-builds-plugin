package hudson.plugins.promoted_builds;

import hudson.model.ProminentProjectAction;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.PermalinkProjectAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * For customizing project top-level GUI.
 * @author Kohsuke Kawaguchi
 */
public class PromotedProjectAction implements ProminentProjectAction, PermalinkProjectAction {
	
	//TODO externalize to a plugin property?
	private static final int SUMMARY_SIZE = 10;
	
	public final AbstractProject<?,?> owner;
    private final JobPropertyImpl property;

    public PromotedProjectAction(AbstractProject<?, ?> owner, JobPropertyImpl property) {
        this.owner = owner;
        this.property = property;
    }

    public List<PromotionProcess> getProcesses() {
        return property.getActiveItems();
    }

    public AbstractBuild<?,?> getLatest(PromotionProcess p) {
        return getLatest(p.getName());
    }

    /**
     * Finds the last promoted build under the given criteria.
     */
    public AbstractBuild<?,?> getLatest(String name) {
    	List<AbstractBuild<?,?>> list = getPromotions(name);
        return list.size() > 0 ? list.get(0) : null;
    }


    public List<AbstractBuild<?,?>> getPromotions(String name){
    	List<AbstractBuild<?,?>> list = new ArrayList<AbstractBuild<?,?>>(); 
        for( AbstractBuild<?,?> build : owner.getBuilds() ) {
            PromotedBuildAction a = build.getAction(PromotedBuildAction.class);
            if(a!=null && a.contains(name))
            	list.add(build);
        }
        Collections.sort(list, new BuildPromotionComparator() );
    	return list;
    }
    
    /**
     * returns the summary of the latest promotions for a promotion process.
     * 
     * @param promotionProcessName
     * @return
     */
    public List<AbstractBuild<?,?>> getPromotionsSummary(String promotionProcessName){
    	List<AbstractBuild<?,?>> promotionList = this.getPromotions(promotionProcessName);
    	if(promotionList.size() > SUMMARY_SIZE ){
    		return promotionList.subList(0, SUMMARY_SIZE);
    	}else{
    		return promotionList;
    	}
    }
    
    
    public List<Permalink> getPermalinks() {
        List<Permalink> r = new ArrayList<Permalink>();
        for (PromotionProcess pp : property.getActiveItems())
            r.add(pp.asPermalink());
        return r;
    }

    public String getIconFileName() {
        return "star.gif";
    }

    public String getDisplayName() {
        return "Promotion Status";
    }

    public String getUrlName() {
        return "promotion";
    }
}
