package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.PermalinkProjectAction;
import hudson.model.ProminentProjectAction;

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
    
    public PromotionProcess getProcess(String name) {
    	for (PromotionProcess pp : getProcesses() ){
    		if(pp.getName().equals(name))
    			return pp;
    	}
        return null;
    }

    public AbstractBuild<?,?> getLatest(PromotionProcess p) {
    	List<Promotion> list = getPromotions( p );
        return list.size() > 0 ? list.get(0) : null;
    }

    /**
     * Finds the last promoted build under the given criteria.
     */
    public AbstractBuild<?,?> getLatest(String name) {
    	List<Promotion> list = getPromotions( getProcess(name) );
        return list.size() > 0 ? list.get(0) : null;
    }


    public List<Promotion> getPromotions(PromotionProcess promotionProcess){
    	List<Promotion> list = new ArrayList<Promotion>(); 
        for( AbstractBuild<?,?> build : owner.getBuilds() ) {
            PromotedBuildAction a = build.getAction(PromotedBuildAction.class);
            if(a!=null && a.contains(promotionProcess))
            	list.addAll( a.getPromotionBuilds(promotionProcess) );
        }
        Collections.sort(list);
        return list;
    }
    
    /**
     * returns the summary of the latest promotions for a promotion process.
     * 
     * @param promotionProcessName
     * @return
     */
    public List<Promotion> getPromotionsSummary(PromotionProcess promotionProcess){
    	List<Promotion> promotionList = this.getPromotions(promotionProcess);
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
        return "star.png";
    }

    public String getDisplayName() {
        return "Promotion Status";
    }

    public String getUrlName() {
        return "promotion";
    }
}
