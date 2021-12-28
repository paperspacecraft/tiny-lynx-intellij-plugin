package com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable;

import com.intellij.psi.PsiComment;
import com.intellij.psi.javadoc.PsiDocComment;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.JavaCodeInsightBase;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

import java.io.IOException;

public class InspectableTest extends JavaCodeInsightBase {

    public void testParseJavadoc() throws IOException {
        PsiDocComment docComment = getJavadoc("method0");
        Assert.assertNotNull(docComment);

        JavaDocInspectable javaDocEntity = new JavaDocInspectable(docComment);

        Assert.assertEquals("Runs method 0 in {@link JavaClass}", javaDocEntity.getText());
    }

    public void testParseJavadocWithParams() throws IOException {
        PsiDocComment docComment = getJavadoc("method1");
        JavaDocInspectable javaDocEntity = new JavaDocInspectable(docComment);

        Assert.assertEquals("Runs method 1 in {@link JavaClass} with {@code param1} as a String parameter", javaDocEntity.getText());

        Assert.assertEquals("String parameter with a long description", javaDocEntity.getTags().get(0).getText());
        Assert.assertEquals(StringUtils.EMPTY, javaDocEntity.getTags().get(1).getText());
        Assert.assertEquals("{@code String}-typed object with a long description", javaDocEntity.getTags().get(2).getText());
        Assert.assertEquals("every time", javaDocEntity.getTags().get(3).getText());
    }

    public void testParseIncompleteJavadoc() throws IOException {
        PsiDocComment docComment = getJavadoc("method2");
        JavaDocInspectable javaDocEntity = new JavaDocInspectable(docComment);

        Assert.assertEquals(StringUtils.EMPTY, javaDocEntity.getText());
        Assert.assertEquals("String parameter with a long description", javaDocEntity.getTags().get(0).getText());
    }

    public void testParseSingleLineComment() throws IOException {
        PsiComment singleLineComment = getComment( "One-line");
        CommentInspectable commentInspectable = new CommentInspectable(singleLineComment);
        Assert.assertEquals("One-line comment", commentInspectable.getText());
    }

    public void testParseSingleLineSeriesComment() throws IOException {
        PsiComment singleLineComment = getComment( "Short comment");
        CommentInspectable commentInspectable = new CommentInspectable(singleLineComment);
        Assert.assertEquals("Short comment split into different lines", commentInspectable.getText());
    }

    public void testParseMultiLineComment() throws IOException {
        PsiComment multiLineComment = getComment( "Multiline");
        CommentInspectable commentInspectable = new CommentInspectable(multiLineComment);

        Assert.assertEquals("Multiline comment transferred to another line", commentInspectable.getText());
    }
}
