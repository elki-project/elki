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

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Collection of objects and their distances.
 * 
 * To iterate over the results, use the following code:
 * 
 * <pre>
 * {@code
 * for (DistanceDBIDResultIter<D> iter = result.iter(); iter.valid(); iter.advance()) {
 *   // You can get the distance via: iter.getDistance();
 *   // Or use iter just like any other DBIDRef
 * }
 * </pre>
 * 
 * If you are only interested in the IDs of the objects, the following is also
 * sufficient:
 * 
 * <pre>
 * {@code
 * for (DBIDIter<D> iter = result.iter(); iter.valid(); iter.advance()) {
 *   // Use iter just like any other DBIDRef
 * }
 * </pre>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf DistanceResultPair
 * @apiviz.has DistanceResultPairIter
 * 
 * @param <D> Distance type
 */
public interface DistanceDBIDResult<D extends Distance<D>> extends DBIDs {
  /**
   * Size of list.
   * 
   * @return Size
   */
  @Override
  public int size();

  /**
   * Access a single pair.
   * 
   * @param off Offset
   * @return Pair
   */
  public DistanceDBIDPair<D> get(int off);

  /**
   * Get an iterator
   * 
   * @return New iterator
   */
  @Override
  public DistanceDBIDResultIter<D> iter();

  /**
   * Sort the result in ascending order
   */
  public void sort();
}