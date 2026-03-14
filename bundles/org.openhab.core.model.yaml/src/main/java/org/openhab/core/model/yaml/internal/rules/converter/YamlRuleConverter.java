/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.model.yaml.internal.rules.converter;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.converter.RuleParser;
import org.openhab.core.automation.converter.RuleSerializer;
import org.openhab.core.automation.module.script.rulesupport.shared.simple.SimpleRule;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.model.yaml.YamlElement;
import org.openhab.core.model.yaml.YamlModelRepository;
import org.openhab.core.model.yaml.internal.rules.YamlRuleDTO;
import org.openhab.core.model.yaml.internal.rules.YamlRuleProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * {@link YamlRuleConverter} is the YAML converter for {@link Rule} objects.
 *
 * @author Ravi Nadahar - Initial contribution
 */
@NonNullByDefault
@Component(immediate = true, service = { RuleSerializer.class, RuleParser.class })
public class YamlRuleConverter implements RuleSerializer, RuleParser {

    private final YamlModelRepository modelRepository;
    private final YamlRuleProvider ruleProvider;
    private final ConfigDescriptionRegistry configDescriptionRegistry; //TODO: (Nad) Needed?

    @Activate
    public YamlRuleConverter(@Reference YamlModelRepository modelRepository,
            @Reference YamlRuleProvider ruleProvider,
            final @Reference ConfigDescriptionRegistry configDescRegistry) {
        this.modelRepository = modelRepository;
        this.ruleProvider = ruleProvider;
        this.configDescriptionRegistry = configDescRegistry;
    }

    @Override
    public String getGeneratedFormat() {
        return "YAML";
    }

    @Override
    public List<SerializabilityResult> checkSerializability(Collection<Rule> rules) {
        List<SerializabilityResult> result = new ArrayList<>(rules.size());
        for (Rule rule : rules) {
            if (rule instanceof SimpleRule) {
                result.add(new SerializabilityResult(false, "Rule '" + rule.getUID() + "' is a SimpleRule with an inaccessible action"));
                continue;
            }
            if (rule.getConfiguration().get("sharedContext") instanceof Boolean shared && shared.booleanValue()) { //TODO: (Nad) Key name
                result.add(new SerializabilityResult(false, "Rule '" + rule.getUID() + "' is a DSL rule with shared context"));
                continue;
            }
            result.add(new SerializabilityResult(true, ""));
        }

        return result;
    }

    @Override
    public List<SerializabilityResult> setRulesToBeSerialized(String id, List<Rule> rules, boolean hideDefaultParameters) {
        List<SerializabilityResult> result = checkSerializability(rules);
        Map<Integer, Rule> supportedRules = new LinkedHashMap<>();
        for (int i = 0; i < result.size(); i++) {
            if (result.get(i).ok()) {
                supportedRules.put(Integer.valueOf(i), rules.get(i));
            }
        }

        Set<Rule> handledRules = new HashSet<>();
        List<YamlElement> elements = new ArrayList<>(rules.size());
        for (Rule rule : supportedRules.values()) {
            if (handledRules.contains(rule)) {
                continue;
            }
            elements.add(new YamlRuleDTO(rule)); // TODO: (Nad) Can this fail?
        }
        modelRepository.addElementsToBeGenerated(id, elements);
        return result;
    }

    @Override
    public void generateFormat(String id, OutputStream out) {
        modelRepository.generateFileFormat(id, out);
    }

    @Override
    public String getParserFormat() {
        return "YAML";
    }

    @Override
    public @Nullable String startParsingFormat(String syntax, List<String> errors, List<String> warnings) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(syntax.getBytes());
        return modelRepository.createIsolatedModel(inputStream, errors, warnings);
    }

    @Override
    public Collection<Rule> getParsedObjects(String modelName) {
        return ruleProvider.getAllFromModel(modelName);
    }

    @Override
    public void finishParsingFormat(String modelName) {
        modelRepository.removeIsolatedModel(modelName);
    }
}
