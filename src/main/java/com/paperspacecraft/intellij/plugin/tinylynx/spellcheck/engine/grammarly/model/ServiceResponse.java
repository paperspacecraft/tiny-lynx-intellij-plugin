package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceResponse {

    public static final ServiceResponse EMPTY = new ServiceResponse(StringUtils.EMPTY, StringUtils.EMPTY);

    private String action;

    private String error;
}
