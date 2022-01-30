package com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.StringHelper;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.quickfix.ReplacementQuickFix;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckAlert;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.intellij.plugins.markdown.lang.psi.MarkdownPsiElementFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Getter
public class ParagraphInspectable implements Inspectable {

    private final PsiElement element;

    @Override
    public boolean isAlertRelevant(SpellcheckAlert alert) {
        return !StringHelper.isWithinCodeSnippet(getText(), alert.getRange())
                && !StringHelper.isWithinDelimiters(getText(), alert.getRange(), "*")
                && !isWithinHyperlink(alert.getRange());
    }

    private boolean isWithinHyperlink(TextRange alertRange) {
        if (!Stream.of("[", "]", "(", ")").allMatch(getText()::contains)) {
            return false;
        }
        List<TextRange> bracketRanges = StringHelper.getDelimitedRanges(getText(), "(", ")");
        TextRange matchingBracketRange = bracketRanges.stream().filter(r -> r.contains(alertRange)).findFirst().orElse(null);
        if (matchingBracketRange == null) {
            return false;
        }
        List<TextRange> squareBracketRanges = StringHelper.getDelimitedRanges(getText(), "[", "]");
        return squareBracketRanges.stream().anyMatch(r -> r.getEndOffset() == matchingBracketRange.getStartOffset());
    }

    @Override
    public LocalQuickFix getReplacement(String replacement) {
        return new ParagraphQuickFix(replacement);
    }

    private static class ParagraphQuickFix extends ReplacementQuickFix {

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
