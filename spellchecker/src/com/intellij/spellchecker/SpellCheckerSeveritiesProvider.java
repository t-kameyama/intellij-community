// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.spellchecker;

import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.SeveritiesProvider;
import com.intellij.icons.AllIcons;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.spellchecker.util.SpellCheckerBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class SpellCheckerSeveritiesProvider extends SeveritiesProvider {
  public static final TextAttributesKey TYPO_KEY = TextAttributesKey.createTextAttributesKey("TYPO");
  @SuppressWarnings("UnresolvedPropertyKey")
  public static final HighlightSeverity TYPO = new HighlightSeverity(
    "TYPO",
    HighlightSeverity.INFORMATION.myVal + 5,
    SpellCheckerBundle.messagePointer("typo.severity"),
    SpellCheckerBundle.messagePointer("typo.severity.capitalized"),
    SpellCheckerBundle.messagePointer("typo.severity.count.message")
  );

  @Override
  @NotNull
  public List<HighlightInfoType> getSeveritiesHighlightInfoTypes() {
    final class T extends HighlightInfoType.HighlightInfoTypeImpl implements HighlightInfoType.Iconable {
      private T(@NotNull HighlightSeverity severity, @NotNull TextAttributesKey attributesKey) {
        super(severity, attributesKey);
      }

      @NotNull
      @Override
      public Icon getIcon() {
        return AllIcons.General.InspectionsTypos;
      }
    }
    return Collections.singletonList(new T(TYPO, TYPO_KEY));
  }

  @Override
  public boolean isGotoBySeverityEnabled(HighlightSeverity minSeverity) {
    return TYPO != minSeverity;
  }
}