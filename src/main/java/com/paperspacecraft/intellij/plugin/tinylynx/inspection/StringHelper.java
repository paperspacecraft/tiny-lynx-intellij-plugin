package com.paperspacecraft.intellij.plugin.tinylynx.inspection;

import com.intellij.openapi.util.TextRange;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@UtilityClass
public class StringHelper {
    private static final char[] EXTRA_WHITESPACE_SAMPLES = new char[] {'/', '*'};

    public static int getFarthestSpaceToTheLeft(String value, int offset) {
        int spacePosition = offset;
        if (!isWhitespace(value.charAt(spacePosition))) {
            while (spacePosition >= 0 && !isWhitespace(value.charAt(spacePosition))) {
                spacePosition--;
            }
        }
        if (spacePosition >= 0) {
            while (spacePosition >= 0 && isWhitespace(value.charAt(spacePosition))) {
                spacePosition--;
            }
        }
        return spacePosition + 1;
    }

    public static int getAfterFarthestSpaceToTheRight(String value) {
        return getAfterFarthestSpaceToTheRight(value, 1);
    }

    public static int getAfterFarthestSpaceToTheRight(String value, int steps) {
        int result = 0;
        for (int i = 0; i < steps; i++) {
            result = skipNonWhitespaces(value, result);
            if (result >= value.length()) {
                return value.length();
            }
            result = skipWhitespaces(value, result);
        }
        return result;
    }

    public static boolean isWithinJavadocTag(String text, TextRange range) {
        InlineTagMatcher matcher = new InlineTagMatcher(text);
        while (matcher.find()) {
            if (matcher.start <= range.getStartOffset() && matcher.end >= range.getEndOffset()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWithinCodeSnippet(String text, TextRange range) {
        if (!StringUtils.contains(text, "`")) {
            return false;
        }
        List<TextRange> snippetRanges = new ArrayList<>(getDelimitedRanges(text, "```", Collections.emptyList(), true));
        snippetRanges.addAll(getDelimitedRanges(text, "`", snippetRanges, true));
        if  (snippetRanges.stream().noneMatch(r -> r.contains(range))) {
            return false;
        }
        return snippetRanges.stream().anyMatch(r -> r.contains(range));
    }

    public static boolean isWithinDelimiters(String text, TextRange range, String delimiter) {
        if (!StringUtils.contains(text, delimiter)) {
            return false;
        }
        List<TextRange> emphasisRanges = new ArrayList<>(getDelimitedRanges(text, delimiter, Collections.emptyList(), false));
        return emphasisRanges.stream().anyMatch(r -> r.contains(range));
    }

    public static List<TextRange> getDelimitedRanges(String text, String startingDelimiter, String closingDelimiter) {
        List<TextRange> result = new ArrayList<>();
        Deque<Integer> startingPositions = new LinkedList<>();
        int nextPoint = text.indexOf(startingDelimiter);
        boolean nextPointIsStart = true;
        while (nextPoint >= 0) {
            if (nextPointIsStart) {
                startingPositions.add(nextPoint);
            } else if (!startingPositions.isEmpty()) {
                result.add(new TextRange(startingPositions.removeLast(), nextPoint + closingDelimiter.length()));
            }
            int nextSearchPos = nextPoint + (nextPointIsStart ? startingDelimiter.length() : closingDelimiter.length());
            int nextStart = text.indexOf(startingDelimiter, nextSearchPos);
            int nextEnd = text.indexOf(closingDelimiter, nextSearchPos);
            if (nextEnd >= 0 && (nextStart < 0 || nextEnd < nextStart)) {
                nextPoint = nextEnd;
                nextPointIsStart = false;
            } else if (nextStart >= 0) {
                nextPoint = nextStart;
                nextPointIsStart = true;
            } else {
                nextPoint = -1;
            }
        }
        return result;
    }

    private static List<TextRange> getDelimitedRanges(
            String text,
            String delimiter,
            List<TextRange> existingRanges,
            boolean greedy) {
        List<TextRange> result = new ArrayList<>();
        int start = text.indexOf(delimiter);
        int end;
        while (start >= 0) {
            final int currentStart = start;
            TextRange existingRange = existingRanges.stream().filter(r -> r.contains(currentStart)).findFirst().orElse(null);
            if (existingRange != null) {
                start = text.indexOf(delimiter, existingRange.getEndOffset());
                continue;
            }
            end = text.indexOf(delimiter, start + delimiter.length());
            if (end <= start) {
                return result;
            }
            result.add(new TextRange(start, end + delimiter.length()));
            start = text.indexOf(delimiter, end + (greedy ? delimiter.length() : 0));
        }
        return result;
    }

    static boolean isOneWord(String value) {
        return value != null && value.chars().noneMatch(Character::isWhitespace);
    }

    private static int skipWhitespaces(String value, int offset) {
        if (offset < 0 || offset >= value.length()) {
            return -1;
        }
        for (int i = offset; i < value.length(); i++) {
            if (!isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return value.length();
    }

    private static int skipNonWhitespaces(String value, int offset) {
        if (offset < 0 || offset >= value.length()) {
            return -1;
        }
        for (int i = offset; i < value.length(); i++) {
            if (isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return value.length() + 1;
    }

    private static boolean isWhitespace(char value) {
        return Character.isWhitespace(value) || ArrayUtils.contains(EXTRA_WHITESPACE_SAMPLES, value);
    }

    public static WhitespaceMatcher whitespaceMatcher(String value) {
        return new WhitespaceMatcher(value);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class WhitespaceMatcher {

        private final String value;

        @Getter
        private int start;

        @Getter
        private int end;

        private int cursor;

        public boolean find() {
            boolean found = false;
            while (!found && cursor < value.length()) {
                start = skipNonWhitespaces(value, cursor);
                end = skipWhitespaces(value, start);
                if (start == -1 || end == -1) {
                    break;
                }
                found = start == 0 || end == value.length() || containsNewline(start, end);
                cursor = end;
            }
            return found;
        }

        private boolean containsNewline(int start, int end) {
            for (int i = start; i < end; i++) {
                if (value.charAt(i) == '\n' || value.charAt(i) == '\r') {
                    return true;
                }
            }
            return false;
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static class InlineTagMatcher {

        private final String value;

        @Getter
        private int start;

        @Getter
        private int end;

        private int cursor;

        public boolean find() {
            start = value.indexOf("{@", cursor);
            end = value.indexOf("}", start);
            boolean found = start >= 0 && end > start;
            cursor = found ? end + 1 : 0;
            return found;
        }
    }
}
