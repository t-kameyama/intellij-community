/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionGroupWrapper;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ExcludingActionGroup extends ActionGroupWrapper {
  private final Set<AnAction> myExcludes;

  public ExcludingActionGroup(@NotNull ActionGroup delegate, @NotNull Set<AnAction> excludes) {
    super(delegate);
    myExcludes = excludes;
  }

  @Override
  public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
    List<AnAction> result = new ArrayList<>();
    for (AnAction action : super.getChildren(e)) {
      if (myExcludes.contains(action)) {
        continue;
      }
      if (action instanceof ActionGroup) {
        result.add(new ExcludingActionGroup((ActionGroup)action, myExcludes));
      }
      else {
        result.add(action);
      }
    }
    return result.toArray(AnAction.EMPTY_ARRAY);
  }
}
