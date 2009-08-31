/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.codeInsight.generation;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * @author peter
 */
public class PsiGenerationInfo<T extends PsiMember> extends GenerationInfo {
  private T myMember;
  private final boolean myMergeIfExists;

  public PsiGenerationInfo(@NotNull final T member) {
    myMember = member;
    myMergeIfExists = true;
  }

  public PsiGenerationInfo(@NotNull T member, boolean mergeIfExists) {
    myMember = member;
    myMergeIfExists = mergeIfExists;
  }

  public final T getPsiMember() {
    return myMember;
  }

  public void insert(PsiClass aClass, PsiElement anchor, boolean before) throws IncorrectOperationException {
    final PsiMember existingMember;
    if (myMember instanceof PsiField) {
      existingMember = aClass.findFieldByName(myMember.getName(), false);
    }
    else if (myMember instanceof PsiMethod) {
      existingMember = aClass.findMethodBySignature((PsiMethod)myMember, false);
    }
    else existingMember = null;
    if (existingMember == null || !myMergeIfExists) {
      PsiElement newMember = GenerateMembersUtil.insert(aClass, myMember, anchor, before);
      myMember = (T)JavaCodeStyleManager.getInstance(aClass.getProject()).shortenClassReferences(newMember);
    }
    else {
      final PsiModifierList modifierList = myMember.getModifierList();
      final PsiModifierList existingModifierList = existingMember.getModifierList();
      if (modifierList != null && existingModifierList != null) {
        final PsiAnnotation[] psiAnnotations = modifierList.getAnnotations();
        PsiElement annoAnchor = existingModifierList.getAnnotations().length > 0 ? existingModifierList.getAnnotations()[0] : existingModifierList.getFirstChild();
        if (psiAnnotations.length > 0) {
          for (PsiAnnotation annotation : psiAnnotations) {
            final PsiAnnotation existingAnno = existingModifierList.findAnnotation(annotation.getQualifiedName());
            if (existingAnno != null) existingAnno.replace(annotation);
            else existingModifierList.addBefore(annotation, annoAnchor);
          }
        }
      }
      myMember = (T)existingMember;
    }

    if (myMember instanceof PsiMethod) {
      final Project project = myMember.getProject();
      SmartPsiElementPointer<T> pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(myMember);
      PostprocessReformattingAspect reformattingAspect = project.getComponent(PostprocessReformattingAspect.class);
      reformattingAspect.doPostponedFormatting(myMember.getContainingFile().getViewProvider());
      myMember = pointer.getElement();
      if (myMember != null) {
        final PsiParameterList parameterList = ((PsiMethod)myMember).getParameterList();
        reformattingAspect.disablePostprocessFormattingInside(new Runnable(){
          public void run() {
            CodeStyleManager.getInstance(project).reformat(parameterList);
          }
        });
      }
    }
  }
}