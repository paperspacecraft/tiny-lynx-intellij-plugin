package com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocToken;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckAlert;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class JavaDocInspectable implements Inspectable {

    private static final CommentInspectable EMPTY_COMMENT = new CommentInspectable(StringUtils.EMPTY);
    private static final String[] IN_COMMENT_ELEMENTS = new String[] {"code", "link"};

    @Getter
    private final PsiElement element;

    private CommentInspectable comment;

    @Getter
    private final List<Inspectable> tags = new ArrayList<>();

    public JavaDocInspectable(PsiElement element) {
        this.element = element;
        comment = EMPTY_COMMENT;

        if (ArrayUtils.isEmpty(element.getChildren())) {
            return;
        }
        List<PsiElement> commentElements = new ArrayList<>();

        for (PsiElement child : element.getChildren()) {
            if (child instanceof PsiDocToken && "DOC_COMMENT_DATA".equals(((PsiDocToken) child).getTokenType().toString())) {
                commentElements.add(child);
            } else if (child instanceof PsiDocTag && StringUtils.equalsAny(((PsiDocTag) child).getName(), IN_COMMENT_ELEMENTS)) {
                commentElements.add(child);
            } else if (child instanceof PsiDocTag) {
                tags.add(new JavaDocTagInspectable(child));
            }
        }

        if (!commentElements.isEmpty()) {
            comment = new CommentInspectable(commentElements);
        }
    }

    @Override
    public String getText() {
        return comment.getText();
    }

    @Override
    public int toPositionInElement(int position) {
        return comment.toPositionInElement(position);
    }

    @Override
    public TextRange toRangeInElement(TextRange range) {
        return comment.toRangeInElement(range);
    }

    @Override
    public boolean isAlertRelevant(SpellcheckAlert alert) {
        return comment.isAlertRelevant(alert);
    }

    @Override
    public LocalQuickFix getQuickReplacement(String replacement) {
        return comment.getQuickReplacement(replacement);
    }
}
