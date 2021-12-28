package com.paperspacecraft.intellij.plugin.tinylynx.settings;

import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

class SettingsComponent {

    private static final InputValidator EXCLUSIONS_VALIDATOR = new ExclusionInputValidator();
    private static final InputVerifier NUMERIC_VERIFIER = new NumericInputVerifier();
    private static final InputVerifier NOT_BLANK_VERIFIER = new NonBlankInputVerifier();

    private JPanel pnlContent;
    private JCheckBox cbEnabled;
    private JCheckBox cbShowAdvanced;
    private JBTextField tbCacheLifespan;
    private JBTextField tbParallelRequests;

    private JBTextField tbGrammarlyClientType;
    private JBTextField tbGrammarlyClientVersion;
    private JBTextField tbGrammarlyClientOrigin;
    private JBTextField tbGrammarlyUserAgent;
    private JBTextField tbGrammarlyCookie;

    private CollectionListModel<String> lstExclusionsModel;

    public SettingsComponent() {
        createUi();
    }

    /* ---------
       Accessors
       --------- */

    public JComponent getContentPanel() {
        return pnlContent;
    }


    public boolean isOnTheFly() {
        return cbEnabled.isSelected();
    }

    public void setOnTheFly(boolean value) {
        cbEnabled.setSelected(value);
    }


    public boolean isShowAdvancedMistakes() {
        return cbShowAdvanced.isSelected();
    }

    public void setShowAdvancedMistakes(boolean value) {
        cbShowAdvanced.setSelected(value);
    }


    public Set<String> getExclusions() {
        return new HashSet<>(lstExclusionsModel.getItems());
    }

    public void setExclusions(Set<String> exclusions) {
        lstExclusionsModel.removeAll();
        lstExclusionsModel.addAll(0, exclusions.stream().sorted().collect(Collectors.toList()));
    }


    public int getCacheLifespan() {
        return getNumber(tbCacheLifespan, SettingsService.DEFAULT_CACHE_LIFESPAN);
    }

    public void setCacheLifespan(int value) {
        tbCacheLifespan.setText(String.valueOf(value));
    }


    public int getParallelRequests() {
        return getNumber(tbParallelRequests, SettingsService.DEFAULT_PARALLEL_REQUESTS);
    }

    public void setParallelRequests(int value) {
        tbParallelRequests.setText(String.valueOf(value));
    }


    public String getGrammarlyClientType() {
        return tbGrammarlyClientType.getText();
    }

    public void setGrammarlyClientType(String value) {
        tbGrammarlyClientType.setText(value);
    }


    public String getGrammarlyClientVersion() {
        return tbGrammarlyClientVersion.getText();
    }

    public void setGrammarlyClientVersion(String value) {
        tbGrammarlyClientVersion.setText(value);
    }


    public String getGrammarlyClientOrigin() {
        return tbGrammarlyClientOrigin.getText();
    }

    public void setGrammarlyClientOrigin(String value) {
        tbGrammarlyClientOrigin.setText(value);
    }


    public String getGrammarlyUserAgent() {
        return tbGrammarlyUserAgent.getText();
    }

    public void setGrammarlyUserAgent(String value) {
        tbGrammarlyUserAgent.setText(value);
    }


    public String getGrammarlyCookie() {
        return tbGrammarlyCookie.getText();
    }

    public void setGrammarlyCookie(String value) {
        tbGrammarlyCookie.setText(value);
    }


    private static int getNumber(JBTextField source, int defaultValue) {
        try {
            return Integer.parseInt(source.getText().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /* -----------
       UI creation
       ----------- */

    private void createUi() {

        // Spellcheck settings

        cbEnabled = new JBCheckBox("Run spellcheck on the fly?");
        cbShowAdvanced = new JBCheckBox("Show advanced (facultative) mistake alerts?");

        JLabel lblExclusions = new JLabel("Manage exclusions");
        lblExclusions.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JPanel pnlExclusions = createExclusionsPanel();

        JPanel pnlSpellcheckSettings = createBox(
                "Spellcheck Settings",
                cbEnabled,
                cbShowAdvanced,
                lblExclusions,
                pnlExclusions);

        // Service settings

        tbCacheLifespan = new LabelledField("Cache results lifespan (min): ", NUMERIC_VERIFIER);
        tbParallelRequests = new LabelledField("Max parallel threads in async mode: ", NUMERIC_VERIFIER);

        JPanel pnlServiceSettings = createPanel(
                "Service Settings",
                tbCacheLifespan,
                tbParallelRequests);


        // Grammarly settings

        tbGrammarlyClientType = new LabelledField("Client type: ", NOT_BLANK_VERIFIER);
        tbGrammarlyClientVersion = new LabelledField("Client version: ", NOT_BLANK_VERIFIER);
        tbGrammarlyClientOrigin = new LabelledField("Client origin: ", NOT_BLANK_VERIFIER);
        tbGrammarlyUserAgent = new LabelledField("User agent: ", NOT_BLANK_VERIFIER);
        tbGrammarlyCookie = new LabelledField("Persistent cookie: ", NOT_BLANK_VERIFIER);

        JPanel pnlGrammarly = createPanel(
                "Grammarly Integration",
                tbGrammarlyClientType,
                tbGrammarlyClientVersion,
                tbGrammarlyClientOrigin,
                tbGrammarlyUserAgent,
                tbGrammarlyCookie);

        // Assembly

        pnlContent = createBox(null, pnlSpellcheckSettings, pnlServiceSettings, pnlGrammarly);
    }

    private static JPanel createBox(String title, JComponent... children) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        if (StringUtils.isNotBlank(title)) {
            panel.setBorder(IdeBorderFactory.createTitledBorder(title));
        }
        Arrays.stream(ArrayUtils.nullToEmpty(children))
                .map(JComponent.class::cast)
                .forEach(component -> {
                    component.setAlignmentX(Component.LEFT_ALIGNMENT);
                    panel.add(component);
                });
        return panel;
    }

    private static JPanel createPanel(String title, JComponent... children) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        if (StringUtils.isNotBlank(title)) {
            panel.setBorder(IdeBorderFactory.createTitledBorder(title));
        }
        GridBagConstraints constraints = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0);
        for (JComponent child : children) {
            LabelledField field = (LabelledField) child;

            constraints.gridx = 0;
            constraints.weightx = 0;
            constraints.fill = GridBagConstraints.NONE;
            panel.add(new JLabel(field.getLabel()), constraints);

            constraints.weightx = 1;
            constraints.gridx = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            panel.add(field, constraints);

            constraints.gridy++;
        }
        return panel;
    }

    private JPanel createExclusionsPanel() {
        lstExclusionsModel = new CollectionListModel<>();
        JBList<String> lstExclusions = new JBList<>(lstExclusionsModel);
        lstExclusions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ToolbarDecorator tlbExclusions = ToolbarDecorator.createDecorator(lstExclusions).disableUpDownActions();
        tlbExclusions
                .setAddAction(button -> {
                    String value = Messages.showInputDialog(
                            lstExclusions,
                            "Enter an excluded word or phrase",
                            "Add Exclusion",
                            null,
                            StringUtils.EMPTY,
                            EXCLUSIONS_VALIDATOR);
                    if (StringUtils.isEmpty(value)) {
                        return;
                    }
                    lstExclusionsModel.add(value.trim());
                    lstExclusionsModel.sort(Comparator.naturalOrder());
                })
                .setRemoveAction(anActionButton -> {
                    int selectedIndex = lstExclusions.getSelectedIndex();
                    lstExclusionsModel.remove(lstExclusions.getSelectedValue());
                    if (selectedIndex < lstExclusionsModel.getSize()) {
                        lstExclusions.setSelectedIndex(selectedIndex);
                    } else if (lstExclusionsModel.getSize() > 0) {
                        lstExclusions.setSelectedIndex(0);
                    }
                });
        return tlbExclusions.createPanel();
    }

    /* ---------------
       Utility classes
       --------------- */

    private static class ExclusionInputValidator implements InputValidator {

        @Override
        public boolean checkInput(String inputString) {
            return StringUtils.isNotBlank(inputString);
        }

        @Override
        public boolean canClose(String inputString) {
            return true;
        }
    }

    private static class LabelledField extends JBTextField {
        @Getter
        private final String label;

        public LabelledField(String label, InputVerifier verifier) {
            super();
            setInputVerifier(verifier);
            setColumns(40);
            this.label = label;
        }
    }

    private static class NumericInputVerifier extends InputVerifier {

        @Override
        public boolean verify(JComponent jComponent) {
            return jComponent instanceof JBTextField
                    && StringUtils.isNumeric(((JBTextField) jComponent).getText());
        }
    }

    private static class NonBlankInputVerifier extends InputVerifier {
        @Override
        public boolean verify(JComponent jComponent) {
            return jComponent instanceof JBTextField
                    && StringUtils.isNotBlank(((JBTextField) jComponent).getText());
        }
    }
}
