// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.refactoring.ui;

import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.PsiModifier;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.util.VisibilityUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class JavaVisibilityPanel extends VisibilityPanelBase<String> {
  private JRadioButton myRbAsIs;
  private JRadioButton myRbEscalate;
  private final JRadioButton myRbPrivate;
  private final JRadioButton myRbProtected;
  private final JRadioButton myRbPackageLocal;
  private final JRadioButton myRbPublic;

  public JavaVisibilityPanel(boolean hasAsIs, final boolean hasEscalate) {
    this(hasAsIs, hasEscalate, RefactoringBundle.message("visibility.border.title"));
  }

  public JavaVisibilityPanel(boolean hasAsIs,
                             final boolean hasEscalate,
                             @NlsContexts.BorderTitle String visibilityTitle) {
    setBorder(IdeBorderFactory.createTitledBorder(visibilityTitle, true,
                                                  JBUI.insets(IdeBorderFactory.TITLED_BORDER_TOP_INSET, UIUtil.DEFAULT_HGAP,
                                                              IdeBorderFactory.TITLED_BORDER_BOTTOM_INSET,
                                                              IdeBorderFactory.TITLED_BORDER_RIGHT_INSET)));
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    ButtonGroup bg = new ButtonGroup();

    ItemListener listener = new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          panelStateChanged(new ChangeEvent(this));
        }
      }
    };

    if (hasEscalate) {
      myRbEscalate = new JRadioButton();
      myRbEscalate.setText(RefactoringBundle.getEscalateVisibility());
      myRbEscalate.addItemListener(listener);
      add(myRbEscalate);
      bg.add(myRbEscalate);
    }

    if (hasAsIs) {
      myRbAsIs = new JRadioButton();
      myRbAsIs.setText(RefactoringBundle.getVisibilityAsIs());
      myRbAsIs.addItemListener(listener);
      add(myRbAsIs);
      bg.add(myRbAsIs);
    }


    myRbPrivate = new JRadioButton();
    myRbPrivate.setText(RefactoringBundle.getVisibilityPrivate());
    myRbPrivate.addItemListener(listener);
    myRbPrivate.setFocusable(false);
    add(myRbPrivate);
    bg.add(myRbPrivate);

    myRbPackageLocal = new JRadioButton();
    myRbPackageLocal.setText(RefactoringBundle.getVisibilityPackageLocal());
    myRbPackageLocal.addItemListener(listener);
    myRbPackageLocal.setFocusable(false);
    add(myRbPackageLocal);
    bg.add(myRbPackageLocal);

    myRbProtected = new JRadioButton();
    myRbProtected.setText(RefactoringBundle.getVisibilityProtected());
    myRbProtected.addItemListener(listener);
    myRbProtected.setFocusable(false);
    add(myRbProtected);
    bg.add(myRbProtected);

    myRbPublic = new JRadioButton();
    myRbPublic.setText(RefactoringBundle.getVisibilityPublic());
    myRbPublic.addItemListener(listener);
    myRbPublic.setFocusable(false);
    add(myRbPublic);
    bg.add(myRbPublic);
  }

  private void panelStateChanged(ChangeEvent event) {
    stateChanged(event);
  }

  @Override
  public @Nullable String getVisibility() {
    if (myRbPublic.isSelected()) {
      return PsiModifier.PUBLIC;
    }
    if (myRbPackageLocal.isSelected()) {
      return PsiModifier.PACKAGE_LOCAL;
    }
    if (myRbProtected.isSelected()) {
      return PsiModifier.PROTECTED;
    }
    if (myRbPrivate.isSelected()) {
      return PsiModifier.PRIVATE;
    }
    if (myRbEscalate != null && myRbEscalate.isSelected()) {
      return VisibilityUtil.ESCALATE_VISIBILITY;
    }

    return null;
  }

  @Override
  public void setVisibility(@Nullable String visibility) {
    if (PsiModifier.PUBLIC.equals(visibility)) {
      myRbPublic.setSelected(true);
    }
    else if (PsiModifier.PROTECTED.equals(visibility)) {
      myRbProtected.setSelected(true);
    }
    else if (PsiModifier.PACKAGE_LOCAL.equals(visibility)) {
      myRbPackageLocal.setSelected(true);
    }
    else if (PsiModifier.PRIVATE.equals(visibility)) {
      myRbPrivate.setSelected(true);
    }
    else if (myRbEscalate != null) {
      myRbEscalate.setSelected(true);
    }
    else if (myRbAsIs != null) {
      myRbAsIs.setSelected(true);
    }
  }

  public void disableAllButPublic() {
    myRbPrivate.setEnabled(false);
    myRbProtected.setEnabled(false);
    myRbPackageLocal.setEnabled(false);
    if (myRbEscalate != null) {
      myRbEscalate.setEnabled(false);
    }
    if (myRbAsIs != null) {
      myRbAsIs.setEnabled(false);
    }
    myRbPublic.setEnabled(true);
    myRbPublic.setSelected(true);
  }
}
