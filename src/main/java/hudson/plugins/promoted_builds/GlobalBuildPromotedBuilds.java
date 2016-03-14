package hudson.plugins.promoted_builds;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Global Jenkins configuration for Promoted Builds
 *
 * @since 2.26
 */

@Extension
public class GlobalBuildPromotedBuilds extends GlobalConfiguration {

    /**
     * By default ISO 8601 like 2016-10-12T09:30Z to be used with
     * environmental variable $PROMOTED_TIMESTAMP.
     */
    private String dateFormat;

    /**
     * By default Java timezone setting to be used with environmental
     * variable $PROMOTED_TIMESTAMP.
     *
     * Other time zones can be selected if field is filled.
     */
    private String timeZone;

    public GlobalBuildPromotedBuilds() {
        load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public static GlobalBuildPromotedBuilds get() {
        return GlobalBuildPromotedBuilds.all().get(GlobalBuildPromotedBuilds.class);
    }
}
