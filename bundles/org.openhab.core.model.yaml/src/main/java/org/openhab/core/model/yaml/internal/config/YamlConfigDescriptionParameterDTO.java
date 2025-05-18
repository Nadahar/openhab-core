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
package org.openhab.core.model.yaml.internal.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;
import org.openhab.core.config.core.FilterCriteria;
import org.openhab.core.config.core.ParameterOption;
import org.openhab.core.config.core.dto.FilterCriteriaDTO;
import org.openhab.core.config.core.dto.ParameterOptionDTO;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This is a data transfer object used to serialize a parameter of a configuration description in a YAML configuration.
 *
 * @author Ravi Nadahar - Initial contribution
 */
public class YamlConfigDescriptionParameterDTO { // TODO: (Nad) JavaDocs

    public String context;
    @JsonProperty("default")
    public String defaultValue;
    public String description;
    public String label;
    public String name;
    public boolean required;
    public Type type;
    public BigDecimal min;
    public BigDecimal max;
    public BigDecimal stepsize;
    public String pattern;
    public Boolean readOnly;
    public Boolean multiple;
    public Integer multipleLimit;
    public String groupName;
    public Boolean advanced;
    public Boolean verify;
    public Boolean limitToOptions;
    public String unit;
    public String unitLabel;

    public List<ParameterOptionDTO> options;
    public List<FilterCriteriaDTO> filterCriteria;

    public YamlConfigDescriptionParameterDTO() {
    }

    public YamlConfigDescriptionParameterDTO(@NonNull ConfigDescriptionParameter parameter) {
        this.name = parameter.getName();
        this.type = parameter.getType();
        this.min = parameter.getMinimum();
        this.max = parameter.getMaximum();
        this.stepsize = parameter.getStepSize();
        this.pattern = parameter.getPattern();
        this.readOnly = parameter.isReadOnly();
        this.multiple = parameter.isMultiple();
        this.context = parameter.getContext();
        this.required = parameter.isRequired();
        this.defaultValue = parameter.getDefault();
        this.label = parameter.getLabel();
        this.description = parameter.getDescription();
        List<@NonNull ParameterOption> options = parameter.getOptions();
        if (!options.isEmpty()) {
            List<ParameterOptionDTO> optionDtos = new ArrayList<>(options.size());
            for (ParameterOption option : options) {
                optionDtos.add(new ParameterOptionDTO(option.getValue(), option.getLabel()));
            }
            this.options = optionDtos;
        }
        List<@NonNull FilterCriteria> filterCriterias = parameter.getFilterCriteria();
        if (!filterCriteria.isEmpty()) {
            List<FilterCriteriaDTO> filterCriteriaDtos = new ArrayList<>(filterCriteria.size());
            for (FilterCriteria filterCriteria : filterCriterias) {
                filterCriteriaDtos.add(new FilterCriteriaDTO(filterCriteria.getName(), filterCriteria.getValue()));
            }
            this.filterCriteria = filterCriteriaDtos;
        }
        this.groupName = parameter.getGroupName();
        this.advanced = parameter.isAdvanced();
        this.limitToOptions = parameter.getLimitToOptions();
        this.multipleLimit = parameter.getMultipleLimit();
        this.unit = parameter.getUnit();
        this.unitLabel = parameter.getUnitLabel();
        this.verify = parameter.isVerifyable();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append(" [");
        if (context != null) {
            builder.append("context=").append(context).append(", ");
        }
        if (defaultValue != null) {
            builder.append("defaultValue=").append(defaultValue).append(", ");
        }
        if (description != null) {
            builder.append("description=").append(description).append(", ");
        }
        if (label != null) {
            builder.append("label=").append(label).append(", ");
        }
        if (name != null) {
            builder.append("name=").append(name).append(", ");
        }
        builder.append("required=").append(required).append(", ");
        if (type != null) {
            builder.append("type=").append(type).append(", ");
        }
        if (min != null) {
            builder.append("min=").append(min).append(", ");
        }
        if (max != null) {
            builder.append("max=").append(max).append(", ");
        }
        if (stepsize != null) {
            builder.append("stepsize=").append(stepsize).append(", ");
        }
        if (pattern != null) {
            builder.append("pattern=").append(pattern).append(", ");
        }
        if (readOnly != null) {
            builder.append("readOnly=").append(readOnly).append(", ");
        }
        if (multiple != null) {
            builder.append("multiple=").append(multiple).append(", ");
        }
        if (multipleLimit != null) {
            builder.append("multipleLimit=").append(multipleLimit).append(", ");
        }
        if (groupName != null) {
            builder.append("groupName=").append(groupName).append(", ");
        }
        if (advanced != null) {
            builder.append("advanced=").append(advanced).append(", ");
        }
        if (verify != null) {
            builder.append("verify=").append(verify).append(", ");
        }
        if (limitToOptions != null) {
            builder.append("limitToOptions=").append(limitToOptions).append(", ");
        }
        if (unit != null) {
            builder.append("unit=").append(unit).append(", ");
        }
        if (unitLabel != null) {
            builder.append("unitLabel=").append(unitLabel).append(", ");
        }
        if (options != null) {
            builder.append("options=").append(options).append(", ");
        }
        if (filterCriteria != null) {
            builder.append("filterCriteria=").append(filterCriteria);
        }
        builder.append("]");
        return builder.toString();
    }

    public static @NonNull List<@NonNull ConfigDescriptionParameter> mapConfigDescriptions(
            @NonNull List<@NonNull YamlConfigDescriptionParameterDTO> configDescriptionDtos) {
        List<ConfigDescriptionParameter> result = new ArrayList<>(configDescriptionDtos.size());
        List<FilterCriteriaDTO> filterCriteriaDtos;
        List<FilterCriteria> filterCriterias;
        List<ParameterOptionDTO> parameterOptionDtos;
        List<ParameterOption> parameterOptions;
        ConfigDescriptionParameterBuilder builder;
        for (YamlConfigDescriptionParameterDTO parameterDto : configDescriptionDtos) {
            builder = ConfigDescriptionParameterBuilder.create(parameterDto.name, parameterDto.type)
                    .withAdvanced(parameterDto.advanced).withContext(parameterDto.context)
                    .withDefault(parameterDto.defaultValue).withDescription(parameterDto.description)
                    .withGroupName(parameterDto.groupName).withLabel(parameterDto.label)
                    .withLimitToOptions(parameterDto.limitToOptions).withMaximum(parameterDto.max)
                    .withMinimum(parameterDto.min).withMultiple(parameterDto.multiple)
                    .withMultipleLimit(parameterDto.multipleLimit).withPattern(parameterDto.pattern)
                    .withReadOnly(parameterDto.readOnly).withRequired(parameterDto.required)
                    .withStepSize(parameterDto.stepsize).withUnit(parameterDto.unit)
                    .withUnitLabel(parameterDto.unitLabel).withVerify(parameterDto.verify);
            filterCriteriaDtos = parameterDto.filterCriteria;
            if (filterCriteriaDtos != null) {
                filterCriterias = new ArrayList<>(filterCriteriaDtos.size());
                for (FilterCriteriaDTO filterCriteriaDto : filterCriteriaDtos) {
                    filterCriterias.add(new FilterCriteria(filterCriteriaDto.name, filterCriteriaDto.value));
                }
                builder.withFilterCriteria(filterCriterias);
            }
            parameterOptionDtos = parameterDto.options;
            if (parameterOptionDtos != null) {
                parameterOptions = new ArrayList<>(parameterOptionDtos.size());
                for (ParameterOptionDTO optionDto : parameterOptionDtos) {
                    parameterOptions.add(new ParameterOption(optionDto.value, optionDto.label));
                }
                builder.withOptions(parameterOptions);
            }
            result.add(builder.build());
        }
        return result;
    }
}
