package com.intellij.openapi.fileChooser.ex;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.fileChooser.FileElement;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class FileNodeDescriptor extends NodeDescriptor {

  private FileElement myFileElement;
  private final Icon myOriginalOpenIcon;
  private final Icon myOriginalClosedIcon;
  private final String myComment;

  public FileNodeDescriptor(Project project,
                            @NotNull FileElement element,
                            NodeDescriptor parentDescriptor,
                            Icon openIcon,
                            Icon closedIcon,
                            String name,
                            String comment) {
    super(project, parentDescriptor);
    myOriginalOpenIcon = openIcon;
    myOriginalClosedIcon = closedIcon;
    myComment = comment;
    myFileElement = element;
    myName = name;
  }

  public boolean update() {
    boolean changed = false;

    // special handling for roots with names (e.g. web roots)
    if (myName == null || myComment == null) {
      final String newName = myFileElement.toString();
      if (!newName.equals(myName)) changed = true;
      myName = newName;
    }

    VirtualFile file = myFileElement.getFile();

    if (file == null) return true;

    myOpenIcon = myOriginalOpenIcon;
    myClosedIcon = myOriginalClosedIcon;
    if (myFileElement.isHidden()) {
      myOpenIcon = IconLoader.getTransparentIcon(myOpenIcon);
      myClosedIcon = IconLoader.getTransparentIcon(myClosedIcon);
    }
    myColor = myFileElement.isHidden() ? SimpleTextAttributes.DARK_TEXT.getFgColor() : null;
    return changed;
  }

  @NotNull
  public final FileElement getElement() {
    return myFileElement;
  }

  protected final void setElement(FileElement descriptor) {
    myFileElement = descriptor;
  }

  public String getComment() {
    return myComment;
  }
}