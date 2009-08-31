package com.intellij.codeInsight.daemon.impl.quickfix;

import com.intellij.codeInsight.CodeInsightUtilBase;
import com.intellij.codeInsight.daemon.QuickFixBundle;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VariableArrayTypeFix implements IntentionAction {
  private static final Logger LOG = Logger.getInstance("#com.intellij.codeInsight.daemon.impl.quickfix.VariableArrayTypeFix");

  private final PsiVariable myVariable;
  /**
   * only for the case when in same statement with initialization
   */
  @Nullable
  private final PsiNewExpression myNewExpression;
  @NotNull
  private final PsiArrayInitializerExpression myInitializer;
  @NotNull
  private final PsiArrayType myTargetType;

  public VariableArrayTypeFix(@NotNull PsiArrayInitializerExpression initializer, @NotNull PsiType componentType) {
    PsiArrayType arrayType = new PsiArrayType(componentType);
    PsiArrayInitializerExpression arrayInitializer = initializer;
    while (arrayInitializer.getParent() instanceof PsiArrayInitializerExpression) {
      arrayInitializer = (PsiArrayInitializerExpression) arrayInitializer.getParent();
      arrayType = new PsiArrayType(arrayType);
    }

    myInitializer = arrayInitializer;
    myTargetType = arrayType;

    PsiNewExpression newExpressionLocal = null;
    PsiVariable variableLocal = null;

    final PsiElement parent = myInitializer.getParent();
    if (parent instanceof PsiVariable) {
      variableLocal = (PsiVariable) parent;
    } else if (parent instanceof PsiNewExpression) {
      newExpressionLocal = (PsiNewExpression) parent;
      final PsiElement newParent = newExpressionLocal.getParent();
      if (newParent instanceof PsiAssignmentExpression) {
        variableLocal = getFromAssignment((PsiAssignmentExpression) newParent);
      } else if (newParent instanceof PsiVariable) {
        variableLocal = (PsiVariable) newParent;
      }
    } else if (parent instanceof PsiAssignmentExpression) {
      variableLocal = getFromAssignment((PsiAssignmentExpression)parent);
    }

    myNewExpression = newExpressionLocal;
    myVariable = variableLocal;
  }

  @Nullable
  private static PsiVariable getFromAssignment(final PsiAssignmentExpression assignment) {
    final PsiExpression reference = assignment.getLExpression();
    final PsiElement referencedElement = reference instanceof PsiReferenceExpression ? ((PsiReferenceExpression)reference).resolve() : null;
    return referencedElement != null && referencedElement instanceof PsiVariable ? (PsiVariable)referencedElement : null;
  }

  private String getNewText() {
    final String newText = myNewExpression.getText();
    final int initializerIdx = newText.indexOf(myInitializer.getText());
    if (initializerIdx != -1) {
      return newText.substring(0, initializerIdx).trim();
    }
    return newText;
  }

  @NotNull
  public String getText() {
    return myTargetType.equals(myVariable.getType()) && myNewExpression != null ?
           QuickFixBundle.message("change.new.operator.type.text", getNewText(), myTargetType.getCanonicalText(), "") :
           QuickFixBundle.message("fix.variable.type.text", myVariable.getName(), myTargetType.getCanonicalText());
  }

  @NotNull
  public String getFamilyName() {
    return myTargetType.equals(myVariable.getType()) && myNewExpression != null ?
           QuickFixBundle.message("change.new.operator.type.family") :
           QuickFixBundle.message("fix.variable.type.family");
  }

  public boolean isAvailable(@NotNull final Project project, final Editor editor, final PsiFile file) {
    return myVariable != null && myVariable.isValid()
        && myVariable.getManager().isInProject(myVariable)
        && myTargetType.isValid()
        && myInitializer.isValid();
  }

  public void invoke(@NotNull final Project project, final Editor editor, final PsiFile file) throws IncorrectOperationException {
    if (!CodeInsightUtilBase.prepareFileForWrite(myVariable.getContainingFile())) return;
    try {
      final PsiElementFactory factory = JavaPsiFacade.getInstance(file.getProject()).getElementFactory();

      if (! myTargetType.equals(myVariable.getType())) {
        myVariable.normalizeDeclaration();
        myVariable.getTypeElement().replace(factory.createTypeElement(myTargetType));
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(myVariable);

        if (! myVariable.getContainingFile().equals(file)) {
          UndoUtil.markPsiFileForUndo(myVariable.getContainingFile());
        }
      }

      if (myNewExpression != null) {
        if (!CodeInsightUtilBase.prepareFileForWrite(file)) return;

        @NonNls String text = "new " + myTargetType.getCanonicalText() + "{}";
        final PsiNewExpression newExpression = (PsiNewExpression) factory.createExpressionFromText(text, myNewExpression.getParent());
        final PsiElement[] children = newExpression.getChildren();
        children[children.length - 1].replace(myInitializer);
        myNewExpression.replace(newExpression);
      }
    } catch (IncorrectOperationException e) {
      LOG.error(e);
    }
  }

  public boolean startInWriteAction() {
    return true;
  }
}