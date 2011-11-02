package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.reinsert;

import java.util.Arrays;
import java.util.Collections;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;

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

/**
 * Reinsert objects on page overflow, starting with farther objects first (even
 * when they will likely be inserted into the same page again!)
 * 
 * Alternative strategy mentioned in the R*-tree
 * 
 * @author Erich Schubert
 */
@Reference(authors = "N. Beckmann, H.-P. Kriegel, R. Schneider, B. Seeger", title = "The R*-tree: an efficient and robust access method for points and rectangles", booktitle = "Proceedings of the 1990 ACM SIGMOD International Conference on Management of Data, Atlantic City, NJ, May 23-25, 1990", url = "http://dx.doi.org/10.1145/93597.98741")
public class FarReinsert extends AbstractPartialReinsert {
  /**
   * Constructor.
   *
   * @param reinsertAmount Amount to reinsert
   * @param distanceFunction Distance function
   */
  public FarReinsert(double reinsertAmount, SpatialPrimitiveDoubleDistanceFunction<?> distanceFunction) {
    super(reinsertAmount, distanceFunction);
  }

  @Override
  public <E extends SpatialComparable, A> int[] computeReinserts(A entries, ArrayAdapter<E, A> getter, SpatialComparable page) {
    DoubleIntPair[] order = new DoubleIntPair[getter.size(entries)];
    for(int i = 0; i < order.length; i++) {
      double distance = distanceFunction.doubleCenterDistance(getter.get(entries, i), page);
      order[i] = new DoubleIntPair(distance, i);
    }
    Arrays.sort(order, Collections.reverseOrder());

    int num = (int) (reinsertAmount * order.length);
    int[] re = new int[num];
    for(int i = 0; i < num; i++) {
      re[i] = order[i].second;
    }
    return re;
  }
  
  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractPartialReinsert.Parameterizer {
    @Override
    protected Object makeInstance() {
      return new CloseReinsert(reinsertAmount, distanceFunction);
    }
  }
}