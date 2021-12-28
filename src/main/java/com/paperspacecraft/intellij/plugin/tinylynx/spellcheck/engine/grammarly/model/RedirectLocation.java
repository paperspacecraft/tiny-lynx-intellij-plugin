package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly.model;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({"unused", "squid:S1170"})
public class RedirectLocation {

    public static final RedirectLocation INSTANCE = new RedirectLocation();

    private final String type = StringUtils.EMPTY;

    private final String location = "https://www.grammarly.com/after_install_page?extension_install=true&utm_medium=store&utm_source=firefox";
}
