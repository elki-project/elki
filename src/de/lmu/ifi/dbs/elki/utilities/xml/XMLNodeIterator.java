package de.lmu.ifi.dbs.elki.utilities.xml;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import org.w3c.dom.Node;

import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Simple adapter class to iterate over a DOM tree nodes children.
 * 
 * @author Erich Schubert
 */
public final class XMLNodeIterator implements Iterator<Node>, Iterable<Node> {
  /**
   * Store the next node
   */
  private Node next;

  /**
   * Constructor with first element to iterate over.
   * 
   * @param first first child of parent node.
   */
  public XMLNodeIterator(Node first) {
    super();
    this.next = first;
  }

  /**
   * Check if the next node is defined. 
   */
  @Override
  public boolean hasNext() {
    return (next != null);
  }

  /**
   * Return next and advance iterator.
   */
  @Override
  public Node next() {
    Node cur = next;
    next = next.getNextSibling();
    return cur;
  }

  /**
   * Removal: unsupported operation.
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException(this.getClass().getSimpleName()+": "+ExceptionMessages.UNSUPPORTED_REMOVE);
  }

  /**
   * Iterator interface adapter - clone.
   */
  @Override
  public Iterator<Node> iterator() {
    return new XMLNodeIterator(this.next);
  }  
}