package com.paperspacecraft.intellij.plugin.tinylynx.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.paperspacecraft.intellij.plugin.tinylynx.ui.CheckTextUi;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class OpenToolWindow extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (e.getProject() == null ||  editor == null) {
            return;
        }
        CheckTextUi.display(e.getProject(), editor.getSelectionModel().getSelectedText());
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Optional
                .ofNullable(editor)
                .map(Editor::getSelectionModel)
                .ifPresent(sel -> e.getPresentation().setText(sel.hasSelection() ? "Open in Tool Window" : "Open Tool Window"));
    }
}
