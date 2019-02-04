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
package de.lmu.ifi.dbs.elki.utilities.xml;

import java.util.Iterator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Simple adapter class to iterate over a DOM tree nodes children.
 *
 * @author Erich Schubert
 * @since 0.2
 */
public final class XMLNodeListIterator implements Iterator<Node> {
  /**
   * Store the next node
   */
  private int index;

  /**
   * The {@link NodeList} to iterate over.
   */
  private NodeList nodelist;

  /**
   * Constructor with first element to iterate over.
   *
   * @param nl NodeList to iterate over.
   */
  public XMLNodeListIterator(NodeList nl) {
    super();
    this.nodelist = nl;
    this.index = 0;
  }

  /**
   * Check if the next node is defined.
   */
  @Override
  public boolean hasNext() {
    return (this.index < this.nodelist.getLength());
  }

  /**
   * Return next and advance iterator.
   */
  @Override
  public Node next() {
    Node cur = this.nodelist.item(this.index);
    this.index++;
    return cur;
  }
}