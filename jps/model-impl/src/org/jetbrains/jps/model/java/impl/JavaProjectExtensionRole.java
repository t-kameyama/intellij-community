/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.jps.model.java.impl;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.JpsElementCreator;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.java.JpsJavaProjectExtension;

/**
 * This class is for internal use only, call {@link JpsJavaExtensionService#getProjectExtension(JpsProject)} to get an instance of 
 * {@link JpsJavaProjectExtension} for a project. 
 */
@ApiStatus.Internal
public class JavaProjectExtensionRole extends JpsElementChildRoleBase<JpsJavaProjectExtension> implements JpsElementCreator<JpsJavaProjectExtension> {
  public static final JavaProjectExtensionRole INSTANCE = new JavaProjectExtensionRole();

  public JavaProjectExtensionRole() {
    super("java project extension");
  }

  @NotNull
  @Override
  public JpsJavaProjectExtension create() {
    return new JpsJavaProjectExtensionImpl();
  }
}
