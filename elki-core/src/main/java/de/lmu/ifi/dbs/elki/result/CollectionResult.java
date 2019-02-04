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
package de.lmu.ifi.dbs.elki.result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Simple 'collection' type of result.
 * For example, a list of NumberVectors.
 * 
 * @author Erich Schubert
 * @since 0.2
 * 
 * @opt nodefillcolor LemonChiffon
 *
 * @param <O> data type
 */
public class CollectionResult<O> extends BasicResult implements IterableResult<O> {
  /**
   * The collection represented.
   */
  private Collection<O> col;
  
  /**
   * Meta information (printed into the header)
   */
  private Collection<String> header;
  
  /**
   * Constructor
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param col Collection represented
   * @param header Auxiliary information for result headers
   */
  public CollectionResult(String name, String shortname, Collection<O> col, Collection<String> header) {
    super(name, shortname);
    this.col = col;
    this.header = header;
  }
  
  /**
   * Constructor
   * 
   * @param name The long name (for pretty printing)
   * @param shortname the short name (for filenames etc.)
   * @param col Collection represented
   */
  public CollectionResult(String name, String shortname, Collection<O> col) {
    this(name, shortname, col, new ArrayList<String>());
  }
  
  /**
   * Add header information
   * 
   * @param s Header information string
   */
  public void addHeader(String s) {
    header.add(s);
  }
  
  /**
   * Get header information
   * 
   * @return header information of the result
   */
  public Collection<String> getHeader() {
    return header;
  }

  /**
   * Implementation of the {@link IterableResult} interface, using the backing collection.
   */
  @Override
  public Iterator<O> iterator() {
    return col.iterator();
  }
  
  /**
   * Get the collection size.
   * 
   * @return Collection size
   */
  public int size() {
    return col.size();
  }
}