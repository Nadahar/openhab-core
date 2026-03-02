package org.openhab.core.model.rule.runtime.internal.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.StringInputStream;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.converter.RuleParser;
import org.openhab.core.automation.converter.RuleSerializer;
import org.openhab.core.io.dto.SerializationException;
import org.openhab.core.model.core.ModelRepository;
import org.openhab.core.model.rule.rules.ChangedEventTrigger;
import org.openhab.core.model.rule.rules.CommandEventTrigger;
import org.openhab.core.model.rule.rules.DateTimeTrigger;
import org.openhab.core.model.rule.rules.EventEmittedTrigger;
import org.openhab.core.model.rule.rules.EventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberChangedEventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberCommandEventTrigger;
import org.openhab.core.model.rule.rules.GroupMemberUpdateEventTrigger;
import org.openhab.core.model.rule.rules.RuleModel;
import org.openhab.core.model.rule.rules.RulesFactory;
import org.openhab.core.model.rule.rules.SystemStartlevelTrigger;
import org.openhab.core.model.rule.rules.ThingStateChangedEventTrigger;
import org.openhab.core.model.rule.rules.ThingStateUpdateEventTrigger;
import org.openhab.core.model.rule.rules.TimerTrigger;
import org.openhab.core.model.rule.rules.UpdateEventTrigger;
import org.openhab.core.model.rule.rules.ValidCommand;
import org.openhab.core.model.rule.rules.ValidState;
import org.openhab.core.model.rule.rules.ValidTrigger;
import org.openhab.core.model.rule.runtime.internal.DSLRuleProvider;
import org.openhab.core.model.script.ScriptStandaloneSetup;
import org.openhab.core.model.script.engine.Script;
import org.openhab.core.model.script.engine.ScriptParsingException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
@Component(immediate = true, service = { RuleSerializer.class, RuleParser.class })
public class DslRuleConverter implements RuleSerializer, RuleParser {

    private static final String SCRIPT_PLACEHOLDER_PREFIX = "SCRIPT_PLACEHOLDER_";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(?<=then\\R)^\\s*\"SCRIPT_PLACEHOLDER_(?<uid>[^\"]+)\"\\s*$\\R", Pattern.MULTILINE);
    private static final Pattern CONTEXT_COMMENT_PATTERN = Pattern.compile("^// context:.*$\\R", Pattern.MULTILINE);
    private static final Pattern INDENTATION_PATTERN = Pattern.compile("^(?=.)", Pattern.MULTILINE);

    private final Logger logger = LoggerFactory.getLogger(DslRuleConverter.class);

    private final ModelRepository modelRepository;
    private final DSLRuleProvider ruleProvider;
//    private final ScriptParser scriptParser;
//    private final GenericItemChannelLinkProvider itemChannelLinkProvider;
//    private final LocaleProvider localeProvider;

    private record ScriptElement(String placeholderLiteral, String scriptContent) {
    }

    private final Map<String, RuleModel> elementsToGenerate = new ConcurrentHashMap<>();

    private final Map<String, List<ScriptElement>> scriptElements = new ConcurrentHashMap<>();

    @Activate
    public DslRuleConverter(@Reference ModelRepository modelRepository,
            @Reference DSLRuleProvider ruleProvider
//            @Reference ScriptParser scriptParser
            /*,
            final @Reference ConfigDescriptionRegistry configDescRegistry,
            final @Reference LocaleProvider localeProvider*/) {
        this.modelRepository = modelRepository;
        this.ruleProvider = ruleProvider;
//        this.scriptParser = scriptParser;
//        this.thingProvider = thingProvider;
//        this.itemChannelLinkProvider = itemChannelLinkProvider;
//        this.localeProvider = localeProvider;
    }

    @Override
    public @NonNull String getParserFormat() {
        return "DSL";
    }

    @Override
    public String getGeneratedFormat() {
        return "DSL";
    }

    @Override
    public void setRulesToBeSerialized(String modelName, List<Rule> rules, boolean hideDefaultParameters) {
        if (rules.isEmpty()) {
            return;
        }
        RuleModel model = RulesFactory.eINSTANCE.createRuleModel();

        // Ensure that the variables collection is not null, calling get() creates an empty collection.
        model.getVariables();

        Set<Rule> handledRules = new HashSet<>();
        for (Rule rule : rules) {
            if (handledRules.contains(rule)) {
                continue;
            }
            try {
                org.openhab.core.model.rule.rules.Rule modelRule = RulesFactory.eINSTANCE.createRule();
                model.getRules().add(modelRule);
                String placeholderUid = UUID.randomUUID().toString();
                String placeholderLiteral = '"' + SCRIPT_PLACEHOLDER_PREFIX + placeholderUid  + '"';
                buildModelRule(rule, modelRule, placeholderLiteral, handledRules);
                scriptElements.compute(modelName, (k, v) -> {
                    List<ScriptElement> result = v == null ? new ArrayList<>() : v;
                    if (rule.getActions().getFirst().getConfiguration().get("script") instanceof String script) {
                        result.add(new ScriptElement(placeholderUid, script));
                    } else {
                        result.add(new ScriptElement(placeholderUid, ""));
                    }
                    return result;
                });
            } catch (SerializationException e) {
                logger.error("Invalid rule: {}", e.getMessage(), e); //TODO: (Nad) Figure out how to handle
            }
        }
        elementsToGenerate.put(modelName, model);
    }

    @Override
    public void generateFormat(String modelName, OutputStream out) {
        RuleModel model = elementsToGenerate.remove(modelName);
        if (model != null) {
            if (logger.isDebugEnabled()) {
                org.eclipse.emf.common.util.Diagnostic diagnostic = Diagnostician.INSTANCE.validate(model);
                if (diagnostic.getSeverity() != org.eclipse.emf.common.util.Diagnostic.OK) {
                    for (org.eclipse.emf.common.util.Diagnostic child : diagnostic.getChildren()) {
                        logger.warn("Model Validation Error: {}", child.getMessage());
                    }
                }
            }

            // Replace the placeholder with the actual script content
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            modelRepository.generateFileFormat(outputStream, "rules", model);
            String generated = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            Matcher m = PLACEHOLDER_PATTERN.matcher(generated);
            int safetyValve = 0;
            List<ScriptElement> elements;
            ScriptElement element;
            String scriptContent;
            while (m.find() && safetyValve < 1000) {
                elements = scriptElements.get(modelName);
                String uid = m.group("uid");
                if (uid != null && elements != null) {
                    element = elements.stream().filter(e -> uid.equals(e.placeholderLiteral)).findAny().orElse(null);
                    if (element != null) {
                        scriptContent = CONTEXT_COMMENT_PATTERN.matcher(element.scriptContent).replaceFirst("");
                        scriptContent = INDENTATION_PATTERN.matcher(scriptContent).replaceAll("\t");
                        generated = m.replaceFirst(scriptContent);
                    }
                }
                m = PLACEHOLDER_PATTERN.matcher(generated);
                safetyValve++;
            }
            if (safetyValve >= 1000) {
                logger.warn("Aborted replacing placeholders with script content to avoid endless loop, generated Rule DSL for '{}' will be invalid", modelName);
            }

            try {
                out.write(generated.getBytes());
            } catch (IOException e) {
                logger.warn("Exception when writing the generated syntax {}", e.getMessage());
            }
        }
    }

    @Override
    public @Nullable String startParsingFormat(String syntax, List<String> errors, List<String> warnings) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(syntax.getBytes());
        String result = modelRepository.createIsolatedModel("rules", inputStream, errors, warnings);
        return result;
    }

    @Override
    public @NonNull Collection<Rule> getParsedObjects(String modelName) {
        return ruleProvider.getAllFromModel(modelName);
    }

    @Override
    public void finishParsingFormat(String modelName) {
        modelRepository.removeModel(modelName);
        scriptElements.remove(modelName);
    }

    private org.openhab.core.model.rule.rules.Rule buildModelRule(Rule rule, org.openhab.core.model.rule.rules.Rule model,
            String placeholderLiteral, Set<Rule> handledRules) throws SerializationException {
        model.setName(rule.getName());

        model.getEventtrigger().add(buildModelTrigger(rule.getTriggers().getFirst()));

        XBlockExpression exp;
        try {
            exp = (XBlockExpression) parseScriptIntoXTextEObject(placeholderLiteral);
            logger.debug("exp={}", exp);
            model.setScript(exp);
        } catch (ScriptParsingException e) {
            // TODO: (Nad) Figure out
            e.printStackTrace();
        }

        handledRules.add(rule);

        return model;
    }

    private EventTrigger buildModelTrigger(Trigger trigger) throws SerializationException {
        String type = trigger.getTypeUID();
        Object value;
        RulesFactory factory = RulesFactory.eINSTANCE;
        switch (type) {
            case "core.SystemStartlevelTrigger":
                value = trigger.getConfiguration().get("startlevel");
                if (value instanceof Number num) {
                    int level = num.intValue();
                    if (level == 40) {
                        return factory.createSystemOnStartupTrigger();
                    } else {
                        SystemStartlevelTrigger result = factory.createSystemStartlevelTrigger();
                        result.setLevel(level);
                        return result;
                    }
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger); //TODO: (Nad) Find suitable exception
                }
            case "core.ItemCommandTrigger":
                value = trigger.getConfiguration().get("itemName");
                if (value instanceof String str) {
                    CommandEventTrigger result = factory.createCommandEventTrigger();
                    result.setItem(str);
                    value = trigger.getConfiguration().get("command");
                    if (value instanceof String command) {
                        ValidCommand cmd = factory.createValidCommand();
                        cmd.setValue(command);
                        result.setCommand(cmd);
                    }
                    return result;
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger); //TODO: (Nad) Find suitable exception
                }
            case "core.GroupCommandTrigger":
                value = trigger.getConfiguration().get("groupName");
                if (value instanceof String str) {
                    GroupMemberCommandEventTrigger result = factory.createGroupMemberCommandEventTrigger();
                    result.setGroup(str);
                    value = trigger.getConfiguration().get("command");
                    if (value instanceof String command) {
                        ValidCommand cmd = factory.createValidCommand();
                        cmd.setValue(command);
                        result.setCommand(cmd);
                    }
                    return result;
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger); //TODO: (Nad) Find suitable exception
                }
            case "core.ItemStateUpdateTrigger":
                value = trigger.getConfiguration().get("itemName");
                if (value instanceof String str) {
                    UpdateEventTrigger result = factory.createUpdateEventTrigger();
                    result.setItem(str);
                    value = trigger.getConfiguration().get("state");
                    if (value instanceof String state) {
                        ValidState st = factory.createValidState();
                        st.setValue(state);
                        result.setState(st);
                    }
                    return result;
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger); //TODO: (Nad) Find suitable exception
                }
            case "core.GroupStateUpdateTrigger":
                value = trigger.getConfiguration().get("groupName");
                if (value instanceof String str) {
                    GroupMemberUpdateEventTrigger result = factory.createGroupMemberUpdateEventTrigger();
                    result.setGroup(str);
                    value = trigger.getConfiguration().get("state");
                    if (value instanceof String state) {
                        ValidState st = factory.createValidState();
                        st.setValue(state);
                        result.setState(st);
                    }
                    return result;
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger); //TODO: (Nad) Find suitable exception
                }
            case "core.ItemStateChangeTrigger":
                value = trigger.getConfiguration().get("itemName");
                if (value instanceof String str) {
                    ChangedEventTrigger result = factory.createChangedEventTrigger();
                    result.setItem(str);
                    value = trigger.getConfiguration().get("state");
                    if (value instanceof String state) {
                        ValidState st = /*createValidStateFromDsl(state); */factory.createValidStateString();
                        st.setValue(state);
                        result.setNewState(st);
                    }
                    value = trigger.getConfiguration().get("previousState");
                    if (value instanceof String prevState) {
                        ValidState st = /*createValidStateFromDsl(prevState); */ factory.createValidStateString();
                        st.setValue(prevState);
                        result.setOldState(st);
                    }
                    return result;
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger); //TODO: (Nad) Find suitable exception
                }
            case "core.GroupStateChangeTrigger":
                value = trigger.getConfiguration().get("groupName");
                if (value instanceof String str) {
                    GroupMemberChangedEventTrigger result = factory.createGroupMemberChangedEventTrigger();
                    result.setGroup(str);
                    value = trigger.getConfiguration().get("state");
                    if (value instanceof String state) {
                        ValidState st = factory.createValidState();
                        st.setValue(state);
                        result.setNewState(st);
                    }
                    value = trigger.getConfiguration().get("previousState");
                    if (value instanceof String state) {
                        ValidState st = factory.createValidState();
                        st.setValue(state);
                        result.setOldState(st);
                    }
                    return result;
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger); //TODO: (Nad) Find suitable exception
                }
            case "timer.GenericCronTrigger":
                value = trigger.getConfiguration().get("cronExpression");
                if (value instanceof String str) {
                    TimerTrigger result = factory.createTimerTrigger();
                    if ("0 0 12 * * ?".equals(str)) {
                        result.setTime("noon");
                    } else if ("0 0 0 * * ?".equals(str)) {
                        result.setTime("midnight");
                    } else {
                        result.setCron(str);
                    }
                    return result;
                } else {
                    throw new SerializationException("Invalid trigger: " + trigger); //TODO: (Nad) Find suitable exception
                }
            case "timer.DateTimeTrigger":
                value = trigger.getConfiguration().get("itemName");
                if (value instanceof String str) {
                    DateTimeTrigger result = factory.createDateTimeTrigger();
                    result.setItem(str);
                    value = trigger.getConfiguration().get("timeOnly");
                    if (value instanceof Boolean timeOnly) {
                        result.setTimeOnly(timeOnly);
                    }
                    value = trigger.getConfiguration().get("offset");
                    if (value instanceof String offset) {
                        result.setOffset(offset);
                        return result;
                    }
                }
                throw new SerializationException("Invalid trigger: " + trigger); //TODO: (Nad) Find suitable exception
            case "core.ChannelEventTrigger":
                value = trigger.getConfiguration().get("channelUID");
                if (value instanceof String str) {
                    EventEmittedTrigger result = factory.createEventEmittedTrigger();
                    result.setChannel(str);
                    value = trigger.getConfiguration().get("event");
                    if (value instanceof String event) {
                        ValidTrigger trg = factory.createValidTrigger();
                        trg.setValue(event);
                        result.setTrigger(trg);
                    }
                    return result;
                }
                throw new SerializationException("Invalid trigger: " + trigger); //TODO: (Nad) Find suitable exception
            case "core.ThingStatusUpdateTrigger":
                value = trigger.getConfiguration().get("thingUID");
                if (value instanceof String str) {
                    ThingStateUpdateEventTrigger result = factory.createThingStateUpdateEventTrigger();
                    result.setThing(str);
                    value = trigger.getConfiguration().get("status");
                    if (value instanceof String status) {
                        result.setState(status);
                        return result;
                    }
                }
                throw new SerializationException("Invalid trigger: " + trigger); //TODO: (Nad) Find suitable exception
            case "core.ThingStatusChangeTrigger":
                value = trigger.getConfiguration().get("thingUID");
                if (value instanceof String str) {
                    ThingStateChangedEventTrigger result = factory.createThingStateChangedEventTrigger();
                    result.setThing(str);
                    value = trigger.getConfiguration().get("status");
                    if (value instanceof String status) {
                        result.setNewState(status);
                        value = trigger.getConfiguration().get("previousStatus");
                        if (value instanceof String previousStatus) {
                            result.setOldState(previousStatus);
                            return result;
                        }
                    }
                }
                throw new SerializationException("Invalid trigger: " + trigger); //TODO: (Nad) Find suitable exception
            default:
                throw new SerializationException("Unsupported trigger: " + trigger);
        }
    }

    private ValidState createValidStateFromDsl(String stateValue) {
        // 1. Create a minimal DSL string that the parser understands
        String dummyDsl = "rule 'temp' when Item x changed to " + stateValue + " then end";

        // 2. Use your existing resourceSet to parse it
        XtextResourceSet resourceSet = ScriptStandaloneSetup.getInjector().getInstance(XtextResourceSet.class);
        Resource resource = resourceSet.createResource(computeUnusedUri(resourceSet)); // IS-A XtextResource

        try (StringInputStream is = new StringInputStream(dummyDsl)) {
            resource.load(is, null);

            // 3. Navigate the model to find the trigger
//            RuleModel model = (RuleModel) resource.getContents().get(0);
            org.openhab.core.model.script.script.impl.ScriptImpl s = (org.openhab.core.model.script.script.impl.ScriptImpl) resource.getContents().getFirst();

//            org.openhab.core.model.rule.rules.Rule rule = model.getRules().getFirst();
//            ChangedEventTrigger trigger = (ChangedEventTrigger) rule.getEventtrigger().get(0);

            // 4. Copy the state object. EcoreUtil.copy is essential here
            // to detach it from the temporary resource.
//            return EcoreUtil.copy(trigger.getNewState());
            return (ValidState) EcoreUtil.copy(s.getExpressions().get(7));
        } catch (Exception e) {
            logger.error("Failed to parse state value: {}", stateValue, e);
            throw new RuntimeException("Failed to parse state value");
        } finally {
            resource.unload();
        }
    }

    private @Nullable EObject parseScriptIntoXTextEObject(String scriptAsString) throws ScriptParsingException {
        XtextResourceSet resourceSet = ScriptStandaloneSetup.getInjector().getInstance(XtextResourceSet.class);
        resourceSet.addLoadOption(XtextResource.OPTION_RESOLVE_ALL, Boolean.TRUE);

        Resource resource = resourceSet.createResource(computeUnusedUri(resourceSet)); // IS-A XtextResource
        try {
            resource.load(new StringInputStream(scriptAsString, StandardCharsets.UTF_8.name()),
                    resourceSet.getLoadOptions());
        } catch (IOException e) {
            throw new ScriptParsingException(
                    "Unexpected IOException; from close() of a String-based ByteArrayInputStream, no real I/O; how is that possible???",
                    scriptAsString, e);
        }

        List<Diagnostic> errors = resource.getErrors();
        if (!errors.isEmpty()) {
            deleteResource(resource);
            throw new ScriptParsingException("Failed to parse expression (due to managed SyntaxError/s)",
                    scriptAsString).addDiagnosticErrors(errors);
        }

        EList<EObject> contents = resource.getContents();
        if (!contents.isEmpty()) {
            return contents.getFirst();
        } else {
            deleteResource(resource);
            return null;
        }
    }

    protected URI computeUnusedUri(ResourceSet resourceSet) {
        String name = "__synthetic";
        int MAX_TRIES = 1000;
        for (int i = 0; i < MAX_TRIES; i++) {
            // NOTE: The "filename extension" (".script") must match the file.extensions in the *.mwe2
            URI syntheticUri = URI
                    .createURI(name + ThreadLocalRandom.current().nextDouble() + "." + Script.SCRIPT_FILEEXT);
            if (resourceSet.getResource(syntheticUri, false) == null) {
                return syntheticUri;
            }
        }
        throw new IllegalStateException("Unable to find a unused URI after 1000 attempts");
    }

    protected Iterable<Issue> getValidationErrors(EObject model) {
        List<Issue> validate = validate(model);
        return validate.stream().filter(input -> Severity.ERROR == input.getSeverity()).toList();
    }

    protected List<Issue> validate(EObject model) {
        IResourceValidator validator = ((XtextResource) model.eResource()).getResourceServiceProvider()
                .getResourceValidator();
        return validator.validate(model.eResource(), CheckMode.ALL, CancelIndicator.NullImpl);
    }

    private void deleteResource(Resource resource) {
        try {
            resource.delete(Map.of());
        } catch (IOException e) {
            // Ignore
        }
    }
}
