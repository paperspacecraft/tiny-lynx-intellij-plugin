package com.paperspacecraft.intellij.plugin.tinylynx.inspection.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.paperspacecraft.intellij.plugin.tinylynx.settings.SettingsService;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckAlert;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@RequiredArgsConstructor
public class IgnoreTextQuickFix implements LocalQuickFix {

    private final String text;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getFamilyName() {
        return "Save \"" + StringUtils.abbreviate(text, 20) + "\" to exclusions list";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        SettingsService.getInstance(descriptor.getPsiElement().getProject()).getExclusionSet().add(text);
    }

    public static boolean isApplicable(SpellcheckAlert alert) {
        if (alert == null) {
            return false;
        }
        if (StringUtils.contains(alert.getCategory(), "Punct")
            || StringUtils.equalsIgnoreCase(alert.getCategory(), "WordChoice")) {
            return false;
        }
        return ArrayUtils.isEmpty(alert.getReplacements()) || Arrays.stream(alert.getReplacements()).allMatch(ReplacementUtil::isStandalone);
    }
}
