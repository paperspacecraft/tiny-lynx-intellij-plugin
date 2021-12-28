package com.paperspacecraft.intellij.plugin.tinylynx.ui;

import com.intellij.openapi.util.TextRange;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckAlert;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class SyntheticInlineAlert implements SpellcheckAlert {

    @Getter
    private final TextRange range;

    @Getter
    private final String fullMessage;

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getOverhead() {
        return null;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getContent() {
        return null;
    }

    @Override
    public String[] getReplacements() {
        return new String[0];
    }

    @Override
    public boolean isFacultative() {
        return false;
    }
}
