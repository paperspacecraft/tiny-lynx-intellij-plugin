package com.paperspacecraft.intellij.plugin.tinylynx.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectUtil;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SettingsConfigurator implements Configurable {

    private final SettingsService settingsService;
    private SettingsComponent settingsComponent;

    public SettingsConfigurator(Project project) {
        settingsService = project.getService(SettingsService.class);
        settingsComponent = new SettingsComponent();
    }

    @Override
    public String getDisplayName() {
        return "Tiny Lynx";
    }

    @Override
    public @Nullable JComponent createComponent() {
        return settingsComponent.getContentPanel();
    }

    @Override
    public boolean isModified() {
        return settingsService.isOnTheFly() != settingsComponent.isOnTheFly()
                || settingsService.isShowAdvancedMistakes() != settingsComponent.isShowAdvancedMistakes()
                || settingsService.getCacheLifespan() != settingsComponent.getCacheLifespan()
                || settingsService.getParallelRequests() != settingsComponent.getParallelRequests()
                || settingsService.isExtendedLogging() != settingsComponent.isExtendedLogging()
                || !StringUtils.equals(settingsService.getGrammarlyClientType(), settingsComponent.getGrammarlyClientType())
                || !StringUtils.equals(settingsService.getGrammarlyClientVersion(), settingsComponent.getGrammarlyClientVersion())
                || !StringUtils.equals(settingsService.getGrammarlyClientOrigin(), settingsComponent.getGrammarlyClientOrigin())
                || !StringUtils.equals(settingsService.getGrammarlyUserAgent(), settingsComponent.getGrammarlyUserAgent())
                || !StringUtils.equals(settingsService.getGrammarlyCookie(), settingsComponent.getGrammarlyCookie())
                || !CollectionUtils.isEqualCollection(settingsService.getExclusionSet(), settingsComponent.getExclusions());
    }

    @Override
    public void apply() {
        settingsService.setOnTheFly(settingsComponent.isOnTheFly());
        settingsService.setShowAdvancedMistakes(settingsComponent.isShowAdvancedMistakes());
        settingsService.setCacheLifespan(settingsComponent.getCacheLifespan());
        settingsService.setParallelRequests(settingsComponent.getParallelRequests());
        settingsService.setExtendedLogging(settingsComponent.isExtendedLogging());
        settingsService.getExclusionSet().clear();
        settingsService.getExclusionSet().addAll(settingsComponent.getExclusions());

        settingsService.setGrammarlyClientType(settingsComponent.getGrammarlyClientType());
        settingsService.setGrammarlyClientVersion(settingsComponent.getGrammarlyClientVersion());
        settingsService.setGrammarlyClientOrigin(settingsComponent.getGrammarlyClientOrigin());
        settingsService.setGrammarlyUserAgent(settingsComponent.getGrammarlyUserAgent());
        settingsService.setGrammarlyCookie(settingsComponent.getGrammarlyCookie());

        ProjectManager.getInstance().reloadProject(ProjectUtil.guessCurrentProject(settingsComponent.getContentPanel()));
    }

    @Override
    public void reset() {
        settingsComponent.setOnTheFly(settingsService.isOnTheFly());
        settingsComponent.setShowAdvancedMistakes(settingsService.isShowAdvancedMistakes());
        settingsComponent.setCacheLifespan(settingsService.getCacheLifespan());
        settingsComponent.setParallelRequests(settingsService.getParallelRequests());
        settingsComponent.setExtendedLogging(settingsService.isExtendedLogging());
        settingsComponent.setExclusions(settingsService.getExclusionSet());

        settingsComponent.setGrammarlyClientType(settingsService.getGrammarlyClientType());
        settingsComponent.setGrammarlyClientVersion(settingsService.getGrammarlyClientVersion());
        settingsComponent.setGrammarlyClientOrigin(settingsService.getGrammarlyClientOrigin());
        settingsComponent.setGrammarlyUserAgent(settingsService.getGrammarlyUserAgent());
        settingsComponent.setGrammarlyCookie(settingsService.getGrammarlyCookie());
    }

    @Override
    public void disposeUIResources() {
        settingsComponent = null;
    }

}
