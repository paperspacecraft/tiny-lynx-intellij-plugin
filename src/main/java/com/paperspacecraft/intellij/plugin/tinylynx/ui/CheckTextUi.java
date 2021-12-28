package com.paperspacecraft.intellij.plugin.tinylynx.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.RegisterToolWindowTask;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import icons.StaticIcons;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

@UtilityClass
public class CheckTextUi {

    private static final String WINDOW_ID = "tinylynx:proofreadingWindow";
    private static final String PROJECT_TITLE = "Tiny Lynx";
    private static final String WINDOW_TITLE = "Proofreading";

    public static void display(Project project, String text) {
        ToolWindow toolWindow = ensureToolWindow(project);
        Content content = toolWindow.getContentManager().findContent(WINDOW_TITLE);
        CheckTextComponent checkTextWindow;
        if (content == null) {
            checkTextWindow = new CheckTextComponent();
            ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            content = contentFactory.createContent(checkTextWindow.getContent(), WINDOW_TITLE, false);
            content.setDisposer(checkTextWindow);
            toolWindow.getContentManager().addContent(content);
            toolWindow.activate(null);
        } else {
            checkTextWindow = (CheckTextComponent) content.getDisposer();
        }
        if (StringUtils.isNotBlank(text)) {
            toolWindow.activate(() -> {
                Objects.requireNonNull(checkTextWindow).getTextEditor().setText(text + "\n");
                checkTextWindow.startChecking();
            });
        } else {
            toolWindow.activate(null);
        }
    }

    private static ToolWindow ensureToolWindow(Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow result = toolWindowManager.getToolWindow(WINDOW_ID);
        if (result != null) {
            return result;
        }
        result = toolWindowManager.registerToolWindow(
                RegisterToolWindowTask.closable(
                        WINDOW_ID,
                        StaticIcons.TinyLynx,
                        ToolWindowAnchor.BOTTOM));
        result.setStripeTitle(PROJECT_TITLE);
        result.setTitle(WINDOW_TITLE);
        return result;
    }
}
