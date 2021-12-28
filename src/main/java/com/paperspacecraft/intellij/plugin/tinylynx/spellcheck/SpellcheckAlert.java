package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck;

import com.intellij.openapi.util.TextRange;

@SuppressWarnings({"unused", "squid:S1170"})
public interface SpellcheckAlert {

    String getGroup();

    String getTitle();

    String getDescription();

    String getFullMessage();

    String getContent();

    TextRange getRange();

    String[] getReplacements();

    boolean isFacultative();
}
