package de.lmu.ifi.dbs.elki.database.query;

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

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Default class to keep a list of distance-object pairs.
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
public class GenericDistanceDBIDList<D extends Distance<D>> extends ArrayList<DistanceResultPair<D>> implements DistanceDBIDResult<D> {
  /**
   * Serialization Version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   */
  public GenericDistanceDBIDList() {
    super();
  }

  /**
   * Constructor.
   * 
   * @param c existing collection
   */
  public GenericDistanceDBIDList(Collection<? extends DistanceResultPair<D>> c) {
    super(c);
  }

  /**
   * Constructor.
   * 
   * @param initialCapacity Capacity
   */
  public GenericDistanceDBIDList(int initialCapacity) {
    super(initialCapacity);
  }
  
  /**
   * Add an element.
   * 
   * @param dist Distance
   * @param id ID
   */
  public void add(D dist, DBIDRef id) {
    add(new GenericDistanceResultPair<D>(dist, id));
  }
}