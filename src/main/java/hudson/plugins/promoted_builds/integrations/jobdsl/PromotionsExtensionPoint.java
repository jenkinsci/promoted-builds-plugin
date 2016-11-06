package hudson.plugins.promoted_builds.integrations.jobdsl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Function;
import com.thoughtworks.xstream.XStream;

import hudson.Extension;
import hudson.model.AbstractItem;
import hudson.model.Item;
import hudson.model.Items;
import hudson.model.Descriptor.FormException;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.util.IOUtils;
import hudson.util.XStream2;
import javaposse.jobdsl.dsl.helpers.properties.PropertiesContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslEnvironment;
import javaposse.jobdsl.plugin.DslExtensionMethod;
import org.xml.sax.SAXException;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

import static com.google.common.collect.Lists.transform;

/**
 * The Job DSL Extension Point for the Promotions. 
 * See also <a href="https://github.com/jenkinsci/job-dsl-plugin/wiki/Extending-the-DSL">Extending the DSL</a>
 * 
 * @author Dennis Schulte
 */
@Extension(optional=true)
public class PromotionsExtensionPoint extends ContextExtensionPoint {

    private static final Logger LOGGER = Logger.getLogger(PromotionsExtensionPoint.class.getName());

    /** key to store List&lt;PromotionContext&gt; in {@link DslEnvironment} */
    private static final String PROMOTION_PROCESSES = PromotionsExtensionPoint.class.getName() + ".promotionProcesses";

    /** Note: this function does not handle null input */
    private static final Function<PromotionContext, String> PROMOTION_CONTEXT_NAME_EXTRACTOR = new Function<PromotionContext, String>() {
        @Override
        public String apply(@Nullable PromotionContext input) {
            if (input != null) {
                return input.name;
            }
            // throw NPE as documented by Function#apply(Object)
            throw new NullPointerException("Unexpected null element");
        }
    };

    @DslExtensionMethod(context = PropertiesContext.class)
    public Object promotions(Runnable closure, DslEnvironment dslEnvironment) throws FormException, IOException {
        PromotionsContext context = dslEnvironment.createContext(PromotionsContext.class);

        executeInContext(closure, context);

        final List<PromotionContext> promotionContexts = context.promotionContexts;
        JobPropertyImpl jobProperty = new JobPropertyImpl(getPromotionNames(promotionContexts));
        dslEnvironment.put(PROMOTION_PROCESSES, promotionContexts);
        return jobProperty;
    }

    @Override
    public void notifyItemCreated(Item item, DslEnvironment dslEnvironment) {
        notifyItemCreated(item, dslEnvironment, false);
    }

    public void notifyItemCreated(Item item, DslEnvironment dslEnvironment, boolean update) {
        LOGGER.log(Level.INFO, String.format("Creating promotions for %s", item.getName()));
        @SuppressWarnings("unchecked")
        Collection<PromotionContext>  promotionProcesses = (List<PromotionContext>) dslEnvironment.get(PROMOTION_PROCESSES);
        if (promotionProcesses != null && promotionProcesses.size() > 0) {
            for (PromotionContext promotionProcess : promotionProcesses) {
                final String name = PROMOTION_CONTEXT_NAME_EXTRACTOR.apply(promotionProcess);
                File dir = new File(item.getRootDir(), "promotions/" + name);
                File configXml = Items.getConfigFile(dir).getFile();
                boolean created = configXml.getParentFile().mkdirs();
                String createUpdate;
                if (created) {
                    createUpdate = "Added";
                } else {
                    createUpdate = "Updated";
                }
                try {
                    String xml = promotionProcess.getXml();
                    InputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
                    IOUtils.copy(in, configXml);
                    LOGGER.log(Level.INFO, String.format("%s promotion with name %s for %s", createUpdate, name, item.getName()));
                    update = true;
                } catch (ParserConfigurationException | SAXException | IOException e) {
                    throw new IllegalStateException("Error handling extension code", e);
                }
            }
        }

        // Only update if a promotion was actually added, updated, or removed.
        if (update) {
            try {
                LOGGER.log(Level.INFO, String.format("Reloading config for %s", item.getName()));
                ((AbstractItem) item).doReload();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to cast item to AbstractItem and reload config", e);
            }
        }
    }

    @Override
    public void notifyItemUpdated(Item item, DslEnvironment dslEnvironment) {
        LOGGER.log(Level.INFO, String.format("Updating promotions for %s", item.getName()));
        @SuppressWarnings("unchecked")
        Set<String> newPromotions = getPromotionNames((List<PromotionContext>) dslEnvironment.get(PROMOTION_PROCESSES));
        File dir = new File(item.getRootDir(), "promotions/");
        boolean update = false;
        // Delete removed promotions
        if (newPromotions != null) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File promotion : files) {
                    if (!newPromotions.contains(promotion.getName())) {
                        boolean deleted = promotion.delete();
                        if(deleted){
                            LOGGER.log(Level.INFO, String.format("Deleted promotion with name %s for %s", promotion.getName(), item.getName()));
                            update = true;
                        }
                    }
                }
            }
        }

        // Delegate to create-method
        this.notifyItemCreated(item, dslEnvironment, update);
    }

    private Set<String> getPromotionNames(final List<PromotionContext> contexts) {
        return new HashSet<>(transform(contexts, PROMOTION_CONTEXT_NAME_EXTRACTOR));
    }

}
