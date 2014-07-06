package de.lmu.ifi.dbs.elki.gui.util;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import java.util.HashMap;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 * Build a tree of available classes for use in Swing UIs.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has TreeNode
 */
public class ClassTree {
  /**
   * Build the class tree for a given set of choices.
   * 
   * @param choices Class choices
   * @param rootpkg Root package name (to strip / hide)
   * @return Root node.
   */
  public static TreeNode build(List<Class<?>> choices, String rootpkg) {
    MutableTreeNode root = new DefaultMutableTreeNode(rootpkg);
    HashMap<String, MutableTreeNode> lookup = new HashMap<>();
    if(rootpkg != null) {
      lookup.put(rootpkg, root);
    }
    lookup.put("de.lmu.ifi.dbs.elki", root);
    lookup.put("", root);

    // Use the shorthand version of class names.
    String prefix = rootpkg != null ? rootpkg + "." : null;

    for(Class<?> impl : choices) {
      String name = impl.getName();
      name = (prefix != null && name.startsWith(prefix)) ? name.substring(prefix.length()) : name;
      MutableTreeNode c = new ClassNode(impl.getSimpleName(), name);

      MutableTreeNode p = null;
      int l = name.lastIndexOf('.');
      while(p == null) {
        if(l < 0) {
          p = root;
          break;
        }
        String pname = name.substring(0, l);
        p = lookup.get(pname);
        if(p != null) {
          break;
        }
        l = pname.lastIndexOf('.');
        MutableTreeNode tmp = new DefaultMutableTreeNode(l >= 0 ? pname.substring(l + 1) : pname);
        tmp.insert(c, 0);
        c = tmp;
        lookup.put(pname, tmp);
        name = pname;
      }
      p.insert(c, p.getChildCount());
    }
    return root;
  }

  /**
   * Tree node representing a single class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class ClassNode extends DefaultMutableTreeNode {
    /**
     * Class name.
     */
    private String clsname;

    /**
     * Current class name.
     * 
     * @param display Displayed name
     * @param clsname Actual class name
     */
    public ClassNode(String display, String clsname) {
      super(display);
      this.clsname = clsname;
    }

    /**
     * Return the class name.
     * 
     * @return Class name
     */
    public String getClassName() {
      return clsname;
    }

    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;
  }
}
