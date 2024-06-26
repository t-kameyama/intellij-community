// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.jps.builders.java.dependencyView;

import org.jetbrains.annotations.NotNull;

class DifferenceImpl extends Difference{

  private final Difference myDelegate;

  DifferenceImpl(@NotNull Difference delegate) {
    myDelegate = delegate;
  }

  @Override
  public int base() {
    return myDelegate.base();
  }

  @Override
  public boolean no() {
    return myDelegate.no();
  }

  @Override
  public boolean accessRestricted() {
    return myDelegate.accessRestricted();
  }

  @Override
  public boolean accessExpanded() {
    return myDelegate.accessExpanded();
  }

  @Override
  public int addedModifiers() {
    return myDelegate.addedModifiers();
  }

  @Override
  public int removedModifiers() {
    return myDelegate.removedModifiers();
  }

  @Override
  public boolean packageLocalOn() {
    return myDelegate.packageLocalOn();
  }

  @Override
  public boolean hadValue() {
    return myDelegate.hadValue();
  }

  @Override
  public Specifier<TypeRepr.ClassType, Difference> annotations() {
    return myDelegate.annotations();
  }
}
