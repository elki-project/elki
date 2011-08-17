package de.lmu.ifi.dbs.elki.utilities.datastructures.hierarchy;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.iterator.EmptyIterator;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;

/**
 * Hierarchy implementation with a per-object representation.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Hierarchical
 * 
 * @param <O> Type of objects in hierarchy
 */
public class HierarchyReferenceLists<O extends Hierarchical<O>> implements Hierarchy<O> {
  /**
   * Owner
   */
  protected O owner;

  /**
   * Storage for children
   */
  protected List<O> children;

  /**
   * Storage for parents
   */
  protected List<O> parents;

  /**
   * Constructor for hierarchy object.
   * 
   * @param owner owning cluster.
   * @param children child clusters. May be null.
   * @param parents parent clusters. May be null.
   */
  public HierarchyReferenceLists(O owner, List<O> children, List<O> parents) {
    super();
    this.owner = owner;
    this.children = children;
    this.parents = parents;
  }

  @Override
  public int numChildren(O self) {
    if(owner != self) {
      throw new UnsupportedOperationException("Decentral hierarchy queried for wrong object!");
    }
    if(children == null) {
      return 0;
    }
    return children.size();
  }

  @Override
  public List<O> getChildren(O self) {
    if(owner != self) {
      throw new UnsupportedOperationException("Decentral hierarchy queried for wrong object!");
    }
    return children;
  }

  @Override
  public IterableIterator<O> iterDescendants(O self) {
    if(owner != self) {
      return EmptyIterator.STATIC();
    }
    if (children == null) {
      return EmptyIterator.STATIC();
    }
    return new ItrDesc(self);
  }

  @Override
  public int numParents(O self) {
    if(owner != self) {
      throw new UnsupportedOperationException("Decentral hierarchy queried for wrong object!");
    }
    if (parents == null) {
      return 0;
    }
    return parents.size();
  }

  /**
   * Return parents
   */
  @Override
  public List<O> getParents(O self) {
    if(owner != self) {
      throw new UnsupportedOperationException("Decentral hierarchy queried for wrong object!");
    }
    return parents;
  }

  @Override
  public IterableIterator<O> iterAncestors(O self) {
    if(owner != self) {
      throw new UnsupportedOperationException("Decentral hierarchy queried for wrong object!");
    }
    if (parents == null) {
      return EmptyIterator.STATIC();
    }
    return new ItrAnc(self);
  }

  /**
   * Iterator to collect into the descendants.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class ItrDesc implements IterableIterator<O> {
    /**
     * Iterator over children
     */
    final Iterator<O> childiter;

    /**
     * Iterator of current child
     */
    Iterator<O> subiter;

    public ItrDesc(O start) {
      assert (start == owner);
      this.childiter = children.iterator();
      this.subiter = null;
    }

    @Override
    public boolean hasNext() {
      if(subiter != null && subiter.hasNext()) {
        return true;
      }
      return childiter.hasNext();
    }

    @Override
    public O next() {
      // Try nested iterator first ...
      if(subiter != null && subiter.hasNext()) {
        return subiter.next();
      }
      // Next direct child, update subiter.
      final O child = childiter.next();
      subiter = child.iterDescendants();
      return child;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<O> iterator() {
      return new ItrDesc(owner);
    }
  }

  /**
   * Iterator over all Ancestors.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  private class ItrAnc implements IterableIterator<O> {
    /**
     * Iterator over parents
     */
    final Iterator<O> parentiter;

    /**
     * Iterator of current parent
     */
    Iterator<O> subiter;

    public ItrAnc(O start) {
      assert (start == owner);
      this.parentiter = parents.iterator();
      this.subiter = null;
    }

    @Override
    public boolean hasNext() {
      if(subiter != null && subiter.hasNext()) {
        return true;
      }
      return parentiter.hasNext();
    }

    @Override
    public O next() {
      // Try nested iterator first ...
      if(subiter != null && subiter.hasNext()) {
        return subiter.next();
      }
      // Next direct parent, update subiter.
      final O parent = parentiter.next();
      subiter = parent.iterAncestors();
      return parent;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<O> iterator() {
      return new ItrAnc(owner);
    }
  }
}