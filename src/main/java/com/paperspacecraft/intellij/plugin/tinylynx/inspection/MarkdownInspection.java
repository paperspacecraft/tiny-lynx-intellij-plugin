package com.paperspacecraft.intellij.plugin.tinylynx.inspection;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable.ParagraphInspectable;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.intellij.plugins.markdown.lang.psi.MarkdownElementVisitor;
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownHeaderImpl;
import org.intellij.plugins.markdown.lang.psi.impl.MarkdownParagraphImpl;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
class MarkdownInspection extends Inspection {

    MarkdownInspection(boolean refreshingMode) {
        super(refreshingMode);
    }

    @Override
    public @NotNull String getShortName() {
        return "tinylynx.markdown";
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new LocalMarkdownElementVisitor(holder, isOnTheFly);
    }

    @Override
    Inspection getRefreshingInspection() {
        return new MarkdownInspection(true);
    }

    @RequiredArgsConstructor
    private class LocalMarkdownElementVisitor extends MarkdownElementVisitor {

        private final ProblemsHolder holder;
        private final boolean isOnTheFly;

        @Override
        public void visitParagraph(@NotNull MarkdownParagraphImpl paragraph) {
            super.visitParagraph(paragraph);
            inspect(new ParagraphInspectable(paragraph), holder, isOnTheFly);
        }

        @Override
        public void visitHeader(@NotNull MarkdownHeaderImpl header) {
            super.visitHeader(header);
            inspect(new ParagraphInspectable(header), holder, isOnTheFly);
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            super.visitElement(element);
            if (element instanceof MarkdownHeaderImpl) {
                inspect(new ParagraphInspectable(element), holder, isOnTheFly);
            }
        }
    }
}
