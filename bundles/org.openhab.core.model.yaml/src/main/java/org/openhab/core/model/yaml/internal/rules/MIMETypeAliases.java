package org.openhab.core.model.yaml.internal.rules;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class MIMETypeAliases { // TODO: (Nad) Heeader + JavaDocs

    private static final Map<String, String> ALIAS_IDX;
    private static final Map<String, String> MIME_TYPE_IDX;

    static {
        String[][] table = { //
            {"DSL", "application/vnd.openhab.dsl.rule"}, //
            {"GROOVY", "application/x-groovy"}, //
            {"JS", "application/javascript"}, // "application/javascript", "application/ecmascript", "text/javascript", "text/ecmascript" "application/javascript;version=ECMAScript-2021"
            {"PY", "application/python"}, // "text/python", "application/python", "text/x-python", "application/x-python"
            {"RUBY", "application/x-ruby"}, //
            {"NASHORNJS", "application/javascript;version=ECMAScript-5.1"}, // "application/javascript;version=ECMAScript-5.1", "application/ecmascript;version=ECMAScript-5.1", "text/javascript;version=ECMAScript-5.1", "text/ecmascript;version=ECMAScript-5.1"
        };

        Map<String, String> aliasIdx = new HashMap<>();
        Map<String, String> mimeTypeIdx = new HashMap<>();
        for (String[] entry : table) {
            aliasIdx.put(entry[0], entry[1]);
            mimeTypeIdx.put(entry[1], entry[0]);
        }
        ALIAS_IDX = Collections.unmodifiableMap(aliasIdx);
        MIME_TYPE_IDX = Collections.unmodifiableMap(mimeTypeIdx);
    }

    /**
     * Not to be instantiated
     */
    private MIMETypeAliases() {
    }

    public static String aliasToType(String alias) {
        return ALIAS_IDX.getOrDefault(alias, alias);
    }

    public static String mimeTypeToAlias(String type) {
        return MIME_TYPE_IDX.getOrDefault(type, type);
    }
}
