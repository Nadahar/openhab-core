package org.openhab.core.io.dto;

import org.eclipse.jdt.annotation.NonNull;
import com.fasterxml.jackson.databind.JsonNode;

public interface ModularDTO<T, D, M> { // TODO: (Nad) Header + JavaDocs

    @NonNull D toDto(@NonNull JsonNode node, @NonNull M mapper) throws SerializationException;

    @NonNull JsonNode fromDto(@NonNull T object, @NonNull M mapper) throws SerializationException;
}
