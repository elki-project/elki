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
package de.lmu.ifi.dbs.elki.utilities.datastructures.histogram;

import de.lmu.ifi.dbs.elki.utilities.datastructures.iterator.ArrayIter;

/**
 * Abstract API for histograms. Without specific type information, to allow this
 * to be shared for primitive types, too!
 * 
 * @author Erich Schubert
 * @since 0.5.5
 * 
 * @has - - - Iter
 */
public interface Histogram {
  /**
   * This parameter controls the cache size used for dynamic histograms before
   * setting the initial thresholds.
   */
  int CACHE_SHIFT = 2;

  /**
   * Get the number of bins actually in use.
   * 
   * @return number of bins
   */
  int getNumBins();

  /**
   * Get the size (width) of a bin.
   * 
   * @return bin size
   */
  double getBinsize();

  /**
   * Get minimum (covered by bins, not data!)
   * 
   * @return minimum
   */
  double getCoverMinimum();

  /**
   * Get maximum (covered by bins, not data!)
   * 
   * @return maximum
   */
  double getCoverMaximum();

  /**
   * Get an iterator over all histogram bins.
   * 
   * @return Iterator
   */
  Iter iter();

  /**
   * Array iterator.
   * 
   * @author Erich Schubert
   */
  interface Iter extends ArrayIter {
    /**
     * Get the bin center.
     * 
     * @return bin center value
     */
    double getCenter();

    /**
     * Get the bin minimum.
     * 
     * @return bin left value
     */
    double getLeft();

    /**
     * Get the bin maximum.
     * 
     * @return bin right value
     */
    double getRight();
  }
}
