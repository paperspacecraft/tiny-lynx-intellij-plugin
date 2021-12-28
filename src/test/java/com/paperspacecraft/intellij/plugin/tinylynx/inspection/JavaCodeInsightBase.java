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
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Predicate;

public abstract class JavaCodeInsightBase extends JavaCodeInsightFixtureTestCase {

    /* ---------------
       Utility methods
       --------------- */

    protected PsiDocComment getJavadoc(String methodName) throws IOException {
        PsiFile classFile = initFile();
        Assert.assertNotNull(classFile);
        PsiMethod testMethod = getMethod(classFile, methodName);
        Assert.assertNotNull(testMethod);
        return testMethod.getDocComment();
    }

    protected PsiComment getComment(String withText) throws IOException {
        PsiFile classFile = initFile();
        ElementFinder commentFinder = new ElementFinder(elt ->
                elt instanceof PsiComment
                        && elt.getText().contains(withText));
        classFile.accept(commentFinder);
        return (PsiComment) commentFinder.getResult();
    }

    private PsiMethod getMethod(PsiFile classFile, String name) {
        ElementFinder methodFinder = new ElementFinder(elt -> elt instanceof PsiMethod && ((PsiMethod) elt).getName().equals(name));
        classFile.accept(methodFinder);
        return (PsiMethod) methodFinder.getResult();
    }

    private PsiFile initFile() throws IOException {
        URL resource = getClass().getClassLoader().getResource("JavadocClass.java");
        assert  resource != null;
        String testClassText = IOUtils.toString(resource, StandardCharsets.UTF_8);
        return PsiFileFactory
                .getInstance(getProject())
                .createFileFromText(
                        Objects.requireNonNull(Language.findLanguageByID("JAVA")),
                        testClassText);
    }

    /* ---------------
       Utility classes
       --------------- */

    private static class ElementFinder extends JavaRecursiveElementVisitor {
        private final Predicate<PsiElement> filter;
        private PsiElement result;

        public ElementFinder(Predicate<PsiElement> filter) {
            this.filter = filter;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            super.visitElement(element);
            if (result != null) {
                return;
            }
            if (filter.test(element)) {
                result = element;
            }
        }

        public PsiElement getResult() {
            return result;
        }
    }
}
