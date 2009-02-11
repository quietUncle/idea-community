package com.intellij.codeInsight.editorActions.enter;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.PropertiesUtil;
import com.intellij.lang.properties.parsing.PropertiesTokenTypes;

public class EnterInPropertiesFileHandler implements EnterHandlerDelegate {
  public Result preprocessEnter(final PsiFile file, final Editor editor, final Ref<Integer> caretOffsetRef, final Ref<Integer> caretAdvance,
                                final DataContext dataContext, final EditorActionHandler originalHandler) {
    int caretOffset = caretOffsetRef.get().intValue();
    PsiElement psiAtOffset = file.findElementAt(caretOffset);
    if (file instanceof PropertiesFile) {
      handleEnterInPropertiesFile(editor, editor.getDocument(), psiAtOffset, caretOffset);
      return Result.Stop;
    }
    return Result.Continue;
  }

  private static void handleEnterInPropertiesFile(final Editor editor,
                                                  final Document document,
                                                  final PsiElement psiAtOffset,
                                                  int caretOffset) {
    String text = document.getText();
    String line = text.substring(0, caretOffset);
    int i = line.lastIndexOf('\n');
    if (i > 0) {
      line = line.substring(i);
    }
    final String toInsert;
    if (PropertiesUtil.isUnescapedBackSlashAtTheEnd(line)) {
      toInsert = "\n  ";
    }
    else {
      final IElementType elementType = psiAtOffset == null ? null : psiAtOffset.getNode().getElementType();

      if (elementType == PropertiesTokenTypes.VALUE_CHARACTERS) {
        toInsert = "\\\n  ";
      }
      else if (elementType == PropertiesTokenTypes.END_OF_LINE_COMMENT && "#!".indexOf(document.getText().charAt(caretOffset)) == -1) {
        toInsert = "\n#";
      }
      else {
        toInsert = "\n";
      }
    }
    document.insertString(caretOffset, toInsert);
    caretOffset+=toInsert.length();
    editor.getCaretModel().moveToOffset(caretOffset);
    editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
    editor.getSelectionModel().removeSelection();
  }

}