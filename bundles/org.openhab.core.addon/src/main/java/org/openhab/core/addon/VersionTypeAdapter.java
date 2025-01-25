package org.openhab.core.addon;

import java.io.IOException;

import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

//TODO: (Nad) Header + JavaDocs
public class VersionTypeAdapter extends TypeAdapter<Version> {

    @Override
    public @Nullable Version read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return Version.valueOf(in.nextString());
    }

    @Override
    public void write(JsonWriter out, @Nullable Version version) throws IOException {
        if (version == null) {
            out.nullValue();
        } else {
            out.value(version.toString());
        }
    }
}
