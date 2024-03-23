package hudson.plugins.promoted_builds.pipeline;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.BuildBadgeAction;
import hudson.model.Run;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.Status;
import hudson.util.CopyOnWriteList;
import org.kohsuke.stapler.export.Exported;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class PipelinePromotedBuildAction implements BuildBadgeAction {

    public final Run<?,?> owner;

    private final CopyOnWriteList<Status> statuses = new CopyOnWriteList<Status>();

    public PipelinePromotedBuildAction(Run<?,?>owner){
        assert owner != null;
        this.owner = owner;
    }

    public PipelinePromotedBuildAction(Run<?,?> owner, Status firstStatus){
        this(owner);
        statuses.add(firstStatus);
    }

    public Run<?,?> getOwner(){

        return owner;
    }
    // What do i do with this: getProject() ?? cannot convert it to getParent??
    public Run<?,?> getProject(){

        return owner.getProject();
    }

    // Gets resolved after Status is refactored
    public boolean contains(PipelinePromotionProcess process){
        for(Status s : statuses)
            if(s.isFor(process))
                return true;
        return false;
    }
    public boolean contains(String name){
        for(Status s : statuses)
                if(s.name.equals(name));
                    return true;
        return false;
    }

    public synchronized boolean add(Status status) throws IOException {
        for(Status s: statuses)
            if(s.name.equals(status.name))
                return false;

        this.statuses.add(status);
        status.parent = this;
        owner.save();
        return true;
    }

    @Exported
    public List<Status> getPromotions(){
     return statuses.getView();
    }

    public List<Promotion> getPromotionBuilds(PromotionProcess promotionProcess) {
        List<Promotion> filtered = new ArrayList<Promotion>();

        for(Status s: getPromotions() ){
            if( s.isFor(promotionProcess)){
                filtered.addAll( s.getPromotionBuilds() );
            }
        }
        return filtered;
    }


    /**
     * Finds the {@link Status} that has matching {@link Status#name} value.
     * Or {@code null} if not found.
     */
    @CheckForNull
    public Status getPromotion(String name) {
        for (Status s : statuses)
            if(s.name.equals(name))
                return s;
        return null;
    }

    public boolean hasPromotion() {
        return !statuses.isEmpty();
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
