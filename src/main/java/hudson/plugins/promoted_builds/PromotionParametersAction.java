package hudson.plugins.promoted_builds;

import java.util.List;

import hudson.model.ParameterValue;
import hudson.model.ParametersAction;

public class PromotionParametersAction extends ParametersAction {

	public PromotionParametersAction(List<ParameterValue> parameters) {
		super(parameters);
	}

	public PromotionParametersAction(ParameterValue... parameters) {
		super(parameters);
	}

}
