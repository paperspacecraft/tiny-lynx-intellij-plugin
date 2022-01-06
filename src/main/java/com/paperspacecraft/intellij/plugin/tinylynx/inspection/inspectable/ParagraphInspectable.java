package com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.StringHelper;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.quickfix.AbstractQuickFix;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckAlert;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.intellij.plugins.markdown.lang.psi.MarkdownPsiElementFactory;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
@Getter
public class ParagraphInspectable implements Inspectable {

    private final PsiElement element;

    @Override
    public boolean isAlertRelevant(SpellcheckAlert alert) {
        return !StringHelper.isWithinCodeSnippet(getText(), alert.getRange())
                && !StringHelper.isWithinEmphasis(getText(), alert.getRange());
    }

    @Override
    public LocalQuickFix getReplacement(String replacement) {
        return new ParagraphQuickFix(replacement);
    }

    private static class ParagraphQuickFix extends AbstractQuickFix {

        public ParagraphQuickFix(String replacement) {
            super(replacement);
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement newElement = MarkdownPsiElementFactory.createTextElement(project, getReplacingText(descriptor));
            descriptor.getPsiElement().replace(newElement);
        }
    }
}
