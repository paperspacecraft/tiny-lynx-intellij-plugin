package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
@Getter
public class SpellcheckResult {

    public static final SpellcheckResult EMPTY = new SpellcheckResult(StringUtils.EMPTY, Collections.emptyList(), StringUtils.EMPTY);

    public SpellcheckResult(String text) {
        this(text, Collections.emptyList(), StringUtils.EMPTY);
    }

    private final String text;
    private final List<SpellcheckAlert> alerts;
    private final String log;

    public boolean isEmpty() {
        return StringUtils.isEmpty(text) && alerts.isEmpty();
    }
}
