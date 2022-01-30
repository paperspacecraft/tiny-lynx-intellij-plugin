package com.paperspacecraft.intellij.plugin.tinylynx.inspection.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.StringHelper;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public abstract class ReplacementQuickFix implements LocalQuickFix {

    private final String replacement;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getName() {
        if (ReplacementUtil.isAppendable(replacement)) {
            return String.format("Append \"%s\"", replacement.trim());
        } else if (ReplacementUtil.isPrependable(replacement)) {
            return String.format("Prepend \"%s\"", replacement.trim());
        } else if (ReplacementUtil.isInsertable(replacement)) {
            return String.format("Insert \"%s\"", replacement.trim());
        }
        return String.format("Replace with \"%s\"", replacement);
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getFamilyName() {
        return "Modify";
    }

    protected String getReplacingText(ProblemDescriptor descriptor) {
        return getReplacingText(descriptor.getPsiElement(), descriptor.getTextRangeInElement());
    }

    protected String getReplacingText(PsiElement element, TextRange range) {
        String oldText = range.substring(element.getText());
        String effectiveReplacement = ReplacementUtil.getFullValue(oldText, replacement);
        TextRange effectiveRange = range;
        if (!Character.isLetterOrDigit(effectiveReplacement.charAt(0))) {
            effectiveRange = extendWithLeadingSpaces(element.getText(), range);
        }
        return effectiveRange.replace(element.getText(), effectiveReplacement);
    }

    private TextRange extendWithLeadingSpaces(String text, TextRange range) {
        return new TextRange(StringHelper.getFarthestSpaceToTheLeft(text, range.getStartOffset()), range.getEndOffset());
    }
}
