package com.paperspacecraft.intellij.plugin.tinylynx.inspection;

import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.smartPointers.Identikit;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

class LightIdentity {
    private final VirtualFile virtualFile;
    private final Identikit identikit;
    private final int positionHash;

    public LightIdentity(PsiElement element, PsiFile containingFile) {
        virtualFile = containingFile != null
                ? containingFile.getViewProvider().getVirtualFile()
                : null;
        identikit = Identikit.fromPsi(element, LanguageUtil.getRootLanguage(element));
        positionHash = getPositionHash(element);
    }

    private static int getPositionHash(PsiElement element) {
        List<Integer> positions = new ArrayList<>();
        PsiElement current = element;
        while (current.getParent() != null) {
            positions.add(ArrayUtils.indexOf(current.getParent().getChildren(), current));
            if (current.getParent().equals(element.getContainingFile())) {
                break;
            }
            current = current.getParent();
        }
        return Arrays.deepHashCode(positions.toArray(Integer[]::new));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LightIdentity that = (LightIdentity) o;

        if (positionHash != that.positionHash) return false;
        if (!Objects.equals(virtualFile, that.virtualFile)) return false;
        return identikit.equals(that.identikit);
    }

    @Override
    public int hashCode() {
        int result = virtualFile != null ? virtualFile.hashCode() : 0;
        result = 31 * result + identikit.hashCode();
        result = 31 * result + positionHash;
        return result;
    }
}
