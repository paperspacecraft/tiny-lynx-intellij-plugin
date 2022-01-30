package com.paperspacecraft.intellij.plugin.tinylynx.inspection;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.impl.HighlightInfoProcessor;
import com.intellij.codeInsight.daemon.impl.LocalInspectionsPass;
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.SmartPointerManager;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.inspectable.Inspectable;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.quickfix.IgnoreCategoryQuickFix;
import com.paperspacecraft.intellij.plugin.tinylynx.inspection.quickfix.IgnoreTextQuickFix;
import com.paperspacecraft.intellij.plugin.tinylynx.settings.SettingsService;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckAlert;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckResult;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

abstract class Inspection extends LocalInspectionTool {
    private static final Logger LOG = Logger.getInstance(Inspection.class);

    private static final String PROBLEM_FORMAT = "<html>%s<p style='font-size: x-small; padding:8px 0 0 0;'>Powered by Tiny Lynx</p></html>";

    private final boolean refreshingMode;

    Inspection() {
        this(false);
    }

    Inspection(boolean refreshingMode) {
        this.refreshingMode = refreshingMode;
    }

    /* -----------------------
       Common inspection logic
       ----------------------- */

    public void inspect(Inspectable target, ProblemsHolder holder, boolean isOnTheFly) {
        boolean canInspect = !isOnTheFly || SettingsService.getInstance(holder.getProject()).isOnTheFly();
        if (!canInspect || target.isEmpty()) {
            return;
        }
        SpellcheckService spellcheckService = SpellcheckService.getInstance(holder.getProject());

        // Refreshing mode: do only lookup in the cache
        if (refreshingMode) {
            SpellcheckResult result = spellcheckService.lookUp(target.getText());
            registerProblems(target, holder, result, true);
            return;
        }

        // Code analysis mode: only synchronous search via cache
        if (!isOnTheFly) {
            SpellcheckResult result = spellcheckService.checkSync(target.getText());
            registerProblems(target, holder, result, false);
        }

        PsiFile containingFile = getContainingFile(target.getElement());
        if (containingFile == null) {
            return;
        }

        // Async search is needed, but first, try to find a cached value
        SpellcheckResult existingResult = spellcheckService.lookUp(target.getText());
        if (!existingResult.isEmpty()) {
            registerProblems(target, holder, existingResult, true);
            return;
        }

        // Finally, start the asynchronous search
        Object identity = new LightIdentity(target.getElement(), containingFile);
        List<RangeHighlighter> highlighters = HighlighterHelper.getHighlighters(holder.getProject(), containingFile);
        spellcheckService
                .checkAsync(identity, target.getText())
                .thenAccept(result -> doAsyncInspectCallback(holder, target.getElement(), containingFile, result));
    }

    private void doAsyncInspectCallback(
            ProblemsHolder holder,
            PsiFile containingFile,
            SpellcheckResult result) {
        // Upon the promise-like resolution, trigger a second ("refreshing") pass
        // with only looking up in the cache
        if (CollectionUtils.isEmpty(result.getAlerts())) {
            return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
            if (holder.getProject().isDisposed()) {
                LOG.warn("Could not perform the inspection: project has been already disposed");
                return;
            }
            lightRefresh(
                    holder.getProject(),
                    getContainingFile(element),
                    (InspectionManagerEx) holder.getManager());
        }, ModalityState.any());

    }

    /* --------------------
       Registering problems
       -------------------- */

    void registerProblems(
            Inspectable target,
            ProblemsHolder holder,
            SpellcheckResult result,
            boolean isOnTheFly) {

        if (CollectionUtils.isEmpty(result.getAlerts())) {
            return;
        }
        SettingsService settings = SettingsService.getInstance(holder.getProject());
        List<SpellcheckAlert> processedAlerts = new ArrayList<>();

        for (SpellcheckAlert alert : result.getAlerts()) {

            boolean isSimilarProcessed = processedAlerts
                    .stream()
                    .anyMatch(other -> StringUtils.equals(alert.getFullMessage(), other.getFullMessage())
                            && alert.getRange().equals(other.getRange()));
            boolean isExcluded = SettingsService.getInstance(holder.getProject()).getExclusionSet()
                    .stream()
                    .anyMatch(exclusion -> StringUtils.equals(exclusion, alert.getContent())
                            || StringUtils.equalsIgnoreCase(exclusion, IgnoreCategoryQuickFix.PREFIX_CATEGORY + alert.getCategory()));

            if ((!alert.isFacultative() || settings.isShowAdvancedMistakes())
                    && !isSimilarProcessed
                    && target.isAlertRelevant(alert)
                    && !isExcluded) {
                registerProblem(target, holder, alert, isOnTheFly);
            }

            processedAlerts.add(alert);
        }
    }

    private void registerProblem(
            Inspectable target,
            ProblemsHolder holder,
            SpellcheckAlert alert,
            boolean isOnTheFly) {

        boolean canHaveReplacements = isOnTheFly && target.canHaveReplacements(alert);

        Stream<LocalQuickFix> ignores = Stream.of(
                IgnoreTextQuickFix.isApplicable(alert) ? new IgnoreTextQuickFix(alert.getContent()) : null,
                new IgnoreCategoryQuickFix(alert.getCategory()))
                .filter(Objects::nonNull);

        Stream<LocalQuickFix> replacements = canHaveReplacements && ArrayUtils.isNotEmpty(alert.getReplacements())
                ? Arrays.stream(alert.getReplacements()).filter(StringUtils::isNotBlank).map(target::getReplacement).filter(Objects::nonNull)
                : Stream.empty();

        LocalQuickFix[] quickFixes = Stream.concat(ignores, replacements).toArray(LocalQuickFix[]::new);

        String fullMessage = String.format(PROBLEM_FORMAT, alert.getFullMessage());
        holder.registerProblem(
                target.getElement(),
                fullMessage,
                alert.isFacultative() ? ProblemHighlightType.WEAK_WARNING : ProblemHighlightType.WARNING,
                target.toRangeInElement(alert.getRange()),
                quickFixes);
    }

    /* ----------------
       Refreshing logic
       ---------------- */

    abstract Inspection getRefreshingInspection();

    private void snapRefresh(
            Project project,
            PsiFile file,
            InspectionManagerEx manager,
            Inspection inspection) {

        if (file == null) {
            LOG.warn("Could not invoke the proper inspection context: file is null");
            return;
        }
        if (project.isDisposed()) {
            LOG.warn("Could not perform the refreshing inspection: project has been already disposed");
            return;
        }
        if (DumbService.isDumb(project)) {
            LOG.info("Cannot refresh because the IDE is in dumb mode");
            return;
        }
        Document document = PsiDocumentManager
                .getInstance(project)
                .getDocument(file);
        if (document == null) {
            return;
        }

        boolean isInjected = InjectedFileHelper.isInjected(project, file);
        PsiFile effectiveFile = isInjected ? InjectedFileHelper.getInspectableCopy(project, file) : file;
        if (effectiveFile == null) {
            return;
        }

        LocalInspectionsPass localInspectionsPass = new LocalInspectionsPass(
                file,
                document,
                0,
                document.getTextLength(),
                LocalInspectionsPass.EMPTY_PRIORITY_RANGE,
                true,
                HighlightInfoProcessor.getEmpty(),
                false);

        ProgressManager.getInstance().runProcess(
                () -> {
                    doSnapRefreshCallback(project, document, manager, localInspectionsPass, inspection);
                    if (isInjected) {
                        HighlighterHelper.purgeRedundantHighlighters(project, document, file);
                    } else {
                        ApplicationManager.getApplication().invokeLater(
                                () -> additionallyRefreshInjectedFiles(project, file, manager),
                                ModalityState.defaultModalityState());
                    }
                },
                new EmptyProgressIndicator());
    }

    private void doSnapRefreshCallback(
            Project project,
            Document document,
            InspectionManagerEx manager,
            LocalInspectionsPass inspectionsPass,
            Inspection inspection) {

        LocalInspectionToolWrapper inspectionToolWrapper = new LocalInspectionToolWrapper(inspection);
        inspectionsPass.doInspectInBatch(
                manager.createNewGlobalContext(),
                manager,
                Collections.singletonList(inspectionToolWrapper));

        UpdateHighlightersUtil.setHighlightersToEditor(
                project,
                document,
                0,
                document.getTextLength(),
                localInspectionsPass.getInfos(),
                null,
                Pass.LOCAL_INSPECTIONS);
    }

    /* ---------------
       Utility methods
       --------------- */

    private static PsiFile getContainingFile(PsiElement element) {
        try {
            return element.getContainingFile();
        } catch (PsiInvalidElementAccessException e) {
            return null;
        }
    }

    private static List<PsiFile> getInjected(Project project, PsiFile source) {
        List<PsiFile> result = new ArrayList<>();
        InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(project);
        PsiElementVisitor visitor = new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (!(element instanceof PsiLanguageInjectionHost) ) {
                    super.visitElement(element);
                    return;
                }
                var injectedPsiFilePairs = injectedLanguageManager.getInjectedPsiFiles(element);
                if (injectedPsiFilePairs == null) {
                    super.visitElement(element);
                    return;
                }
                result.addAll(injectedPsiFilePairs.stream().map(p -> (PsiFile) p.getFirst()).distinct().collect(Collectors.toList()));
                super.visitElement(element);
            }
        };
        source.acceptChildren(visitor);
        return result;
    }
}
