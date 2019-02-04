/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.gui.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import de.lmu.ifi.dbs.elki.utilities.ELKIServiceScanner;

/**
 * Build a tree of available classes for use in Swing UIs.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - TreeNode
 * @composed - - - PackageNode
 * @composed - - - ClassNode
 */
public final class ClassTree {
  /**
   * Private constructor. Static methods only.
   */
  private ClassTree() {
    // Do not use.
  }

  /**
   * Build the class tree for a given set of choices.
   *
   * @param choices Class choices
   * @param rootpkg Root package name (to strip / hide)
   * @return Root node.
   */
  public static TreeNode build(List<Class<?>> choices, String rootpkg) {
    MutableTreeNode root = new PackageNode(rootpkg, rootpkg);
    HashMap<String, MutableTreeNode> lookup = new HashMap<>();
    if(rootpkg != null) {
      lookup.put(rootpkg, root);
    }
    lookup.put("de.lmu.ifi.dbs.elki", root);
    lookup.put("", root);

    // Use the shorthand version of class names.
    String prefix = rootpkg != null ? rootpkg + "." : null;

    Class<?>[] choic = choices.toArray(new Class<?>[choices.size()]);
    Arrays.sort(choic, ELKIServiceScanner.SORT_BY_PRIORITY);
    for(Class<?> impl : choic) {
      String name = impl.getName();
      name = (prefix != null && name.startsWith(prefix)) ? name.substring(prefix.length()) : name;
      int plen = (impl.getPackage() != null) ? impl.getPackage().getName().length() + 1 : 0;
      MutableTreeNode c = new ClassNode(impl.getName().substring(plen), name);

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
        MutableTreeNode tmp = new PackageNode(l >= 0 ? pname.substring(l + 1) : pname, pname);
        tmp.insert(c, 0);
        c = tmp;
        lookup.put(pname, tmp);
        name = pname;
      }
      p.insert(c, p.getChildCount());
    }
    // Simplify tree, except for root node
    for(int i = 0; i < root.getChildCount(); i++) {
      MutableTreeNode c = (MutableTreeNode) root.getChildAt(i);
      MutableTreeNode c2 = simplifyTree(c, null);
      if(c != c2) {
        root.remove(i);
        root.insert(c2, i);
      }
    }
    return root;
  }

  /**
   * Simplify the tree.
   *
   * @param cur Current node
   * @param prefix Prefix to add
   * @return Replacement node
   */
  private static MutableTreeNode simplifyTree(MutableTreeNode cur, String prefix) {
    if(cur instanceof PackageNode) {
      PackageNode node = (PackageNode) cur;
      if(node.getChildCount() == 1) {
        String newprefix = (prefix != null) ? prefix + "." + (String) node.getUserObject() : (String) node.getUserObject();
        cur = simplifyTree((MutableTreeNode) node.getChildAt(0), newprefix);
      }
      else {
        if(prefix != null) {
          node.setUserObject(prefix + "." + (String) node.getUserObject());
        }
        for(int i = 0; i < node.getChildCount(); i++) {
          MutableTreeNode c = (MutableTreeNode) node.getChildAt(i);
          MutableTreeNode c2 = simplifyTree(c, null);
          if(c != c2) {
            node.remove(i);
            node.insert(c2, i);
          }
        }
      }
    }
    else if(cur instanceof ClassNode) {
      ClassNode node = (ClassNode) cur;
      if(prefix != null) {
        node.setUserObject(prefix + "." + (String) node.getUserObject());
      }
    }
    return cur;
  }

  /**
   * Tree node representing a single class.
   *
   * @author Erich Schubert
   */
  public static class PackageNode extends DefaultMutableTreeNode {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Class name.
     */
    private String pkgname;

    /**
     * Current class name.
     *
     * @param display Displayed name
     * @param pkgname Actual class name
     */
    public PackageNode(String display, String pkgname) {
      super(display);
      this.pkgname = pkgname;
    }

    /**
     * Return the package name.
     *
     * @return Package name
     */
    public String getPackageName() {
      return pkgname;
    }
  }

  /**
   * Tree node representing a single class.
   *
   * @author Erich Schubert
   */
  public static class ClassNode extends DefaultMutableTreeNode {
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

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
  }
}
