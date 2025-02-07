package org.openhab.core.addon;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class DiscourseCookedUtil {

    public static final Pattern TAG_PATTERN = Pattern.compile("<(/)?([a-zA-Z]+\\b)[^>]*?(/)?>");
    public static final Set<String> VOID_TAGS = Set.of("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr");
    public static final Set<String> DISQUALIFYING_TAGS = Set.of("img", "svg");

    /**
     * Not to be instantiated.
     */
    private DiscourseCookedUtil() {
    }

    // Partially parses the HTML content to find the correct insertion point for adding a paragraph before the existing text
    // -1 means not found
    public static int findParagraphInsertionPoint(@Nullable String source) {
        int result = -1;
        if (source == null || source.isBlank()) {
            return result;
        }
        Map<String, Integer> tags = new HashMap<>();
        Matcher m = TAG_PATTERN.matcher(source);
        String tag;
        boolean closing, selfClosing;
        int count;
        Integer value;
        boolean disqualified = true;
        int pIdx = -1;
        while (m.find()) {
            closing = m.group(1) != null;
            tag = m.group(2).toLowerCase(Locale.ROOT);
            selfClosing = m.group(3) != null || VOID_TAGS.contains(tag);
            if (DISQUALIFYING_TAGS.contains(tag)) {
                disqualified = true;
            }
            if (selfClosing) {
                continue;
            }
            value = tags.get(tag);
            if (value != null) {
                if (closing) {
                    count = value - 1;
                    if (count < 1) {
                        if ("p".equals(tag) && !disqualified) {
                            result = pIdx;
                            break;
                        }
                        tags.remove(tag);
                    } else {
                        tags.put(tag, Integer.valueOf(count));
                    }
                } else {
                    count = value + 1;
                    tags.put(tag, Integer.valueOf(count));
                }
            } else if (!closing) {
                if ("p".equals(tag)) {
                    pIdx = m.start();
                    disqualified = false;
                }
                tags.put(tag, Integer.valueOf(1));
            }
        }
        return result;
    }
}
