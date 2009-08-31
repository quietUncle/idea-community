package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.impl.EditorColorsManagerImpl;
import com.intellij.openapi.editor.colors.impl.EditorColorsSchemeImpl;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.options.SharedScheme;
import com.intellij.openapi.project.Project;

import java.util.Collection;

/**
 * @author max
 */
public class QuickChangeColorSchemeAction extends QuickSwitchSchemeAction {
  protected void fillActions(Project project, DefaultActionGroup group) {
    final EditorColorsScheme[] schemes = EditorColorsManager.getInstance().getAllSchemes();
    EditorColorsScheme current = EditorColorsManager.getInstance().getGlobalScheme();
    for (final EditorColorsScheme scheme : schemes) {
      addScheme(group, current, scheme, false);
    }


    Collection<SharedScheme<EditorColorsSchemeImpl>> sharedSchemes = ((EditorColorsManagerImpl)EditorColorsManager.getInstance()).getSchemesManager().loadSharedSchemes();

    if (!sharedSchemes.isEmpty()) {
      group.add(Separator.getInstance());

      for (SharedScheme<EditorColorsSchemeImpl> sharedScheme : sharedSchemes) {
        addScheme(group, current, sharedScheme.getScheme(), true);
      }
    }

  }

  private void addScheme(final DefaultActionGroup group, final EditorColorsScheme current, final EditorColorsScheme scheme, final boolean addScheme) {
    group.add(new AnAction(scheme.getName(), "", scheme == current ? ourCurrentAction : ourNotCurrentAction) {
      public void actionPerformed(AnActionEvent e) {
        if (addScheme) {
          EditorColorsManager.getInstance().addColorsScheme(scheme);
        }
        EditorColorsManager.getInstance().setGlobalScheme(scheme);
        Editor[] editors = EditorFactory.getInstance().getAllEditors();
        for (Editor editor : editors) {
          ((EditorEx)editor).reinitSettings();
        }
      }
    });
  }

  protected boolean isEnabled() {
    return EditorColorsManager.getInstance().getAllSchemes().length > 1 || ((EditorColorsManagerImpl)EditorColorsManager.getInstance()).getSchemesManager().isImportAvailable();
  }
}