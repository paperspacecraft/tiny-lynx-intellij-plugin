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
import org.junit.Assert;

import java.io.IOException;

public class JavaProblemRegisteringTest extends JavaCodeInsightBase implements SpellcheckResultProvider {

    public void testJavadocProblemRegistering() throws IOException {
        JavaDocInspectable javaDocInspectable = new JavaDocInspectable(getJavadoc("method1"));
        String[] offenderWords = new String[] {"method", "parameter", "String"};

        SpellcheckResult spellcheckResult = doBasicCheck(javaDocInspectable, offenderWords);

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
        SpellcheckResult result = doBasicCheck(commentInspectable, offenderWords);
        Assert.assertEquals(offenderWords.length, result.getAlerts().size());
    }

    public void testSingleLineSeriesProblemRegistering() throws IOException {
        CommentInspectable commentInspectable = new CommentInspectable(getComment("Short comment"));
        String[] offenderWords = new String[] {"Short", "different", "lines"};
        SpellcheckResult result = doBasicCheck(commentInspectable, offenderWords);
        Assert.assertEquals(offenderWords.length, result.getAlerts().size());

        commentInspectable = new CommentInspectable(getComment("Comment after"));
        offenderWords = new String[] {"the", "whole", "class"};
        result = doBasicCheck(commentInspectable, offenderWords);
        Assert.assertEquals(offenderWords.length, result.getAlerts().size());
    }

    public void testMultilineCommentProblemRegistering() throws IOException {
        CommentInspectable commentInspectable = new CommentInspectable(getComment("Multiline comment"));
        String[] offenderWords = new String[] {"Multiline", "another"};
        SpellcheckResult result = doBasicCheck(commentInspectable, offenderWords);
        Assert.assertEquals(offenderWords.length, result.getAlerts().size());
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
}
