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
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Rule;
import org.openhab.core.automation.converter.RuleParser;
import org.openhab.core.automation.converter.RuleSerializer;
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
    public void setRulesToBeSerialized(String id, List<Rule> rules, boolean hideDefaultParameters) {
        List<YamlElement> elements = new ArrayList<>(rules.size());
        for (Rule rule : rules) {
            elements.add(new YamlRuleDTO(rule));
        }
        modelRepository.addElementsToBeGenerated(id, elements);
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
