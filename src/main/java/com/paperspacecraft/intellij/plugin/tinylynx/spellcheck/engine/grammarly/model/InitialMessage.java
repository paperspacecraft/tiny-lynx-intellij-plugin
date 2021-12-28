package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({"unused", "squid:S1170"})
public class InitialMessage {

    public static final InitialMessage INSTANCE = new InitialMessage();

    private final String type = "initial";

    private final String docId = UUID.randomUUID().toString();

    private final String client = "extension_firefox";

    private final String protocolVersion = "1.0";

    private final String[] clientSupports = new String[] {
            "free_clarity_alerts",
            "readability_check",
            "filler_words_check",
            "sentence_variety_check",
            "free_occasional_premium_alerts"
    };

    private final String dialect = "american";

    private final String clientVersion = "14.924.2437";

    private final String extDomain = "keep.google.com";

    private final String action = "start";

    private final int id = 0;

    private final int sid = 0;
}
