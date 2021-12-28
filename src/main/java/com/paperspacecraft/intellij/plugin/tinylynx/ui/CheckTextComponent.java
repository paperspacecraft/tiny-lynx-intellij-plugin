package com.paperspacecraft.intellij.plugin.tinylynx.ui;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.UIUtil;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.quickfix.ReplacementUtil;
import com.paperspacecraft.intellij.plugin.tinylynx.settings.SettingsConfigurator;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckAlert;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckResult;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckTask;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckWorkerService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

class CheckTextComponent implements Disposable {
    private static final Logger LOG = Logger.getInstance(CheckTextComponent.class);

    private static final String HTML_PLACEHOLDER = "<span class='placeholder'>Click &quot;Run&quot; to spellcheck the fragment</span>";
    private static final String HTML_RESULT_TEMPLATE = "%s<h3>Full response:</h3><div class='pre'>%s</div>";
    private static final String HTML_COLOR_STYLES = "body{color:%s;background-color:%s;}.placeholder,h1,h2,h3,h4,h6,h6{color:%s;}.description{color:%s;}.pre{background-color:%s;}";

    private static final String MISTAKE_TEMPLATE = "<span class='%s'>%s<sup>%d</sup></span>";
    private static final String MISTAKE_DESCRIPTION_TEMPLATE = "<p class='description'>%d. %s</p>";

    private JBSplitter splitter;
    private JBTextArea textEditor;
    private JEditorPane htmlView;

    private String htmlTemplate;
    private final AnAction checkAction;

    public CheckTextComponent() {
        checkAction = new CheckTextAction();
        initResources();
        initUiComponents();
    }

    private void initUiComponents() {
        splitter = new JBSplitter(false);
        splitter.setDividerWidth(1);

        JPanel textEditorPanel = new JPanel(new BorderLayout());
        textEditor = new JBTextArea();
        textEditor.setLineWrap(true);
        textEditor.setFont(splitter.getFont());
        JBScrollPane leftScrollPane = new JBScrollPane(textEditor);
        leftScrollPane.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 0));
        textEditorPanel.add(leftScrollPane, BorderLayout.CENTER);

        ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(
                ActionPlaces.TOOLBAR,
                new DefaultActionGroup(checkAction, new OpenSettingsAction()),
                false);
        actionToolbar.setTargetComponent(splitter);
        textEditorPanel.add(actionToolbar.getComponent(), BorderLayout.EAST);

        htmlView = new JEditorPane("text/html", String.format(htmlTemplate, prepareColorStylesPart(), HTML_PLACEHOLDER));
        htmlView.setEditable(false);
        htmlView.setBorder(BorderFactory.createEmptyBorder());
        JBScrollPane rightScrollPane = new JBScrollPane(htmlView);
        rightScrollPane.setBorder(BorderFactory.createEmptyBorder());

        splitter.setFirstComponent(textEditorPanel);
        splitter.setSecondComponent(rightScrollPane);

    }

    private void initResources() {
        try {
            URL resource = CheckTextComponent.class.getResource("/html/template.html");
            htmlTemplate = IOUtils.toString(Objects.requireNonNull(resource), StandardCharsets.UTF_8);
        } catch (IOException | NullPointerException e) {
            LOG.error("Could not read the resource at /html/template.html", e);
            htmlTemplate = "%s";
        }
    }

    @Override
    public void dispose() {
        splitter = null;
        textEditor = null;
        htmlView = null;
    }

    public JComponent getContent() {
        return splitter;
    }

    public JBTextArea getTextEditor() {
        return textEditor;
    }

    public void startChecking() {
        checkAction.actionPerformed(new AnActionEvent(
                null,
                DataManager.getInstance().getDataContext(splitter),
                ActionPlaces.TOOLBAR,
                checkAction.getTemplatePresentation(),
                ActionManager.getInstance(),
                0));
    }

    private static String prepareReport(String htmlTemplate, SpellcheckResult result) {
        String outerPart = String.format(htmlTemplate, prepareColorStylesPart(), HTML_RESULT_TEMPLATE);
        return String.format(
                outerPart,
                prepareReportMainPart(result),
                result.getLog().replace("\n", "<br/><br/>"));
    }

    private static String prepareReportMainPart(SpellcheckResult result) {
        if (result.getAlerts().isEmpty()) {
            return "No mistakes found";
        }
        StringBuilder analysis = new StringBuilder(result.getText());

        SpellcheckAlert[] sortedAlerts = result.getAlerts()
                .stream()
                .sorted((a,b) -> b.getRange().getStartOffset() - a.getRange().getStartOffset())
                .toArray(SpellcheckAlert[]::new);

        List<SpellcheckAlert> mergedSortedAlerts = mergeAlerts(sortedAlerts);
        int footnoteNumber = mergedSortedAlerts.size();
        List<String> mistakeDescriptions = new ArrayList<>();

        for (SpellcheckAlert alert : mergedSortedAlerts) {
            String oldText = alert.getRange().substring(analysis.toString());
            String insertion = String.format(
                    MISTAKE_TEMPLATE,
                    alert.isFacultative() ? "facultative" : "mistake",
                    oldText,
                    footnoteNumber);
            analysis.replace(alert.getRange().getStartOffset(), alert.getRange().getEndOffset(), insertion);

            String mistakeReplacements = Arrays.stream(ArrayUtils.nullToEmpty(alert.getReplacements()))
                    .map(repl -> ReplacementUtil.getFullValue(oldText, repl))
                    .collect(Collectors.joining("; "));
            if (!mistakeReplacements.isEmpty()) {
                mistakeReplacements = "<br/>Proposed replacements: " + mistakeReplacements;
            }

            String mistakeDescription = String.format(
                    MISTAKE_DESCRIPTION_TEMPLATE,
                    footnoteNumber--,
                    alert.getFullMessage() + mistakeReplacements);
            mistakeDescriptions.add(0, mistakeDescription);
        }
        return analysis + "<h3>Notes:</h3>" + String.join(StringUtils.EMPTY, mistakeDescriptions);
    }

    private static List<SpellcheckAlert> mergeAlerts(SpellcheckAlert[] alerts) {
        for (int i = 0; i < alerts.length - 1; i++) {
            SpellcheckAlert current = alerts[i];
            if (current == null) {
                continue;
            }
            for (int j = i + 1; j < alerts.length; j++) {
                SpellcheckAlert contender = alerts[j];
                if (contender != null && current.getRange().intersects(contender.getRange())) {
                    int start = Math.min(current.getRange().getStartOffset(), contender.getRange().getStartOffset());
                    int end = Math.max(current.getRange().getEndOffset(), contender.getRange().getEndOffset());
                    TextRange newRange = new TextRange(start, end);
                    alerts[i] = new SyntheticInlineAlert(
                            newRange,
                            current.getFullMessage() + ".<br/>" + contender.getFullMessage());
                    alerts[j] = null;
                }
            }
        }
        return Arrays.stream(alerts).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static String prepareColorStylesPart() {
        return String.format(
                HTML_COLOR_STYLES,
                toHex(UIUtil.getTextFieldForeground()),
                toHex(UIUtil.getTextFieldBackground()),
                toHex(UIUtil.getInactiveTextColor()),
                toHex(UIUtil.getTextAreaForeground().darker()),
                toHex(UIUtil.getPanelBackground()));
    }

    private static String toHex(Color color) {
        return String.format( "#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }


    private class CheckTextAction extends AnAction {

        public CheckTextAction() {
            super(
                    "Check the Current Fragment",
                    StringUtils.EMPTY,
                    AllIcons.RunConfigurations.TestState.Run);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {

            SpellcheckWorkerService service = Optional.ofNullable(e.getProject())
                    .map(project -> project.getService(SpellcheckWorkerService.class))
                    .orElse(null);
            String text = textEditor.getText().trim();
            if (service == null || text.isEmpty()) {
                return;
            }
            SpellcheckResult spellcheckResult = service.run(SpellcheckTask.syncModal(text)).getResult();
            htmlView.setText(prepareReport(htmlTemplate, spellcheckResult));
            SwingUtilities.invokeLater(() -> htmlView.scrollRectToVisible(new Rectangle(0, 0, 1, 1)));
        }

    }

    private static class OpenSettingsAction extends AnAction {

        public OpenSettingsAction() {
            super(
                    "Open Settings",
                    StringUtils.EMPTY,
                    AllIcons.Nodes.Editorconfig);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            ShowSettingsUtil.getInstance().showSettingsDialog(e.getProject(), SettingsConfigurator.class);
        }
    }
}
