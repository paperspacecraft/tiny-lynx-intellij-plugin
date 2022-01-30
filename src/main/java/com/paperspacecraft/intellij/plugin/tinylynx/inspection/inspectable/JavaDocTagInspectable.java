package com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.StringHelper;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.quickfix.ReplacementQuickFix;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@Getter
class JavaDocTagInspectable extends CommentInspectable {

    private static final String TAG_RETURN = "@return ";
    private static final String TAG_CUSTOM = "@custom ";

    public JavaDocTagInspectable(PsiElement element) {
        super(element);
        extractTagHeading();
    }

    @Override
    public LocalQuickFix getReplacement(String replacement) {
        return new JavaDocTagQuickFix(this, replacement);
    }

    private void extractTagHeading() {
        Chunk existingTagChunk = getChunks()
                .stream()
                .filter(chunk -> chunk.isText() && chunk.getContent().startsWith("@"))
                .findFirst()
                .orElse(null);
        if (existingTagChunk == null) {
            return;
        }

        String content = existingTagChunk.getContent();
        String heading;
        if (StringUtils.startsWithAny(content, TAG_RETURN, TAG_CUSTOM)) {
            int positionAfterSpace = StringHelper.getPositionAfterSpace(content);
            if (positionAfterSpace > 0) {
                heading = content.substring(0, positionAfterSpace);
                content = content.substring(positionAfterSpace);
            } else {
                heading = content;
                content = null;
            }
        } else {
            int positionAfterSecondSpace = StringHelper.getPositionAfterSecondSpace(content);
            if (positionAfterSecondSpace > 0) {
                heading = content.substring(0, positionAfterSecondSpace);
                content = content.substring(positionAfterSecondSpace);
            } else {
                heading = content;
                content = null;
            }
        }

        if (content == null) {
            existingTagChunk.setText(false);
            return;
        }

        Chunk newHeadingChunk = new Chunk(
                heading,
                new TextRange(
                        existingTagChunk.getRange().getStartOffset(),
                        existingTagChunk.getRange().getStartOffset() + heading.length()),
                false);
        int positionOfTagHeadingChunk = getChunks().indexOf(existingTagChunk);
        getChunks().add(positionOfTagHeadingChunk, newHeadingChunk);
        existingTagChunk.setText(true);
        existingTagChunk.setContent(content);
        existingTagChunk.setRange(new TextRange(
                existingTagChunk.getRange().getStartOffset() + heading.length(),
                existingTagChunk.getRange().getEndOffset()));
    }

    private static class JavaDocTagQuickFix extends ReplacementQuickFix {

        private final int parentOffset;

        public JavaDocTagQuickFix(Inspectable inspectable, String replacement) {
            super(replacement);
            this.parentOffset = inspectable.getElement().getStartOffsetInParent();
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement javaDocTag = descriptor.getPsiElement();
            PsiElement javaDoc = javaDocTag.getParent();

            TextRange tagRangeRelativeToJavaDoc = descriptor.getTextRangeInElement().shiftRight(parentOffset);
            String newText = getReplacingText(javaDoc, tagRangeRelativeToJavaDoc);
            PsiElement newElement = PsiElementFactory
                    .getInstance(project)
                    .createCommentFromText(newText, null);

            javaDoc.replace(newElement);
        }
    }
}
