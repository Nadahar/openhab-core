package org.openhab.core.automation.parser;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

@NonNullByDefault
public class ValidationException extends Exception { //TODO: (Nad) Header + JavaDocs

    private static final long serialVersionUID = 1L;

    /**
     * Keeps information about the type of the automation object for validation - module type, template or rule.
     */
    private final ObjectType type;

    /**
     * Keeps information about the UID of the automation object for validation - module type, template or rule.
     */
    private final @Nullable String uid;

    public ValidationException(ObjectType type, @Nullable String uid, @Nullable String message) {
        super(message);
        this.type = type;
        this.uid = uid;
    }

    public ValidationException(ObjectType type, @Nullable String uid, @Nullable Throwable cause) {
        super(cause);
        this.type = type;
        this.uid = uid;
    }

    public ValidationException(ObjectType type, @Nullable String uid, @Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
        this.type = type;
        this.uid = uid;
    }

    public ValidationException(ObjectType type, @Nullable String uid, @Nullable String message, @Nullable Throwable cause, boolean enableSuppression,
        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.type = type;
        this.uid = uid;
    }

    @Override
    public @Nullable String getMessage() {
        StringBuilder sb = new StringBuilder();
        switch (type) {
            case MODULE_TYPE:
                sb.append("[Module Type");
                break;
            case TEMPLATE:
                sb.append("[Template");
                break;
            case RULE:
                sb.append("[Rule");
                break;
            default:
                break;
        }
        if (uid != null) {
            sb.append(' ').append(uid);
        }
        sb.append("] ").append(super.getMessage());
        return sb.toString();
    }

    public enum ObjectType {
        MODULE_TYPE,
        TEMPLATE,
        RULE;
    }
}
