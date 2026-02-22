package org.openhab.core.model.rule.internal.fileconverter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.fileconverter.RuleParser;
import org.openhab.core.automation.fileconverter.RuleSerializer;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.rule.internal.DSLRuleProvider;
import org.openhab.core.model.rule.rules.RuleModel;
import org.openhab.core.model.rule.rules.RulesFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
@Component(immediate = true, service = { RuleSerializer.class, RuleParser.class })
public class DslRuleFileConverter implements RuleSerializer, RuleParser {

    private final Logger logger = LoggerFactory.getLogger(DslRuleFileConverter.class);

    public @Nullable ModelRepository modelRepository;

    public @Nullable DSLRuleProvider ruleProvider;
//    private final LocaleProvider localeProvider;

    private final Map<String, RuleModel> elementsToGenerate = new ConcurrentHashMap<>();

//    @Activate
//    public DslRuleFileConverter(@Reference ModelRepository modelRepository,
//            @Reference DSLRuleProvider ruleProvider) {
//        this.modelRepository = modelRepository;
//        this.ruleProvider = ruleProvider;
//    }

    @Override
    public @NonNull String getParserFormat() {
        return "DSL";
    }

    @Override
    public String getGeneratedFormat() {
        return "DSL";
    }

    @Override
    public void setRulesToBeGenerated(String id, List<Rule> rules, boolean hideDefaultParameters) {
        if (rules.isEmpty()) {
            return;
        }
        RuleModel model = RulesFactory.eINSTANCE.createRuleModel();
        Set<Rule> handledRules = new HashSet<>();
        for (Rule thing : rules) {
            if (handledRules.contains(thing)) {
                continue;
            }
            model.getRules().add(buildModelRule(thing, hideDefaultParameters, rules.size() > 1,
                    true, rules, handledRules));
        }
        elementsToGenerate.put(id, model);
    }

    @Override
    public void generateFormat(String id, OutputStream out) {
        RuleModel model = elementsToGenerate.remove(id);
        if (model != null) { //TODO: (Nad) Check everything
            // Double quotes are unexpectedly generated in thing UID when the segment contains a -.
            // Fix that by removing these double quotes. Requires to first build the generated syntax as a String
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            modelRepository.generateFileFormat(outputStream, "rules", model);
            String syntax = new String(outputStream.toByteArray()).replaceAll(":\"([a-zA-Z0-9_][a-zA-Z0-9_-]*)\"",
                    ":$1");
            try {
                out.write(syntax.getBytes());
            } catch (IOException e) {
                logger.warn("Exception when writing the generated syntax {}", e.getMessage());
            }
        }
    }

    @Override
    public @Nullable String startParsingFormat(String syntax, List<String> errors, List<String> warnings) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(syntax.getBytes());
        return modelRepository.createIsolatedModel("rules", inputStream, errors, warnings);
    }

    @Override
    public @NonNull Collection<Rule> getParsedObjects(String modelName) {
        return List.of(); // TODO: (Nad) Temp
//        return ruleProvider.getAllFromModel(modelName);
    }

    @Override
    public void finishParsingFormat(String modelName) {
        modelRepository.removeModel(modelName);
    }

    private org.openhab.core.model.rule.rules.Rule buildModelRule(Rule rule, boolean hideDefaultParameters,
            boolean preferPresentationAsTree, boolean topLevel, List<Rule> onlyThings, Set<Rule> handledRules) {
        org.openhab.core.model.rule.rules.Rule model;
        model = RulesFactory.eINSTANCE.createRule();
        if (!preferPresentationAsTree || topLevel) {
//            model.setId(rule.getUID().getAsString());
        } else {
            model.setName(rule.getName());
//            model.setThingId(rule.getUID().getId());
        }

//        for (ConfigParameter param : getConfigurationParameters(rule, hideDefaultParameters)) {
//            ModelProperty property = buildModelProperty(param.name(), param.value());
//            if (property != null) {
//                model.getProperties().add(property);
//            }
//        }

        // TODO: (Nad) Lots... triggers, script...
//        List<Channel> channels = hideDefaultChannels ? getNonDefaultChannels(rule) : rule.getChannels();
//        model.setChannelsHeader(!channels.isEmpty());
//        for (Channel channel : channels) {
//            model.getChannels().add(buildModelChannel(channel, hideDefaultParameters));
//        }

        handledRules.add(rule);

        return model;
    }
}
