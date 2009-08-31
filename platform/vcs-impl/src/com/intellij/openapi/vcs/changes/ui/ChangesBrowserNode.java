package com.intellij.openapi.vcs.changes.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListOwner;
import com.intellij.openapi.vcs.changes.LocallyDeletedChange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * @author max
 */
public class ChangesBrowserNode<T> extends DefaultMutableTreeNode {
  protected int myCount = -1;
  protected int myDirectoryCount = -1;
  public static final Object IGNORED_FILES_TAG = new Object() {
    public String toString() {
      return VcsBundle.message("changes.nodetitle.ignored.files");
    }
  };
  public static final Object LOCKED_FOLDERS_TAG = new Object() {
    public String toString() {
      return VcsBundle.message("changes.nodetitle.locked.folders");
    }
  };
  public static final Object LOGICALLY_LOCKED_TAG = VcsBundle.message("changes.nodetitle.logicallt.locked.folders");
  
  public static final Object UNVERSIONED_FILES_TAG = new Object() {
    public String toString() {
      return VcsBundle.message("changes.nodetitle.unversioned.files");
    }
  };
  public static final Object MODIFIED_WITHOUT_EDITING_TAG = new Object() {
    public String toString() {
      return VcsBundle.message("changes.nodetitle.modified.without.editing");
    }
  };
  public static final Object SWITCHED_FILES_TAG = new Object() {
    public String toString() {
      return VcsBundle.message("changes.nodetitle.switched.files");
    }
  };

  protected ChangesBrowserNode(Object userObject) {
    super(userObject);
  }

  public static ChangesBrowserNode create(final Project project, @NotNull final LocallyDeletedChange change) {
    return new ChangesBrowserLocallyDeletedNode(change, project);
  }

  public static ChangesBrowserNode create(final Project project, @NotNull Object userObject) {
    /*if (userObject instanceof Change) {
      return new ChangesBrowserChangeNode(project, (Change) userObject);
    }*/
    if (userObject instanceof VirtualFile) {
      return new ChangesBrowserFileNode(project, (VirtualFile) userObject);
    }
    if (userObject instanceof FilePath) {
      return new ChangesBrowserFilePathNode((FilePath) userObject);
    }
/*    if (userObject instanceof ChangeList) {
      return new ChangesBrowserChangeListNode(project, (ChangeList) userObject);
    }*/
    if (userObject == IGNORED_FILES_TAG) {
      return new ChangesBrowserIgnoredFilesNode(userObject);
    }
    if (userObject == LOCKED_FOLDERS_TAG) {
      return new ChangesBrowserLockedFoldersNode(project, userObject);
    }
    if (userObject instanceof ChangesBrowserLogicallyLockedFile) {
      return (ChangesBrowserNode) userObject;
    }
    return new ChangesBrowserNode(userObject);
  }

  public void insert(MutableTreeNode newChild, int childIndex) {
    super.insert(newChild, childIndex);
    myCount = -1;
    myDirectoryCount = -1;
  }

  public int getCount() {
    if (myCount == -1) {
      myCount = 0;
      final Enumeration nodes = children();
      while (nodes.hasMoreElements()) {
        ChangesBrowserNode child = (ChangesBrowserNode)nodes.nextElement();
        myCount += child.getCount();
      }
    }
    return myCount;
  }

  public int getDirectoryCount() {
    if (myDirectoryCount == -1) {
      myDirectoryCount = isDirectory() ? 1 : 0;

      final Enumeration nodes = children();
      while (nodes.hasMoreElements()) {
        ChangesBrowserNode child = (ChangesBrowserNode)nodes.nextElement();
        myDirectoryCount += child.getDirectoryCount();
      }
    }
    return myDirectoryCount;
  }

  protected boolean isDirectory() {
    return false;
  }

  public List<Change> getAllChangesUnder() {
    List<Change> changes = new ArrayList<Change>();
    final Enumeration enumeration = depthFirstEnumeration();
    while (enumeration.hasMoreElements()) {
      ChangesBrowserNode child = (ChangesBrowserNode)enumeration.nextElement();
      final Object value = child.getUserObject();
      if (value instanceof Change) {
        changes.add((Change)value);
      }
    }
    return changes;
  }

  public List<VirtualFile> getAllFilesUnder() {
    List<VirtualFile> files = new ArrayList<VirtualFile>();
    final Enumeration enumeration = breadthFirstEnumeration();
    while (enumeration.hasMoreElements()) {
      ChangesBrowserNode child = (ChangesBrowserNode)enumeration.nextElement();
      final Object value = child.getUserObject();
      if (value instanceof VirtualFile) {
        final VirtualFile file = (VirtualFile)value;
        if (file.isValid()) {
          files.add(file);
        }
      }
    }

    return files;
  }

  public List<FilePath> getAllFilePathsUnder() {
    List<FilePath> files = new ArrayList<FilePath>();
    final Enumeration enumeration = breadthFirstEnumeration();
    while (enumeration.hasMoreElements()) {
      ChangesBrowserNode child = (ChangesBrowserNode)enumeration.nextElement();
      final Object value = child.getUserObject();
      if (child.isLeaf() && value instanceof FilePath) {
        final FilePath file = (FilePath)value;
        files.add(file);
      }
      final FilePath ownPath = child.getMyPath();
      if (ownPath != null) {
        files.add(ownPath);
      }
    }

    return files;
  }

  @Nullable
  protected FilePath getMyPath() {
    return null;
  }

  public void render(final ChangesBrowserNodeRenderer renderer, final boolean selected, final boolean expanded, final boolean hasFocus) {
    renderer.append(userObject.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    appendCount(renderer);
  }

  protected String getCountText() {
    int count = getCount();
    int dirCount = getDirectoryCount();
    if (dirCount == 0) {
      return " " + VcsBundle.message("changes.nodetitle.changecount", count);
    }
    else if (count == 0 && dirCount > 0) {
      return " " + VcsBundle.message("changes.nodetitle.directory.changecount", dirCount);
    }
    else {
      return " " + VcsBundle.message("changes.nodetitle.directory.file.changecount", dirCount, count);
    }
  }

  protected void appendCount(final ColoredTreeCellRenderer renderer) {
    final String countText = getCountText();
    renderer.append(countText, SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
  }

  public String toString() {
    return getTextPresentation();
  }

  public String getTextPresentation() {
    return userObject == null ? "" : userObject.toString();
  }

  public T getUserObject() {
    //noinspection unchecked
    return (T) userObject;
  }

  public boolean canAcceptDrop(final ChangeListDragBean dragBean) {
    return false;
  }

  public void acceptDrop(final ChangeListOwner dragOwner, final ChangeListDragBean dragBean) {
  }

  public int getSortWeight() {
    return 8;
  }

  public int compareUserObjects(final Object o2) {
    return 0;
  }

  public FilePath[] getFilePathsUnder() {
    return new FilePath[0];
  }
}