// Copyright (C) 2003, 2004 Philip Aston
// All rights reserved.
//
// This file is part of The Grinder software distribution. Refer to
// the file LICENSE which is part of The Grinder distribution for
// licensing details. The Grinder distribution is available on the
// Internet at http://grinder.sourceforge.net/
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
// STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
// OF THE POSSIBILITY OF SUCH DAMAGE.

package net.grinder.console.swingui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.grinder.console.common.ConsoleException;
import net.grinder.console.common.ErrorHandler;
import net.grinder.console.common.Resources;
import net.grinder.console.model.ConsoleProperties;
import net.grinder.console.model.editor.Buffer;
import net.grinder.console.model.editor.EditorModel;


/**
 * Panel containing controls for choosing script file set.
 *
 * @author Philip Aston
 * @version $Revision$
 */
final class FileTree {

  private final Resources m_resources;
  private final ErrorHandler m_errorHandler;
  private final EditorModel m_editorModel;
  private final FileTreeModel m_fileTreeModel;

  // Can't initialise tree until model has a valid directory.
  private final JTree m_tree;
  private final CustomAction m_openFileAction;
  private final JScrollPane m_scrollPane;

  public FileTree(final ConsoleProperties consoleProperties,
                  Resources resources,
                  ErrorHandler errorHandler,
                  EditorModel editorModel) {

    m_resources = resources;
    m_errorHandler = errorHandler;
    m_editorModel = editorModel;
    m_fileTreeModel = new FileTreeModel(m_editorModel);

    m_fileTreeModel.setRootDirectory(
      consoleProperties.getDistributionDirectory());

    consoleProperties.addPropertyChangeListener(
      new PropertyChangeListener()  {
        public void propertyChange(PropertyChangeEvent e) {
          if (e.getPropertyName().equals(
                ConsoleProperties.DISTRIBUTION_DIRECTORY_PROPERTY)) {
            m_fileTreeModel.setRootDirectory(
              consoleProperties.getDistributionDirectory());
          }
        }
      });

    final CompositeTreeModel compositeTreeModel = new CompositeTreeModel();

    compositeTreeModel.addTreeModel(m_fileTreeModel, true);

    final DefaultMutableTreeNode bufferRoot =
      new DefaultMutableTreeNode("Buffers");

    bufferRoot.add(new DefaultMutableTreeNode("bah"));
    bufferRoot.add(new DefaultMutableTreeNode("New file 1"));
    bufferRoot.add(new DefaultMutableTreeNode("New file 2"));
    bufferRoot.add(new DefaultMutableTreeNode("foo"));
    final TreeModel bufferModel = new DefaultTreeModel(bufferRoot);

    compositeTreeModel.addTreeModel(bufferModel, false);

    m_tree = new JTree(compositeTreeModel);
    m_tree.setRootVisible(false);
    m_tree.setShowsRootHandles(true);

    m_tree.setCellRenderer(new CustomTreeCellRenderer());
    m_tree.getSelectionModel().setSelectionMode(
      TreeSelectionModel.SINGLE_TREE_SELECTION);

    // Left double-click -> open file.
    m_tree.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          if (SwingUtilities.isLeftMouseButton(e)) {
            final TreePath path =
              m_tree.getPathForLocation(e.getX(), e.getY());

            if (path == null) {
              return;
            }

            final Object node = path.getLastPathComponent();

            if (node instanceof FileTreeModel.FileNode) {
              final FileTreeModel.FileNode fileNode =
                (FileTreeModel.FileNode)node;

              final int clickCount = e.getClickCount();

              if (clickCount == 1 && fileNode.getBuffer() != null ||
                  clickCount == 2) {
                selectFileNode(fileNode);
              }
            }
          }
        }
      });

    m_tree.addTreeSelectionListener(new TreeSelectionListener() {
        public void valueChanged(TreeSelectionEvent e) {
          updateActionState();
        }
      });

    m_openFileAction = new OpenFileAction();

    // J2SE 1.4 drops the mapping from "ENTER" -> "toggle"
    // (expand/collapse) that J2SE 1.3 has. I like it this mapping, so
    // we combine the "toggle" action with our OpenFileAction and let
    // TeeAction figure out which to call based on what's enabled.
    final InputMap inputMap = m_tree.getInputMap();

    inputMap.put(KeyStroke.getKeyStroke("ENTER"), "activateNode");
    inputMap.put(KeyStroke.getKeyStroke("SPACE"), "activateNode");

    final ActionMap actionMap = m_tree.getActionMap();
    actionMap.put("activateNode",
                  new TeeAction(actionMap.get("toggle"), m_openFileAction));

    m_scrollPane = new JScrollPane(m_tree);

    m_editorModel.addListener(new EditorModel.Listener() {
        public void bufferChanged(Buffer buffer) {

          final File file = buffer.getFile();

          if (file != null) {
            final FileTreeModel.FileNode oldFileNodeForBuffer =
              m_fileTreeModel.findFileNode(buffer);

            // Find a node, if its in our directory structure. This
            // may cause parts of the tree to be refreshed.
            final FileTreeModel.Node node = m_fileTreeModel.findNode(file);

            if (oldFileNodeForBuffer != null &&
                !oldFileNodeForBuffer.equals(node)) {
              oldFileNodeForBuffer.setBuffer(null);
            }

            if (node instanceof FileTreeModel.FileNode) {
              final FileTreeModel.FileNode fileNode =
                (FileTreeModel.FileNode)node;

              fileNode.setBuffer(buffer);

              final Object[] fileTreePath = fileNode.getPath().getPath();
              final Object[] path = new Object[fileTreePath.length + 1];
              System.arraycopy(fileTreePath, 0, path, 1, fileTreePath.length);
              path[0] = m_tree.getModel().getRoot();

              m_tree.scrollPathToVisible(new TreePath(path));
            }
          }

          // Couldn't find a nice way to repaint a single node. This:
          //    m_tree.getSelectionModel().setSelectionPath(fileNode.getPath());
          // doesn't work if the node is already selected. Give up and
          // repaint the world:
          m_tree.treeDidChange();
        }
      });
  }

  public JComponent getComponent() {
    return m_scrollPane;
  }

  public CustomAction getOpenFileAction() {
    return m_openFileAction;
  }

  /**
   * Action for opening the currently selected file in the tree.
   */
  private final class OpenFileAction extends CustomAction {
    public OpenFileAction() {
      super(m_resources, "open-file");
    }

    public void actionPerformed(ActionEvent e) {
      final FileTreeModel.FileNode fileNode = getSelectedFileNode();

      if (fileNode != null) {
        selectFileNode(fileNode);
      }
    }
  }

  private void updateActionState() {
    m_openFileAction.setEnabled(m_tree.isEnabled() &&
                                getSelectedFileNode() != null);
  }

  private FileTreeModel.FileNode getSelectedFileNode() {
    final Object node = m_tree.getLastSelectedPathComponent();

    if (node instanceof FileTreeModel.FileNode) {
      return (FileTreeModel.FileNode)node;
    }
    else {
      return null;
    }
  }

  /**
   * Custom cell renderer.
   */
  private final class CustomTreeCellRenderer extends DefaultTreeCellRenderer {
    private final DefaultTreeCellRenderer m_defaultRenderer =
      new DefaultTreeCellRenderer();

    private final Font m_boldFont;
    private final Font m_boldItalicFont;
    private final ImageIcon m_pythonIcon =
      m_resources.getImageIcon("script.pythonfile.image");

    private boolean m_active;

    CustomTreeCellRenderer() {
      m_boldFont = new JLabel().getFont().deriveFont(Font.BOLD);
      m_boldItalicFont = m_boldFont.deriveFont(Font.BOLD | Font.ITALIC);
    }

    public Component getTreeCellRendererComponent(
      JTree tree, Object value, boolean selected, boolean expanded,
      boolean leaf, int row, boolean hasFocus) {

      if (value instanceof FileTreeModel.FileNode) {
        final FileTreeModel.FileNode fileNode = (FileTreeModel.FileNode)value;

        setLeafIcon(fileNode.isPythonFile() ?
                    m_pythonIcon : m_defaultRenderer.getLeafIcon());

        final Buffer buffer = fileNode.getBuffer();

        setTextNonSelectionColor(
          fileNode.isBoringFile() && buffer == null ?
          Colours.INACTIVE_TEXT :
          m_defaultRenderer.getTextNonSelectionColor());

        if (buffer != null) {
          // File is open.
          setFont(buffer.isDirty() ? m_boldItalicFont : m_boldFont);
        }
        else {
          setFont(m_defaultRenderer.getFont());
        }

        m_active = m_editorModel.getSelectedBuffer() == buffer;

        return super.getTreeCellRendererComponent(
          tree, value, selected, expanded, leaf, row, hasFocus);
      }
      else {
        return m_defaultRenderer.getTreeCellRendererComponent(
          tree, value, selected, expanded, leaf, row, hasFocus);
      }
    }

    /**
     * Our parent overrides validate() and revalidate() for speed.
     * This means it never resizes. Go with this, but be a few pixels
     * wider to allow text to be italicised.
     */
    public Dimension getPreferredSize() {
      final Dimension result = super.getPreferredSize();

      return result != null ?
        new Dimension(result.width + 3, result.height) : null;
    }

    public void paint(Graphics g) {
      final Color backgroundColour;

      if (m_active) {
        backgroundColour = Colours.FAINT_YELLOW;
      }
      else if (selected) {
        backgroundColour = getBackgroundSelectionColor();
      }
      else {
        backgroundColour = getBackgroundNonSelectionColor();
      }

      if (backgroundColour != null) {
        g.setColor(backgroundColour);
        g.fillRect(0, 0, getWidth() - 1, getHeight());
      }

      // Sigh. The whole reason we override paint is that the
      // DefaultTreeCellRenderer version is crap. We can't call
      // super.super.paint() so we work hard to make the
      // DefaultTreeCellRenderer version ineffectual.

      final boolean oldHasFocus = hasFocus;
      final boolean oldSelected = selected;
      final Color oldBackgroundNonSelectionColour =
        getBackgroundNonSelectionColor();

      try {
        hasFocus = false;
        selected = false;
        setBackgroundNonSelectionColor(backgroundColour);

        super.paint(g);
      }
      finally {
        hasFocus = oldHasFocus;
        selected = oldSelected;
        setBackgroundNonSelectionColor(oldBackgroundNonSelectionColour);
      }

      // Now draw our border.
      final Color borderColour;

      if (m_active) {
        borderColour = getTextNonSelectionColor();
      }
      else if (hasFocus) {
        borderColour = getBorderSelectionColor();
      }
      else {
        borderColour = null;
      }

      if (borderColour != null) {
        g.setColor(borderColour);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
      }
    }
  }

  private void selectFileNode(FileTreeModel.FileNode fileNode) {
    try {
      fileNode.setBuffer(
        m_editorModel.selectBufferForFile(fileNode.getFile()));
    }
    catch (ConsoleException e) {
      m_errorHandler.handleException(
        e, m_resources.getString("fileError.title"));
    }
  }
}


