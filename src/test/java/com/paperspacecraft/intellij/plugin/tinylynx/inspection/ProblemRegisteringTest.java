package com.paperspacecraft.intellij.plugin.tinylynx.inspection;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.openapi.util.TextRange;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable.CommentInspectable;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable.Inspectable;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable.JavaDocInspectable;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckResult;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckAlert;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProblemRegisteringTest extends JavaCodeInsightBase {

    public void testJavadocProblemRegistering() throws IOException {
        JavaDocInspectable javaDocInspectable = new JavaDocInspectable(getJavadoc("method1"));
        String[] offenderWords = new String[] {"method", "parameter", "String"};

        SpellcheckResult spellcheckResult = doCommentCheck(javaDocInspectable, offenderWords);

        InspectionManager inspectionManager = new InspectionManagerEx(getProject());
        ProblemsHolder problemsHolder = new ProblemsHolder(inspectionManager, javaDocInspectable.getElement().getContainingFile(), true);
        JavaInspection inspection = new JavaInspection();
        inspection.registerProblems(javaDocInspectable, problemsHolder, spellcheckResult, true);
        Assert.assertEquals(offenderWords.length, problemsHolder.getResultCount());
        for (int i = 0; i < offenderWords.length; i++) {
            Assert.assertTrue(problemsHolder.getResultsArray()[i].toString().contains("Tactical mistake: " + offenderWords[i]));
        }
    }

    public void testJavadocTagProblemRegistering() throws IOException {
        JavaDocInspectable javaDocInspectable = new JavaDocInspectable(getJavadoc("method1"));
        doTagCheck(javaDocInspectable, 0, "description");
        doTagCheck(javaDocInspectable, 2, "typed");
        doTagCheck(javaDocInspectable, 3, "every time");
    }

    public void testSingleLineCommentProblemRegistering() throws IOException {
        CommentInspectable commentInspectable = new CommentInspectable(getComment("One-line"));
        String[] offenderWords = new String[] {"line", "comment"};
        doCommentCheck(commentInspectable, offenderWords);
    }

    public void testSingleLineSeriesProblemRegistering() throws IOException {
        CommentInspectable commentInspectable = new CommentInspectable(getComment("Short comment"));
        String[] offenderWords = new String[] {"Short", "different", "lines"};
        doCommentCheck(commentInspectable, offenderWords);

        commentInspectable = new CommentInspectable(getComment("Comment after"));
        offenderWords = new String[] {"the", "whole", "class"};
        doCommentCheck(commentInspectable, offenderWords);
    }

    public void testMultilineCommentProblemRegistering() throws IOException {
        CommentInspectable commentInspectable = new CommentInspectable(getComment("Multiline comment"));
        String[] offenderWords = new String[] {"Multiline", "another"};
        doCommentCheck(commentInspectable, offenderWords);
    }

    private void doTagCheck(JavaDocInspectable inspectable, int tagIndex, String offenderWord) {
        Inspectable javaDocTag = inspectable.getTags().get(tagIndex);

        SpellcheckResult spellcheckResult = getMockSpellcheckResult(javaDocTag.getText(), offenderWord);
        SpellcheckAlert alert = spellcheckResult.getAlerts().get(0);

        TextRange amendedRange = javaDocTag.toRangeInElement(alert.getRange());
        Assert.assertEquals(
                alert.getTitle(),
                amendedRange.substring(javaDocTag.getElement().getText()));
        Assert.assertEquals(
                alert.getTitle(),
                amendedRange.shiftRight(javaDocTag.getElement().getStartOffsetInParent()).substring(inspectable.getElement().getText()));

        InspectionManager inspectionManager = new InspectionManagerEx(getProject());
        ProblemsHolder problemsHolder = new ProblemsHolder(inspectionManager, inspectable.getElement().getContainingFile(), false);
        JavaInspection inspection = new JavaInspection();
        inspection.registerProblems(javaDocTag, problemsHolder, spellcheckResult, true);
        Assert.assertTrue(problemsHolder.getResults().get(0).toString().contains("Tactical mistake: " + offenderWord));
    }

    private SpellcheckResult doCommentCheck(Inspectable inspectable, String[] offenderWords) {
        SpellcheckResult spellcheckResult = getMockSpellcheckResult(inspectable.getText(), offenderWords);
        for (SpellcheckAlert alert : spellcheckResult.getAlerts()) {
            TextRange amendedRange = inspectable.toRangeInElement(alert.getRange());
            Assert.assertEquals(alert.getTitle(), amendedRange.substring(inspectable.getElement().getText()));
        }
        return spellcheckResult;
    }

    private static SpellcheckResult getMockSpellcheckResult(String text, String... offenderWords) {
        List<SpellcheckAlert> alerts = new ArrayList<>();
        for (String offenderWord : offenderWords) {
            int start = text.indexOf(offenderWord);
            int end = start + offenderWord.length();
            com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly.model.Alert alert = new com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly.model.Alert();
            alert.setGroup("Tactical");
            alert.setTitle(offenderWord);
            alert.setContent(text);
            alert.setStart(start);
            alert.setEnd(end);
            alert.setReplacements(new String[] {"replacement"});
            alerts.add(alert);
        }
        return new SpellcheckResult(text, alerts, StringUtils.EMPTY);
    }
}
