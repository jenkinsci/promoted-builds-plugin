package hudson.plugins.promoted_builds;

import hudson.Extension;
import hudson.model.ItemGroup;
import org.jenkinsci.plugins.configfiles.ConfigContextResolver;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Extension(optional = true)
@Restricted(NoExternalUse.class)
public class JobPropertyImplConfigContextResolver extends ConfigContextResolver {

    @Override
    public ItemGroup getConfigContext(ItemGroup itemGroup) {
        if (itemGroup instanceof JobPropertyImpl) {
            return JobPropertyImpl.class.cast(itemGroup).getOwner().getParent();
        }
        return null;
    }
}
