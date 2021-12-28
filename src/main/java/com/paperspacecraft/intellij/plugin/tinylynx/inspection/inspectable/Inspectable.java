package com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckAlert;
import org.apache.commons.lang.StringUtils;

public interface Inspectable {

    /* --------------
       Main accessors
       -------------- */

    PsiElement getElement();

    default String getText() {
        return getElement().getText();
    }

    default boolean isEmpty() {
        return getText() == null || StringUtils.isEmpty(getText());
    }

    /* -----------
       Positioning
       ----------- */

    default int toPositionInElement(int position) {
        return toRangeInElement(new TextRange(position, position + 1)).getStartOffset();
    }

    default TextRange toRangeInElement(TextRange range) {
        return range;
    }

    /* -----------------
       Alerts management
       ----------------- */

    default boolean isAlertRelevant(SpellcheckAlert alert) {
        return true;
    }

    default boolean canHaveQuickFixes(SpellcheckAlert alert) {
        return true;
    }

    LocalQuickFix getQuickReplacement(String replacement);
}
