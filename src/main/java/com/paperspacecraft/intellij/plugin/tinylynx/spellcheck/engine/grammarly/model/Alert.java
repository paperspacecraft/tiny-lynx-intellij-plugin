package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly.model;

import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.util.TextRange;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckAlert;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unused", "squid:S1170"})
public class Alert implements SpellcheckAlert {

    @Setter
    private String group;

    @Setter
    private String title;

    private String category;

    @SerializedName("categoryHuman")
    private String friendlyCategoryInfo;

    @SerializedName("pname")
    private String extendedCategoryInfo;

    private String details;

    private String explanation;

    @Getter
    @SerializedName("hidden")
    private boolean facultative;

    @Getter
    @Setter
    @SerializedName("text")
    private String content;

    @SerializedName("highlightBegin")
    @Setter
    private int start;

    @SerializedName("highlightEnd")
    @Setter
    private int end;

    @Getter
    @Setter
    private String[] replacements;

    private CardLayout cardLayout;

    @Override
    public String getGroup() {
        String[] words = StringUtils.splitByCharacterTypeCamelCase(group);
        for (int i = 1; i < words.length; i++) {
            words[i] = words[i].toLowerCase();
        }
        return String.join(StringUtils.SPACE, words);
    }

    public String getTitle() {
        return StringUtils.defaultIfBlank(title, getCategory());
    }

    private String getCategory() {
        if (StringUtils.containsAny(extendedCategoryInfo, "WPCRedundantComma", "WPCRedComma")) {
            return "redundant comma";
        } else if (StringUtils.containsAny(extendedCategoryInfo, "WPCMissingComma", "WPCMissingComma")) {
            return "missing comma";
        }
        if (StringUtils.isNotBlank(friendlyCategoryInfo)) {
            return friendlyCategoryInfo;
        }
        return String.join(StringUtils.SPACE, StringUtils.splitByCharacterTypeCamelCase(category)).toLowerCase();
    }

    @Override
    public String getDescription() {
        return Stream.of(
                explanation,
                details,
                cardLayout != null ? cardLayout.getUserMuteCategoryDescription() : StringUtils.EMPTY)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.joining(StringUtils.SPACE));
    }

    @Override
    public String getFullMessage() {
        StringBuilder result = new StringBuilder(getGroup());
        if (!"Enhancement".equals(result.toString())) {
            result.append(" mistake");
        }
        if (StringUtils.isNotBlank(getTitle())) {
            result.append(": ").append(getTitle());
        }
        String description = getDescription();
        if (StringUtils.isNotBlank(description)) {
            result.append(". ").append(description);
        }
        return result.toString();
    }

    public TextRange getRange() {
        return new TextRange(start, end);
    }

}
