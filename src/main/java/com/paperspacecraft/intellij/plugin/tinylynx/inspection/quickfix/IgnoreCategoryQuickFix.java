package com.paperspacecraft.intellij.plugin.tinylynx.inspection.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.paperspacecraft.intellij.plugin.tinylynx.settings.SettingsService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class IgnoreCategoryQuickFix implements LocalQuickFix {

    public static final String PREFIX_CATEGORY = "category:";

    private final String category;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getFamilyName() {
        return "Save category \"" + category + "\" to exclusions list";
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        SettingsService.getInstance(descriptor.getPsiElement().getProject()).getExclusionSet().add(PREFIX_CATEGORY + category);
    }
}
