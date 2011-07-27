package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;

import java.util.Comparator;


/**
 * this Comparator orders a Build collection based on their last promotion date.
 * 
 * @author gerard_hcp
 *
 */
public class BuildPromotionComparator implements Comparator<AbstractBuild<?,?>> {

	public int compare(AbstractBuild<?,?> o1, AbstractBuild<?,?> o2) {
		PromotedBuildAction a1 = o1.getAction(PromotedBuildAction.class);
		PromotedBuildAction a2 = o2.getAction(PromotedBuildAction.class);
		
		if( a1 == null || a2 == null) return 0;
		
		return a2.getLastPromotion().getId().compareTo(a1.getLastPromotion().getId());
	}

}
