/*
 * User: anna
 * Date: 29-Dec-2008
 */
package com.intellij.codeInsight.highlighting;

import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.LocalInspectionsPass;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.codeInspection.ex.InspectionManagerEx;
import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.codeInspection.reference.RefManagerImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.*;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HighlightSuppressedWarningsHandler extends HighlightUsagesHandlerBase<PsiLiteralExpression> {
  private static final Logger LOG = Logger.getInstance("#" + HighlightSuppressedWarningsHandler.class.getName());

  private final PsiAnnotation myTarget;
  private final PsiLiteralExpression mySuppressedExpression;

  protected HighlightSuppressedWarningsHandler(Editor editor, PsiFile file, PsiAnnotation target, PsiLiteralExpression suppressedExpression) {
    super(editor, file);
    myTarget = target;
    mySuppressedExpression = suppressedExpression;
  }

  public List<PsiLiteralExpression> getTargets() {
    final List<PsiLiteralExpression> result = new ArrayList<PsiLiteralExpression>();
    if (mySuppressedExpression != null) {
      result.add(mySuppressedExpression);
      return result;
    }
    final PsiAnnotationParameterList list = myTarget.getParameterList();
    final PsiNameValuePair[] attributes = list.getAttributes();
    for (PsiNameValuePair attribute : attributes) {
      final PsiAnnotationMemberValue value = attribute.getValue();
      if (value instanceof PsiArrayInitializerMemberValue) {
        final PsiAnnotationMemberValue[] initializers = ((PsiArrayInitializerMemberValue)value).getInitializers();
        for (PsiAnnotationMemberValue initializer : initializers) {
          if (initializer instanceof PsiLiteralExpression) {
            result.add((PsiLiteralExpression)initializer);
          }
        }
      }
    }
    return result;
  }

  protected void selectTargets(List<PsiLiteralExpression> targets, final Consumer<List<PsiLiteralExpression>> selectionConsumer) {
    if (targets.size() == 1) {
      selectionConsumer.consume(targets);
    } else {
      JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<PsiLiteralExpression>("Choose Inspections to Highlight Suppressed Problems from", targets){
        @Override
        public PopupStep onChosen(PsiLiteralExpression selectedValue, boolean finalChoice) {
          selectionConsumer.consume(Collections.singletonList(selectedValue));
          return FINAL_CHOICE;
        }

        @NotNull
        @Override
        public String getTextFor(PsiLiteralExpression value) {
          final Object o = value.getValue();
          LOG.assertTrue(o instanceof String);
          return (String)o;
        }
      }).showInBestPositionFor(myEditor);
    }
  }

  public void computeUsages(List<PsiLiteralExpression> targets) {
    final Project project = myTarget.getProject();
    final PsiElement parent = myTarget.getParent().getParent();
    LocalInspectionsPass pass = new LocalInspectionsPass(myFile, myFile.getViewProvider().getDocument(), parent.getTextRange().getStartOffset(), parent.getTextRange().getEndOffset());
    final InspectionProfile inspectionProfile =
      InspectionProjectProfileManager.getInstance(project).getInspectionProfile();
    for (PsiLiteralExpression target : targets) {
      final Object value = target.getValue();
      if (value instanceof String) {
        final InspectionProfileEntry toolById = ((InspectionProfileImpl)inspectionProfile).getToolById(((String)value), target);
        if (toolById instanceof LocalInspectionToolWrapper) {
          final LocalInspectionToolWrapper tool = new LocalInspectionToolWrapper(((LocalInspectionToolWrapper)toolById).getTool());
          final InspectionManagerEx managerEx = ((InspectionManagerEx)InspectionManagerEx.getInstance(project));
          final GlobalInspectionContextImpl context = managerEx.createNewGlobalContext(false);
          tool.initialize(context);
          ((RefManagerImpl)context.getRefManager()).inspectionReadActionStarted();
          pass.doInspectInBatch(managerEx, new InspectionProfileEntry[]{tool}, ProgressManager.getInstance().getProgressIndicator(), false);
          for (HighlightInfo info : pass.getInfos()) {
            final PsiElement element = CollectHighlightsUtil.findCommonParent(myFile, info.startOffset, info.endOffset);
            if (element != null) {
              addOccurrence(element);
            }
          }
        }
      }
    }
  }
}