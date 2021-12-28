package com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.quickfix.AbstractQuickFix;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckAlert;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@Getter
public class LiteralInspectable implements Inspectable {

    private final PsiElement element;

    @Override
    public boolean isAlertRelevant(SpellcheckAlert alert) {
        return !StringUtils.containsIgnoreCase(alert.getTitle(), "closing punct");
    }

    @Override
    public LocalQuickFix getQuickReplacement(String replacement) {
        return new LiteralQuickFix(replacement);
    }

    private static class LiteralQuickFix extends AbstractQuickFix {

        public LiteralQuickFix(String replacement) {
            super(replacement);
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement newElement = JavaPsiFacade.getElementFactory(project).createExpressionFromText(getReplacingText(descriptor), null);
            descriptor.getPsiElement().replace(newElement);
        }
    }

}
