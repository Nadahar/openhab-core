/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.model.yaml.internal.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Condition;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.RuleProvider;
import org.openhab.core.automation.Trigger;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.util.ModuleBuilder;
import org.openhab.core.automation.util.RuleBuilder;
import org.openhab.core.common.registry.AbstractProvider;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.model.yaml.YamlModelListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;

/**
 * {@link YamlRuleProvider} is an OSGi service, that allows definition of rules in YAML configuration files. Files can
 * be added, updated or removed at runtime. The rules are automatically registered with
 * {@link org.openhab.core.automation.RuleRegistry}.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault // TODO: (Nad) Cleanup + JavaDocs
@Component(immediate = true, service = { RuleProvider.class, YamlRuleProvider.class, YamlModelListener.class })
public class YamlRuleProvider extends AbstractProvider<Rule> implements RuleProvider, YamlModelListener<YamlRuleDTO> {

    private final Logger logger = LoggerFactory.getLogger(YamlRuleProvider.class);

    private final Map<String, Collection<Rule>> rulesMap = new ConcurrentHashMap<>();

    @Activate
    public YamlRuleProvider() {
    }

    @Deactivate
    public void deactivate() {
        rulesMap.clear();
    }

    @Override
    public Collection<Rule> getAll() {
        return rulesMap.values().stream().flatMap(list -> list.stream()).toList();
    }

    @Override
    public Class<YamlRuleDTO> getElementClass() {
        return YamlRuleDTO.class;
    }

    @Override
    public JsonNode modifyTree(JsonNode node, ObjectMapper yamlMapper) {
        JsonNode triggersNode = node.get("triggers");
        if (triggersNode.isArray()) {
            boolean missingId = false;
            Set<String> ids = new HashSet<>();
            Iterator<JsonNode> iterator = triggersNode.elements();
            JsonNode aNode, bNode;
            String s;
            while (iterator.hasNext()) {
                aNode = iterator.next();
                if ((bNode = aNode.get("id")) != null && (s = bNode.asText()) != null && !s.isBlank()) {
                    ids.add(s);
                } else {
                    missingId |= true;
                }
            }
            if (missingId) {
                iterator = triggersNode.elements();
                int id = 0;
                String idStr;
                ObjectNode oNode;
                while (iterator.hasNext()) {
                    aNode = iterator.next();
                    if (aNode.isObject()) {
                        oNode = (ObjectNode) aNode;
                        if ((bNode = oNode.get("id")) == null || (s = bNode.asText()) == null || s.isBlank()) {
                            while (ids.contains(idStr = Integer.toString(++id))) {}
                            oNode.put("id", idStr);
                        }
                    }
                }
                ArrayNode arrayNode = (ArrayNode) triggersNode;
                // TODO: (Nad) Continue here
            }
        }

        return node;
    }

    @Override
    public boolean isVersionSupported(int version) {
        return version >= 1;
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public void addedModel(String modelName, Collection<YamlRuleDTO> elements) {
        List<Rule> added = elements.stream().map(r -> {
            try {
                return mapRule(r);
            } catch (JsonProcessingException e) {
                logger.warn("Failed to add rule \"{}\" to model {}: {}", r.uid, modelName, e.getMessage());
            }
            return null;
        }).filter(Objects::nonNull).toList();
        Collection<Rule> modelRules = Objects
                .requireNonNull(rulesMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        modelRules.addAll(added);
        added.forEach(r -> {
            logger.debug("model {} added rule {}", modelName, r.getUID());
            notifyListenersAboutAddedElement(r);
        });
    }

    @Override
    public void updatedModel(String modelName, Collection<YamlRuleDTO> elements) {
        List<Rule> updated = elements.stream().map(r -> {
            try {
                return mapRule(r);
            } catch (JsonProcessingException e) {
                logger.warn("Failed to update rule \"{}\" for model {}: {}", r.uid, modelName, e.getMessage());
            }
            return null;
        }).filter(Objects::nonNull).toList();
        Collection<Rule> modelRules = Objects
                .requireNonNull(rulesMap.computeIfAbsent(modelName, k -> new ArrayList<>()));
        updated.forEach(r -> {
            modelRules.stream().filter(rule -> rule.getUID().equals(r.getUID())).findFirst()
                    .ifPresentOrElse(oldRule -> {
                        modelRules.remove(oldRule);
                        modelRules.add(r);
                        logger.debug("model {} updated rule {}", modelName, r.getUID());
                        notifyListenersAboutUpdatedElement(oldRule, r);
                    }, () -> {
                        modelRules.add(r);
                        logger.debug("model {} added rule {}", modelName, r.getUID());
                        notifyListenersAboutAddedElement(r);
                    });
        }); // TODO: (Nad) Remember to create tests
    }

    @Override
    public void removedModel(String modelName, Collection<YamlRuleDTO> elements) {
        List<Rule> removed = elements.stream().map(r -> {
            try {
                return mapRule(r);
            } catch (JsonProcessingException e) {
                logger.warn("Failed to remove rule \"{}\" from model {}: {}", r.uid, modelName, e.getMessage());
            }
            return null;
        }).filter(Objects::nonNull).toList();
        Collection<Rule> modelRules = rulesMap.getOrDefault(modelName, List.of());
        removed.forEach(r -> {
            modelRules.stream().filter(rule -> rule.getUID().equals(r.getUID())).findFirst()
                    .ifPresentOrElse(oldRule -> {
                        modelRules.remove(oldRule);
                        logger.debug("model {} removed rule {}", modelName, r.getUID());
                        notifyListenersAboutRemovedElement(oldRule);
                    }, () -> logger.debug("model {} rule {} not found", modelName, r.getUID()));
        });
        if (modelRules.isEmpty()) {
            rulesMap.remove(modelName);
        }
    }

    private @Nullable Rule mapRule(YamlRuleDTO ruleDto) throws JsonProcessingException {
        YAMLFactory yamlFactory = YAMLFactory.builder() //
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER) // omit "---" at file start
            .disable(YAMLGenerator.Feature.SPLIT_LINES) // do not split long lines
            .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR) // indent arrays
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES) // use quotes only where necessary
            .enable(YAMLParser.Feature.PARSE_BOOLEAN_LIKE_WORDS_AS_STRINGS).build(); // do not parse ON/OFF/... as
                                                                                     // booleans
        ObjectMapper yamlMapper = new ObjectMapper(yamlFactory);
        yamlMapper.findAndRegisterModules();
        yamlMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        yamlMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        yamlMapper.setSerializationInclusion(Include.NON_NULL);
        yamlMapper.enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);

        String s;
        JsonNode node;
        RuleBuilder ruleBuilder = RuleBuilder.create(ruleDto.uid);
        if ((s = ruleDto.name) != null) {
            ruleBuilder.withName(s);
        }
        if ((s = ruleDto.templateUid) != null) {
            ruleBuilder.withTemplateUID(s);
        }
        Set<String> tags = ruleDto.tags;
        if (tags != null) {
            ruleBuilder.withTags(tags);
        }
        if ((s = ruleDto.description) != null) {
            ruleBuilder.withDescription(s);
        }
        Visibility visibility = ruleDto.getVisibility();
        if (visibility != null) {
            ruleBuilder.withVisibility(visibility);
        }
        Map<String, Object> configuration = ruleDto.config;
        if (configuration != null) {
            ruleBuilder.withConfiguration(new Configuration(configuration));
        }
        List<ConfigDescriptionParameter> configurationDescriptions = ruleDto.configDescriptions;
        if (configurationDescriptions != null) {
            ruleBuilder.withConfigurationDescriptions(configurationDescriptions);
        }
        if ((node = ruleDto.conditions) != null) {
            ruleBuilder.withConditions(mapModules(node, yamlMapper, Condition.class, YamlConditionDTO.class));
        }
        if ((node = ruleDto.actions) != null) {
            ruleBuilder.withActions(mapModules(node, yamlMapper, Action.class, YamlActionDTO.class));
        }
        if ((node = ruleDto.triggers) != null) {
            ruleBuilder.withTriggers(mapModules(node, yamlMapper, Trigger.class, YamlModuleDTO.class));
        }

        return ruleBuilder.build();
    }

    @SuppressWarnings("unchecked")
    private <T extends Module, D extends YamlModuleDTO> List<T> mapModules(JsonNode node, ObjectMapper yamlMapper,
            Class<T> targetClazz, Class<D> dtoClazz) throws JsonProcessingException {
        List<T> modules = new ArrayList<>();
        D moduleDto;
        if (node.isArray()) {
            JsonNode element;
            boolean generateIds = false;
            List<D> moduleDtos = new ArrayList<>();
            for (Iterator<JsonNode> iterator = node.iterator(); iterator.hasNext();) {
                element = iterator.next();
                try {
                    moduleDto = yamlMapper.treeToValue(element, dtoClazz);
                    generateIds |= moduleDto.id == null || moduleDto.id.isBlank();
                    moduleDtos.add(moduleDto);
                } catch (RuntimeException e) {
                    throw new JsonMappingException(null, "Failed to process YAML rule " + targetClazz.getSimpleName()
                            + ":\n" + element.toPrettyString() + "\n" + e.getMessage(), e);
                }
            }
            while (generateIds) {
                int id = 0;
                for (;;) {
                    String ids = Integer.toString(++id);
                    if (!moduleDtos.stream().anyMatch(m -> ids.equals(m.id))) {
                        break;
                    }
                }
                final String ids2 = Integer.toString(id);
                moduleDtos.stream().filter(m -> m.id == null || m.id.isBlank()).findFirst().ifPresent(m -> m.id = ids2);
                generateIds = moduleDtos.stream().anyMatch(m -> m.id == null || m.id.isBlank());
            }
            for (D mDto : moduleDtos) {
                try {
                    if (targetClazz.isAssignableFrom(Condition.class) && mDto instanceof YamlConditionDTO cDto) {
                        modules.add((T) ModuleBuilder.createCondition().withId(mDto.id).withTypeUID(mDto.type)
                                .withConfiguration(new Configuration(mDto.config)).withInputs(cDto.inputs)
                                .withLabel(mDto.label).withDescription(mDto.description).build());
                    } else if (targetClazz.isAssignableFrom(Action.class) && mDto instanceof YamlActionDTO aDto) {
                        modules.add((T) ModuleBuilder.createAction().withId(mDto.id).withTypeUID(mDto.type)
                                .withConfiguration(new Configuration(mDto.config)).withInputs(aDto.inputs)
                                .withLabel(mDto.label).withDescription(mDto.description).build());
                    } else if (targetClazz.isAssignableFrom(Trigger.class)) {
                        modules.add((T) ModuleBuilder.createTrigger().withId(mDto.id).withTypeUID(mDto.type)
                                .withConfiguration(new Configuration(mDto.config)).withLabel(mDto.label)
                                .withDescription(mDto.description).build());
                    } else {
                        throw new IllegalArgumentException("Invalid combination of target and dto classes: "
                                + targetClazz.getSimpleName() + " <-> " + dtoClazz.getSimpleName());
                    }
                } catch (RuntimeException e) {
                    throw new JsonMappingException(null, "Failed to process YAML rule " + targetClazz.getSimpleName()
                            + ": \"" + mDto + "\": " + e.getMessage(), e);
                }
            }
        } else {
            Entry<String, JsonNode> entry;
            for (Iterator<Entry<String, JsonNode>> iterator = node.fields(); iterator.hasNext();) {
                entry = iterator.next();
                try {
                    moduleDto = yamlMapper.treeToValue(entry.getValue(), dtoClazz);
                    if (targetClazz.isAssignableFrom(Condition.class) && moduleDto instanceof YamlConditionDTO cDto) {
                        modules.add(
                                (T) ModuleBuilder.createCondition().withId(entry.getKey()).withTypeUID(moduleDto.type)
                                        .withConfiguration(new Configuration(moduleDto.config)).withInputs(cDto.inputs)
                                        .withLabel(moduleDto.label).withDescription(moduleDto.description).build());
                    } else if (targetClazz.isAssignableFrom(Action.class) && moduleDto instanceof YamlActionDTO aDto) {
                        modules.add((T) ModuleBuilder.createAction().withId(entry.getKey()).withTypeUID(moduleDto.type)
                                .withConfiguration(new Configuration(moduleDto.config)).withInputs(aDto.inputs)
                                .withLabel(moduleDto.label).withDescription(moduleDto.description).build());
                    } else if (targetClazz.isAssignableFrom(Trigger.class)) {
                        modules.add((T) ModuleBuilder.createTrigger().withId(entry.getKey()).withTypeUID(moduleDto.type)
                                .withConfiguration(new Configuration(moduleDto.config)).withLabel(moduleDto.label)
                                .withDescription(moduleDto.description).build());
                    } else {
                        throw new IllegalArgumentException("Invalid combination of target and dto classes: "
                                + targetClazz.getSimpleName() + " <-> " + dtoClazz.getSimpleName());
                    }
                } catch (RuntimeException e) {
                    throw new JsonMappingException(null, "Failed to process YAML rule condition:\n"
                            + entry.getValue().toPrettyString() + "\n" + e.getMessage(), e);
                }
            }
        }

        return modules;
    }
}
