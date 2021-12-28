package com.paperspacecraft.intellij.plugin.tinylynx.inspection.quickfix;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class ReplacementUtil {

    private static final String[] CONJUNCTIONS = new String[] {"and", "or"};

    static boolean isStandalone(String value) {
        return StringUtils.isNotEmpty(value) && !isAppendable(value) && !isPrependable(value) && !isInsertable(value);
    }

    static boolean isInsertable(String value) {
        return StringUtils.equals(value, ",");
    }

    static boolean isAppendable(String value) {
        if (StringUtils.isEmpty(value)) {
            return false;
        }
        return Character.isWhitespace(value.charAt(0))
                || (value.chars().noneMatch(Character::isLetterOrDigit) && !isInsertable(value));
    }

    static boolean isPrependable(String value) {
        if (StringUtils.isEmpty(value)) {
            return false;
        }
        return Character.isWhitespace(value.charAt(value.length() - 1));
    }

    public static String getFullValue(String oldValue, String newVaule) {
        if (isStandalone(newVaule)) {
            return newVaule;
        } else if (isPrependable(newVaule)) {
            return newVaule + oldValue;
        } else if (isInsertable(newVaule) && StringUtils.endsWithAny(oldValue, CONJUNCTIONS)) {
            return newVaule + StringUtils.SPACE + oldValue;
        } else {
            return oldValue + newVaule;
        }
    }
}
