package com.paperspacecraft.intellij.plugin.tinylynx.inspection;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import lombok.experimental.UtilityClass;

import java.util.Objects;

@UtilityClass
class InjectedFileHelper {

    private static final Logger LOG = Logger.getInstance(InjectedFileHelper.class);

    public static boolean isInjected(Project project, PsiFile value) {
        boolean result = InjectedLanguageManager.getInstance(project).isInjectedFragment(value);
        if (result) {
            return true;
        }
        FileViewProvider viewProvider = value.getViewProvider();
        PsiFile topFileInBaseLanguage = viewProvider.getPsi(viewProvider.getBaseLanguage());
        return !Objects.equals(topFileInBaseLanguage, value);
    }

    public static PsiFile getInspectableCopy(Project project, PsiFile original) {
        try {
            return PsiFileFactory
                    .getInstance(project)
                    .createFileFromText(
                            original.getName(),
                            original.getFileType(),
                            original.getText(),
                            original.getModificationStamp(),
                            true);
        } catch (AssertionError e) {
            LOG.warn("Could not create an inspectable copy: " + e.getMessage());
        }
        return null;
    }
}
