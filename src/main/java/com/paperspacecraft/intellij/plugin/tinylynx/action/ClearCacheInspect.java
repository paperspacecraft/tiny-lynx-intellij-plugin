package com.paperspacecraft.intellij.plugin.tinylynx.action;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckService;
import org.jetbrains.annotations.NotNull;

public class ClearCacheInspect extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        project.getService(SpellcheckService.class).cleanUp();
        ActionManager
                .getInstance()
                .getAction("InspectCode")
                .actionPerformed(new AnActionEvent(
                        null,
                        e.getDataContext(),
                        ActionPlaces.EDITOR_POPUP,
                        getTemplatePresentation(),
                        ActionManager.getInstance(),
                        0));
    }
}
