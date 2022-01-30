package com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiWhiteSpace;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.StringHelper;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.quickfix.ReplacementQuickFix;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckAlert;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class CommentInspectable implements Inspectable {

    @Getter
    private PsiElement element;

    @Getter(value = AccessLevel.PACKAGE)
    private List<Chunk> chunks;

    private boolean isSequenceOfSingleLines;


    /* --------------
       Initialization
       -------------- */

    public CommentInspectable(PsiElement element) {
        if (isSingleLine(element) && !hasUpperSinglelineSiblings(element)) {
            List<PsiElement> commentSiblings = collectSinglelineSiblings(element);
            if (commentSiblings.size() > 1) {
                isSequenceOfSingleLines = true;
                init(commentSiblings, element.getParent());
            } else {
                init(commentSiblings.get(0), commentSiblings.get(0).getText());
            }
        } else if (!isSingleLine(element)) {
            init(element, element.getText());
        }
        // isSingleLine() && hasUpperSiblings() => irrelevant; do not initialize
    }

    CommentInspectable(String text) {
        init(null, text);
    }

    CommentInspectable(List<PsiElement> elements) {
        init(elements, elements.get(0).getParent()); // Needed to provide a non-null "identity" for e.g. a debouncer
    }

    private void init(PsiElement element, String text) {
        this.element = element;
        this.chunks = split(text);
    }

    private void init(List<PsiElement> elements, PsiElement parent) {
        this.element = parent;
        this.chunks = elements
                .stream()
                .flatMap(elt -> split(elt.getText(), elt.getStartOffsetInParent()).stream())
                .collect(Collectors.toList());
    }

    /* ------------------------
       Public interface methods
       ------------------------ */

    @Override
    public String getText() {
        if (chunks == null) {
            return StringUtils.EMPTY;
        }
        return chunks.stream().filter(Chunk::isText).map(Chunk::getContent).collect(Collectors.joining(StringUtils.SPACE));
    }

    @Override
    public TextRange toRangeInElement(TextRange range) {
        int position = range.getStartOffset();
        if (position < 0 || chunks.isEmpty()) {
            return TextRange.EMPTY_RANGE;
        }

        Queue<Chunk> haystack = new LinkedList<>(chunks);
        int currentStart = 0;
        int currentEnd;
        while (!haystack.isEmpty()) {
            Chunk currentChunk = haystack.remove();
            if (!currentChunk.isText) {
                continue;
            }
            currentEnd = currentStart + currentChunk.getContent().length();
            if (position >= currentStart && position < currentEnd) {
                int delta = position - currentStart;
                int newRangeStart = currentChunk.getRange().getStartOffset() + delta;
                int newRangeEnd = Math.min(newRangeStart + range.getLength(), currentChunk.getRange().getEndOffset());
                return new TextRange(newRangeStart, newRangeEnd);
            }
            currentStart = currentEnd + 1; // 1 is for the space delimiter between chunks
        }
        return TextRange.EMPTY_RANGE;
    }


    @Override
    public boolean isAlertRelevant(SpellcheckAlert alert) {
        return !StringHelper.isWithinJavadocTag(getText(), alert.getRange());
    }

    @Override
    public LocalQuickFix getReplacement(String replacement) {
        return new CommentQuickFix(replacement);
    }

    @Override
    public boolean canHaveReplacements(SpellcheckAlert alert) {
        return !isSequenceOfSingleLines;
    }

    /* ---------------
       Utility methods
       --------------- */

    private static boolean isSingleLine(PsiElement element) {
        return element.getText().trim().startsWith("//");
    }

    private static boolean hasUpperSinglelineSiblings(PsiElement element) {
        PsiElement current = element.getPrevSibling();
        while (current != null) {
            if (!(current instanceof PsiWhiteSpace) && !(current instanceof PsiComment)) {
                return false;
            }
            if (current instanceof PsiComment) {
                return true;
            }
            current = current.getPrevSibling();
        }
        return false;
    }

    private static List<PsiElement> collectSinglelineSiblings(PsiElement element) {
        List<PsiElement> result = new ArrayList<>();
        result.add(element);
        PsiElement current = element.getNextSibling();
        while (current != null) {
            if (!(current instanceof PsiWhiteSpace) && !(current instanceof PsiComment)) {
                break;
            }
            if (current instanceof PsiComment) {
                result.add(current);
            }
            current = current.getNextSibling();
        }
        return result;
    }

    private static List<Chunk> split(String value) {
        return split(value, 0);
    }

    private static List<Chunk> split(String value, int offset) {
        List<Chunk> result = new ArrayList<>();
        if (StringUtils.isEmpty(value)) {
            return result;
        }

        StringHelper.WhitespaceMatcher matcher = StringHelper.newWhitespaceMatcher(value);
        int lastVisitedPos = 0;
        while (matcher.find()) {
            if (matcher.getStart() > lastVisitedPos) {
                TextRange newTextRange = new TextRange(lastVisitedPos, matcher.getStart());
                Chunk newTextChunk = new Chunk(
                        newTextRange.substring(value),
                        newTextRange.shiftRight(offset),
                        true);
                result.add(newTextChunk);
            }
            TextRange newWhitespaceRange = new TextRange(matcher.getStart(), matcher.getEnd());
            Chunk newWhitespaceChunk = new Chunk(
                    newWhitespaceRange.substring(value),
                    newWhitespaceRange.shiftRight(offset),
                    false);
            result.add(newWhitespaceChunk);
            lastVisitedPos = matcher.getEnd();
        }
        if (lastVisitedPos < value.length()) {
            TextRange newTextRange = new TextRange(lastVisitedPos, value.length());
            Chunk newTextChunk = new Chunk(
                    newTextRange.substring(value),
                    newTextRange.shiftRight(offset),
                    true);
            result.add(newTextChunk);
        }
        return result;
    }

    /* ---------------
       Utility classes
       --------------- */

    @AllArgsConstructor
    @Getter
    @Setter
    static class Chunk {

        private String content;

        private TextRange range;

        private boolean isText;
    }

    private static class CommentQuickFix extends ReplacementQuickFix {

        public CommentQuickFix(String replacement) {
            super(replacement);
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement newElement = PsiElementFactory
                    .getInstance(project)
                    .createCommentFromText(getReplacingText(descriptor), null);
            descriptor.getPsiElement().replace(newElement);
        }
    }
}
