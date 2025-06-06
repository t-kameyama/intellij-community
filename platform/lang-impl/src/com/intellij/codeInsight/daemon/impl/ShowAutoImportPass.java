// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeInsight.daemon.DaemonBundle;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import com.intellij.codeInsight.daemon.ReferenceImporter;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.HintAction;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.inlinePrompt.InlinePrompt;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.impl.ImaginaryEditor;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.ExperimentalUI;
import com.intellij.ui.JBColor;
import com.intellij.util.SlowOperations;
import com.intellij.util.ThreeState;
import com.intellij.util.concurrency.ThreadingAssertions;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class ShowAutoImportPass extends TextEditorHighlightingPass {
  private final Editor myEditor;

  private final PsiFile myPsiFile;

  private final TextRange myVisibleRange;
  private final boolean hasDirtyTextRange;
  private final List<BooleanSupplier> autoImportActions = Collections.synchronizedList(new ArrayList<>());

  @RequiresBackgroundThread
  ShowAutoImportPass(@NotNull PsiFile psiFile, @NotNull Editor editor, @NotNull ProperTextRange visibleRange) {
    super(psiFile.getProject(), editor.getDocument(), false);
    ApplicationManager.getApplication().assertIsNonDispatchThread();

    myEditor = editor;
    myVisibleRange = visibleRange;
    myPsiFile = psiFile;
    hasDirtyTextRange = FileStatusMap.getDirtyTextRange(editor.getDocument(), psiFile, Pass.UPDATE_ALL) != null;
  }

  @Override
  public void doCollectInformation(@NotNull ProgressIndicator progress) {
    if (isInlinePromptShown()) {
      return;
    }

    Document document = myEditor.getDocument();
    List<HighlightInfo> infos = new ArrayList<>();
    List<BooleanSupplier> result = new ArrayList<>();
    int exceptCaretOffset = myEditor.getCaretModel().getOffset();

    DaemonCodeAnalyzerEx.processHighlights(document, myProject, null, 0, document.getTextLength(), info -> {
      if ((info.hasLazyQuickFixes() || info.type.getAttributesKey().equals(CodeInsightColors.WRONG_REFERENCES_ATTRIBUTES))// auto import fix can be either "lazy fix" or the regular fix attached to the info for unresolved reference
          && info.getSeverity() == HighlightSeverity.ERROR && !info.containsOffset(exceptCaretOffset, true)) {
        infos.add(info);
      }
      return true;
    });

    for (HighlightInfo info : infos) {
      for (ReferenceImporter importer : ReferenceImporter.EP_NAME.getExtensionList()) {
        if (!importer.isAddUnambiguousImportsOnTheFlyEnabled(myPsiFile)) {
          continue;
        }
        BooleanSupplier action = importer.computeAutoImportAtOffset(myEditor, myPsiFile, info.getActualStartOffset(), false);
        if (action != null) {
          result.add(action);
        }
      }
    }
    autoImportActions.addAll(result);
  }

  @Override
  public void doApplyInformationToEditor() {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!UIUtil.hasFocus(myEditor.getContentComponent())) {
        return;
      }
      if (DumbService.isDumb(myProject)) {
        return;
      }

      if (isInlinePromptShown()) {
        return;
      }

      try (AccessToken ignore = SlowOperations.knownIssue("IJPL-162974")) {
        if (!myPsiFile.isValid()) {
          return;
        }
        if (myEditor.isDisposed() || myEditor instanceof EditorWindow window && !window.isValid()) {
          return;
        }
      }

      int caretOffset = myEditor.getCaretModel().getOffset();
      importUnambiguousImports();
      if (isImportHintEnabled() && !(myEditor instanceof ImaginaryEditor)) {
        List<HighlightInfo> visibleHighlights = getVisibleHighlights(myVisibleRange, myProject, myEditor, hasDirtyTextRange);
        // sort by distance to the caret
        visibleHighlights.sort(Comparator.comparingInt(info -> Math.abs(info.getActualStartOffset() - caretOffset)));
        try (AccessToken ignore = SlowOperations.knownIssue("IDEA-301732, IDEA-305605, EA-829346, EA-789713, ...")) {
          for (HighlightInfo visibleHighlight : visibleHighlights) {
            if (showAddImportHint(visibleHighlight)) {
              break;
            }
          }
        }
      }
    }, myProject.getDisposed());
  }

  private boolean isInlinePromptShown() {
    return InlinePrompt.isInlinePromptShown(myEditor) || InlinePrompt.isInlinePromptGenerating(myEditor);
  }

  private void importUnambiguousImports() {
    ThreadingAssertions.assertEventDispatchThread();
    try (AccessToken ignore = SlowOperations.knownIssue("IDEA-335057, EA-843299")) {
      if (!autoImportActions.isEmpty() && mayAutoImportNow(myPsiFile, true, ThreeState.UNSURE)) {
        for (BooleanSupplier autoImportAction : autoImportActions) {
          autoImportAction.getAsBoolean();
        }
      }
    }
  }

  public static boolean mayAutoImportNow(@NotNull PsiFile psiFile, boolean isInContent,
                                         @NotNull ThreeState extensionsAllowToChangeFileSilently) {
    return isAddUnambiguousImportsOnTheFlyEnabled(psiFile) &&
           (ApplicationManager.getApplication().isUnitTestMode() ||
            DaemonListeners.canChangeFileSilently(psiFile, isInContent, extensionsAllowToChangeFileSilently)) &&
           !LaterInvocator.isInModalContext();
  }

  public static boolean isAddUnambiguousImportsOnTheFlyEnabled(@NotNull PsiFile psiFile) {
    PsiFile templateFile = PsiUtilCore.getTemplateLanguageFile(psiFile);
    if (templateFile == null) return false;
    return ContainerUtil.exists(ReferenceImporter.EP_NAME.getExtensionList(),
                                importer -> importer.isAddUnambiguousImportsOnTheFlyEnabled(psiFile));
  }

  private static @NotNull List<HighlightInfo> getVisibleHighlights(@NotNull TextRange visibleRange,
                                                                   @NotNull Project project,
                                                                   @NotNull Editor editor,
                                                                   boolean isDirty) {
    List<HighlightInfo> highlights = new ArrayList<>();
    int offset = editor.getCaretModel().getOffset();
    DaemonCodeAnalyzerEx.processHighlights(editor.getDocument(), project, null, visibleRange.getStartOffset(), visibleRange.getEndOffset(), info -> {
      //no changes after escape => suggest imports under caret only
      if (!isDirty && !info.containsOffset(offset, true)) {
        return true;
      }
      if (!info.hasHint() || editor.getFoldingModel().isOffsetCollapsed(info.startOffset)) {
        return true;
      }
      highlights.add(info);
      return true;
    });
    return highlights;
  }

  private boolean showAddImportHint(@NotNull HighlightInfo info) {
    for (HintAction action : extractHints(info)) {
      if (action.isAvailable(myProject, myEditor, myPsiFile) && action.showHint(myEditor)) {
        return true;
      }
    }
    return false;
  }

  private boolean isImportHintEnabled() {
    return DaemonCodeAnalyzerSettings.getInstance().isImportHintEnabled() &&
           DaemonCodeAnalyzer.getInstance(myProject).isImportHintsEnabled(myPsiFile);
  }

  @VisibleForTesting
  @ApiStatus.Internal
  public static @NotNull List<HintAction> extractHints(@NotNull HighlightInfo info) {
    List<HintAction> result = new ArrayList<>();
    info.findRegisteredQuickFix((descriptor, range) -> {
      ProgressManager.checkCanceled();
      IntentionAction action = descriptor.getAction();
      if (action instanceof HintAction hint) {
        result.add(hint);
      }
      return null;
    });
    return result;
  }

  public static @NotNull @NlsContexts.HintText String getMessage(boolean multiple, @Nullable String kind, @NotNull String name) {
    return kind != null && ExperimentalUI.isNewUI() ? getMessage(kind, name) : getMessage(multiple, name);
  }

  public static @NotNull @NlsContexts.HintText String getMessage(@NotNull String kind, @NotNull String name) {
    String action = KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS));
    String actionColor = ColorUtil.toHex(JBColor.namedColor("shortcutForeground", 0x818594, 0x6F737A));
    return DaemonBundle.message("import.popup.hint.text", kind, name, action, actionColor);
  }

  public static @NotNull @NlsContexts.HintText String getMessage(boolean multiple, @NotNull String name) {
    String messageKey = multiple ? "import.popup.multiple" : "import.popup.text";
    String hintText = DaemonBundle.message(messageKey, name);
    hintText += " " + KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS));
    return hintText;
  }
}
