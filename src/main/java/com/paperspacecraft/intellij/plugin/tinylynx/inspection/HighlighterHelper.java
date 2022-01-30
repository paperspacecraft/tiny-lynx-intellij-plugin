package com.paperspacecraft.intellij.plugin.tinylynx.inspection;

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.injected.editor.DocumentWindow;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.resolve.FileContextUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@UtilityClass
class HighlighterHelper {

    public static List<RangeHighlighter> getHighlighters(Project project, PsiFile file) {
        return getHighlighters(project, PsiDocumentManager.getInstance(project).getDocument(file), file);
    }

    private static List<RangeHighlighter> getHighlighters(Project project, Document document, PsiFile file) {
        Document effectiveDocument = document instanceof DocumentWindow
                ? ((DocumentWindow) document).getDelegate()
                : document;
        TextRange effectiveRange = document instanceof DocumentWindow
                ? Optional.ofNullable(FileContextUtil.getFileContext(file)).map(PsiElement::getTextRange).orElse(TextRange.EMPTY_RANGE)
                : new TextRange(0, document.getTextLength());
        List<RangeHighlighter> managedHighlighters = new ArrayList<>();
        DaemonCodeAnalyzerEx.processHighlights(
                effectiveDocument,
                project,
                null,
                effectiveRange.getStartOffset(),
                effectiveRange.getEndOffset(),
                info -> {
                    if (isManaged(info)) {
                        managedHighlighters.add(info.getHighlighter());
                    }
                    return true;
                });
        return managedHighlighters;
    }

    public static void purgeRedundantHighlighters(
            Project project,
            Document document,
            PsiFile file) {

        List<RangeHighlighter> managedHighlighters = getHighlighters(project, document, file);
        List<RangeHighlighter> uniqueHighlighters = managedHighlighters
                .stream()
                .map(HighlighterFacade::new)
                .filter(HighlighterFacade::isValid)
                .distinct()
                .map(HighlighterFacade::getValue)
                .collect(Collectors.toList());
        for(RangeHighlighter highlighter : managedHighlighters) {
            if (!uniqueHighlighters.contains(highlighter)) {
                highlighter.dispose();
            }
        }
        managedHighlighters.clear();
        uniqueHighlighters.clear();
    }

    private static boolean isManaged(HighlightInfo value) {
        Object tooltip = Optional
                .ofNullable(value.getHighlighter())
                .map(RangeHighlighter::getErrorStripeTooltip)
                .orElse(null);
        if (!(tooltip instanceof HighlightInfo)) {
            return false;
        }
        // We cannot rely on .getInspectionId() because sometimes is empty
        return StringUtils.contains(((HighlightInfo) tooltip).getDescription(), Inspection.BRAND_TOKEN);
    }

    @AllArgsConstructor
    @Getter
    private static class HighlighterFacade {
        private final RangeHighlighter value;

        public boolean isValid() {
            return getEndOffset() > getStartOffset();
        }

        private String getMessage() {
            if (!(value instanceof HighlightInfo)) {
                return StringUtils.EMPTY;
            }
            return ((HighlightInfo) value).getToolTip();
        }

        private int getStartOffset() {
            return value != null ? value.getStartOffset() : 0;
        }

        private int getEndOffset() {
            return value != null ? value.getEndOffset() : 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            HighlighterFacade that = (HighlighterFacade) o;
            return getMessage().equals(that.getMessage())
                    && getStartOffset() == that.getStartOffset()
                    && getEndOffset() == that.getEndOffset();
        }

        @Override
        public int hashCode() {
            int result = getMessage().hashCode();
            result = 31 * result + getStartOffset();
            result = 31 * result + getEndOffset();
            return result;
        }
    }
}
