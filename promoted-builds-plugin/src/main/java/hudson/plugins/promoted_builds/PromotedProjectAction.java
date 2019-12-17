package hudson.plugins.promoted_builds;

import hudson.model.*;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * For customizing project top-level GUI.
 * @author Kohsuke Kawaguchi
 */
@ExportedBean
public class PromotedProjectAction implements ProminentProjectAction, PermalinkProjectAction {
	
	//TODO externalize to a plugin property?
	private static final int SUMMARY_SIZE = 10;
	
	public final AbstractProject<?,?> owner;
    private final JobPropertyImpl property;

    public PromotedProjectAction(AbstractProject<?, ?> owner, JobPropertyImpl property) {
        this.owner = owner;
        this.property = property;
    }

    public Api getApi() {
        return new Api(this);
    }

    @Exported
    public List<PromotionProcess> getProcesses() {
        return property.getActiveItems();
    }
    
    /**
     * Get the promotion process by name.
     * @param name Name of the process
     * @return Discovered process or {@code null} if it cannot be found
     */
    @CheckForNull
    public PromotionProcess getProcess(String name) {
    	for (PromotionProcess pp : getProcesses() ){
    		if(pp.getName().equals(name))
    			return pp;
    	}
        return null;
    }

    public AbstractBuild<?,?> getLatest(PromotionProcess p) {
    	List<Promotion> list = getPromotions( p );
    	Collections.sort(list);
        return list.size() > 0 ? list.get(list.size() - 1) : null;
    }

    @Restricted(NoExternalUse.class)
    public List<PromotionProcess> getPromotionProcesses() {
        List<PromotionProcess> processes = null;
        processes = getProcesses();
        if (processes == null) {
            // assert ?
            // this case should not happen, the action should get deleted
            // when there is no process; but we're now safe for the UI.
            processes = new ArrayList<PromotionProcess>();
        }
        return processes;
    }

    @Restricted(NoExternalUse.class)
    public Status getStatus(PromotionProcess process) {
        List<Promotion> list = getPromotions( process );
        Promotion latest = list.size() > 0 ? list.get(list.size() - 1) : null;
        Status status = latest != null ? latest.getStatus() : null;
        return status;
    }

    /**
     * Finds the last promoted build under the given criteria.
     */
    public AbstractBuild<?,?> getLatest(String name) {
    	List<Promotion> list = getPromotions( getProcess(name) );
        return list.size() > 0 ? list.get(list.size() - 1) : null;
    }


    public List<Promotion> getPromotions(PromotionProcess promotionProcess){
    	List<Promotion> list = new ArrayList<Promotion>(); 
        for( AbstractBuild<?,?> build : owner.getBuilds() ) {
            PromotedBuildAction a = build.getAction(PromotedBuildAction.class);
            if(a!=null && a.contains(promotionProcess))
            	list.addAll( a.getPromotionBuilds(promotionProcess) );
        }

        Collections.sort(list, Collections.<Promotion>reverseOrder());

        return list;
    }
    
    /**
     * returns the summary of the latest promotions for a promotion process.
     * 
     * @param promotionProcess Name of the promotion process
     * @return List of latest promotions
     */
    public List<Promotion> getPromotionsSummary(PromotionProcess promotionProcess){
    	List<Promotion> promotionList = this.getPromotions(promotionProcess);
    	if(promotionList.size() > SUMMARY_SIZE ){
	    return promotionList.subList(promotionList.size() - SUMMARY_SIZE, promotionList.size());
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

    @RequirePOST
    public HttpResponse doCreateProcess(@QueryParameter String name, StaplerRequest req) throws IOException {
        property.createProcessFromXml(name, req.getInputStream());
        return HttpResponses.ok();
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
