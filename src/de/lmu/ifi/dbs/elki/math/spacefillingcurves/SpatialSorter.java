package de.lmu.ifi.dbs.elki.math.spacefillingcurves;

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

import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

/**
 * Interface for spatial sorting - ZCurves, Peano curves, Hilbert curves, ...
 * 
 * @author Erich Schubert
 */
public interface SpatialSorter {
  /**
   * Partitions the specified feature vectors
   * 
   * @param <T> actual type we sort
   * @param objs the spatial objects to be sorted
   */
  <T extends SpatialComparable> void sort(List<T> objs);

  /**
   * Sort part of the list (start to end).
   * 
   * @param <T> actual type we sort
   * @param objs the spatial objects to be sorted
   * @param start First index to sort (e.g. 0)
   * @param end End of range (e.g. <code>site()</code>)
   * @param minmax Array with dim pairs of (min, max) of value ranges
   */
  <T extends SpatialComparable> void sort(List<T> objs, int start, int end, double[] minmax);
}