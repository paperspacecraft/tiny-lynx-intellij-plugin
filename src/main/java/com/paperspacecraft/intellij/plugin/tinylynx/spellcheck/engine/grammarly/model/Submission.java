package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly.model;

import com.google.gson.annotations.SerializedName;

@SuppressWarnings({"unused", "squid:S1170"})
public class Submission {
    private static final String PAYLOAD_TEMPLATE = "+0:0:%s:0";

    @SerializedName("ch")
    private final String[] payload;

    @SerializedName("rev")
    private final int revision = 0;

    private final int id = 0;

    private final String action = "submit_ot";

    public Submission(String text) {
        this.payload = new String[] {String.format(PAYLOAD_TEMPLATE, text)};
    }
}
