// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.util.io.DataExternalizer;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@OverrideOnly
@Internal
public interface CustomInputsIndexFileBasedIndexExtension<K> {
  @NotNull
  DataExternalizer<Collection<K>> createExternalizer();
}
