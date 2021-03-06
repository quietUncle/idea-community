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

package com.intellij.lang;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

/**
 * @author yole
 */
public abstract class PsiBuilderFactory {
  public static PsiBuilderFactory getInstance() {
    return ServiceManager.getService(PsiBuilderFactory.class);
  }

  public abstract PsiBuilder createBuilder(@NotNull Project project, @NotNull ASTNode chameleon);

  public abstract PsiBuilder createBuilder(@NotNull Project project, @NotNull LighterLazyParseableNode chameleon);

  /**
   * @deprecated consider using {@link #createBuilder(com.intellij.openapi.project.Project, ASTNode)} instead.
   */
  public PsiBuilder createBuilder(@NotNull Project project, @NotNull ASTNode tree, @NotNull Language lang, @NotNull CharSequence seq) {
    return createBuilder(project, tree, null, lang, seq);
  }

  public abstract PsiBuilder createBuilder(@NotNull Project project, @NotNull ASTNode chameleon, @Nullable Lexer lexer,
                                           @NotNull Language lang, @NotNull CharSequence seq);

  public abstract PsiBuilder createBuilder(@NotNull Project project, @NotNull LighterLazyParseableNode chameleon, @Nullable Lexer lexer,
                                           @NotNull Language lang, @NotNull CharSequence seq);

  @TestOnly
  public abstract PsiBuilder createBuilder(@NotNull Lexer lexer, @NotNull Language lang, @NotNull CharSequence seq);
}
