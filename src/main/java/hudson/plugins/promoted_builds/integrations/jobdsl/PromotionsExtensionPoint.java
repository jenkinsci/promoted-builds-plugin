package hudson.plugins.promoted_builds.integrations.jobdsl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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

/**
 * The Job DSL Extension Point for the Promotions. See also {@linktourl https://github.com/jenkinsci/job-dsl-plugin/wiki/Extending-the-DSL}
 *
 * @author Dennis Schulte
 */
@Extension
public class PromotionsExtensionPoint extends ContextExtensionPoint {

    private static final Logger LOGGER = Logger.getLogger(PromotionsExtensionPoint.class.getName());

    private static final XStream XSTREAM = new XStream2();

    @DslExtensionMethod(context = PropertiesContext.class)
    public Object promotions(Runnable closure, DslEnvironment dslEnvironment) throws FormException, IOException {
        PromotionsContext context = new PromotionsContext();
        executeInContext(closure, context);
        dslEnvironment.put("processNames", context.names);
        JobPropertyImpl jobProperty = new JobPropertyImpl(context.names);
        Map<String,JobDslPromotionProcess> promotionProcesses = new HashMap<String,JobDslPromotionProcess>();
        for (String processName : context.names) {
            PromotionContext promotionContext = context.promotionContexts.get(processName);
            JobDslPromotionProcess jobDslPromotionProcess = new JobDslPromotionProcess();
            jobDslPromotionProcess.setName(processName);
            jobDslPromotionProcess.setIcon(promotionContext.getIcon());
            jobDslPromotionProcess.setAssignedLabel(promotionContext.getRestrict());
            jobDslPromotionProcess.setBuildSteps(promotionContext.getActions());
            jobDslPromotionProcess.setConditions(promotionContext.getConditions());
            promotionProcesses.put(processName,jobDslPromotionProcess);
        }
        dslEnvironment.put("promotionProcesses", promotionProcesses);
        return jobProperty;
    }

    @Override
    public void notifyItemCreated(Item item, DslEnvironment dslEnvironment) {
        notifyItemCreated(item, dslEnvironment, false);
    }

    @SuppressWarnings("unchecked")
    public void notifyItemCreated(Item item, DslEnvironment dslEnvironment, boolean update) {
        LOGGER.log(Level.INFO, String.format("Creating promotions for %s", item.getName()));
        Map<String,JobDslPromotionProcess>  promotionProcesses = (Map<String,JobDslPromotionProcess>) dslEnvironment.get("promotionProcesses");
        Set<String> names = (Set<String>) dslEnvironment.get("processNames");
        if (names != null && names.size() > 0) {
            for (String name : names) {
                JobDslPromotionProcess promotionProcess = promotionProcesses.get(name);
                XSTREAM.registerConverter(new JobDslPromotionProcessConverter(XSTREAM.getMapper(), XSTREAM.getReflectionProvider()));
                XSTREAM.registerConverter(new ManualConditionConverter(XSTREAM.getMapper(), XSTREAM.getReflectionProvider()));
                String xml = XSTREAM.toXML(promotionProcess);
                File dir = new File(item.getRootDir(), "promotions/" + name);
                File configXml = Items.getConfigFile(dir).getFile();
                boolean created = configXml.getParentFile().mkdirs();
                String createUpdate;
                if(created){
                    createUpdate = "Added";
                }else{
                    createUpdate = "Updated";
                }
                try {
                    InputStream in = new ByteArrayInputStream(xml.getBytes("UTF-8"));
                    IOUtils.copy(in, configXml);
                    LOGGER.log(Level.INFO, String.format(createUpdate + " promotion with name %s for %s", name, item.getName()));
                    update = true;
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException("Error handling extension code", e);
                } catch (IOException e) {
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
        Set<String> newPromotions = (Set<String>) dslEnvironment.get("processNames");
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

}
