package com.paperspacecraft.intellij.plugin.tinylynx.inspection;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.javadoc.PsiDocComment;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable.CommentInspectable;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable.Inspectable;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable.JavaDocInspectable;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable.LiteralInspectable;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
class JavaInspection extends Inspection {

    private static final int MIN_VIABLE_LITERAL_LENGTH = 4;

    public JavaInspection(boolean refreshingMode) {
        super(refreshingMode);
    }

    @Override
    public @NotNull String getShortName() {
        return "tinylynx.java";
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new LocalJavaElementVisitor(holder, isOnTheFly);
    }

    @Override
    Inspection getRefreshingInspection() {
        return new JavaInspection(true);
    }

    @RequiredArgsConstructor
    private class LocalJavaElementVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private final boolean isOnTheFly;

        @Override
        public void visitComment(@NotNull PsiComment comment) {
            super.visitComment(comment);
            if (comment instanceof PsiDocComment) {
                JavaDocInspectable javaDocInspectable = new JavaDocInspectable(comment);
                inspect(javaDocInspectable, holder, isOnTheFly);
                for (Inspectable tag : javaDocInspectable.getTags()) {
                    inspect(tag, holder, isOnTheFly);
                }
            } else {
                CommentInspectable commentInspectable = new CommentInspectable(comment);
                if (!commentInspectable.isEmpty()) {
                    inspect(new CommentInspectable(comment), holder, isOnTheFly);
                }
            }
        }

        @Override
        public void visitLiteralExpression(PsiLiteralExpression expression) {
            super.visitLiteralExpression(expression);
            String text = expression.getText();
            int expressionLength = StringUtils.length(text);
            if (StringUtils.startsWith(text, "\"") && StringUtils.endsWith(text, "\"")) {
                expressionLength -= 2;
            }
            if (expressionLength < MIN_VIABLE_LITERAL_LENGTH) {
                return;
            }
            if (isOnTheFly && StringHelper.isOneWord(StringUtils.strip(text, "\""))) {
                return;
            }
            inspect(new LiteralInspectable(expression), holder, isOnTheFly);
        }
    }
}
