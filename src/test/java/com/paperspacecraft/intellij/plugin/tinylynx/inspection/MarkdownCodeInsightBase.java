package com.paperspacecraft.intellij.plugin.tinylynx.inspection;

import com.intellij.lang.Language;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;
import org.apache.commons.io.IOUtils;
import org.intellij.plugins.markdown.lang.psi.MarkdownPsiElement;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Predicate;

public abstract class MarkdownCodeInsightBase extends JavaCodeInsightFixtureTestCase {

    MarkdownPsiElement getParagraph(int ordinal) throws IOException {
        PsiFile psiFile = initFile();
        PsiElement psiElement = psiFile.getChildren()[0].getChildren()[ordinal];
        Assert.assertTrue(psiElement instanceof MarkdownPsiElement);
        return (MarkdownPsiElement) psiElement;
    }

    private PsiFile initFile() throws IOException {
        URL resource = getClass().getClassLoader().getResource("MarkdownFile.md");
        assert  resource != null;
        String testClassText = IOUtils.toString(resource, StandardCharsets.UTF_8);
        return PsiFileFactory
                .getInstance(getProject())
                .createFileFromText(
                        Objects.requireNonNull(Language.findLanguageByID("Markdown")),
                        testClassText);
    }

}
