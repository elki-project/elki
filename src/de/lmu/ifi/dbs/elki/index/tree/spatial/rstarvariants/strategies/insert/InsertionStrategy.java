package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert;
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

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * RTree insertion strategy interface.
 * 
 * @author Erich Schubert
 */
public interface InsertionStrategy extends Parameterizable {
  /**
   * Choose insertion rectangle.
   * 
   * @param options Options to choose from
   * @param getter Array adapter for options
   * @param obj Insertion object
   * @param height Tree height
   * @param depth Insertion depth (depth == height - 1 indicates leaf level)
   * @return Subtree index in array.
   */
  public <A> int choose(A options, ArrayAdapter<? extends SpatialComparable, A> getter, SpatialComparable obj, int height, int depth);
}