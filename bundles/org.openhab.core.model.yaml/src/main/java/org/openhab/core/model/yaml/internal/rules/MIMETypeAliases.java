package org.openhab.core.model.yaml.internal.rules;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class MIMETypeAliases { // TODO: (Nad) Heeader + JavaDocs

    private static final Map<String, String> ALIAS_IDX;
    private static final Map<String, String> MIME_TYPE_IDX;

    static {
        /*
         * The table below is formatted as follows:
         * - The first column is the MIME type
         * - The second column is the "primary code" that will be used when translating to alias
         * - The remaining columns are optional aliases that will only work when translation to MIME type
         */
        String[][] table = { //
            {"application/vnd.openhab.dsl.rule", "RuleDSL", "DSL"}, //
            {"application/x-groovy", "Groovy"}, //
            {"application/javascript", "JavaScript", "JS", "GraalJS"}, //
            {"application/python", "Python", "Python3", "PY", "PY3"}, //
            {"application/x-python2", "Jython", "JythonPY", "PY2"},
            {"application/x-ruby", "Ruby", "RB"}, //
            {"application/javascript;version=ECMAScript-5.1", "NashornJS", "NashornJavaScript" } //
        };

        Map<String, String> aliasIdx = new HashMap<>();
        Map<String, String> mimeTypeIdx = new HashMap<>();
        for (String[] entry : table) {
            mimeTypeIdx.put(entry[0], entry[1]);
            for (int i = 1; i < entry.length; i++) {
                aliasIdx.put(entry[i].toLowerCase(Locale.ROOT), entry[0]);
            }
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
        return ALIAS_IDX.getOrDefault(alias.toLowerCase(Locale.ROOT), alias);
    }

    public static String mimeTypeToAlias(String type) {
        return MIME_TYPE_IDX.getOrDefault(type, type);
    }
}
