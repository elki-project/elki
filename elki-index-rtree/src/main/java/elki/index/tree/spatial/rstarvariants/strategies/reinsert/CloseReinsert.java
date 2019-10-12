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
package elki.index.tree.spatial.rstarvariants.strategies.reinsert;

import java.util.Arrays;

import elki.data.DoubleVector;
import elki.data.spatial.SpatialComparable;
import elki.data.spatial.SpatialUtil;
import elki.distance.SpatialPrimitiveDistance;
import elki.math.MathUtil;
import elki.utilities.datastructures.arraylike.ArrayAdapter;
import elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;
import elki.utilities.documentation.Reference;

/**
 * Reinsert objects on page overflow, starting with close objects first (even
 * when they will likely be inserted into the same page again!)
 * <p>
 * The strategy preferred by the R*-Tree
 * <p>
 * Reference:
 * <p>
 * Norbert Beckmann, Hans-Peter Kriegel, Ralf Schneider, Bernhard Seeger<br>
 * The R*-tree: an efficient and robust access method for points and
 * rectangles<br>
 * Proc. 1990 ACM SIGMOD Int. Conf. Management of Data
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
@Reference(authors = "Norbert Beckmann, Hans-Peter Kriegel, Ralf Schneider, Bernhard Seeger", //
    title = "The R*-tree: an efficient and robust access method for points and rectangles", //
    booktitle = "Proc. 1990 ACM SIGMOD Int. Conf. Management of Data", //
    url = "https://doi.org/10.1145/93597.98741", //
    bibkey = "DBLP:conf/sigmod/BeckmannKSS90")
public class CloseReinsert extends AbstractPartialReinsert {
  /**
   * Constructor.
   * 
   * @param reinsertAmount Amount of objects to reinsert
   * @param distance Distance function to use for reinsertion
   */
  public CloseReinsert(double reinsertAmount, SpatialPrimitiveDistance<?> distance) {
    super(reinsertAmount, distance);
  }

  @Override
  public <A> int[] computeReinserts(A entries, ArrayAdapter<? extends SpatialComparable, ? super A> getter, SpatialComparable page) {
    DoubleVector centroid = DoubleVector.wrap(SpatialUtil.centroid(page));
    final int size = getter.size(entries);
    double[] dist = new double[size];
    int[] idx = MathUtil.sequence(0, size);
    for(int i = 0; i < size; i++) {
      dist[i] = distance.minDist(DoubleVector.wrap(SpatialUtil.centroid(getter.get(entries, i))), centroid);
    }
    DoubleIntegerArrayQuickSort.sort(dist, idx, size);
    return Arrays.copyOf(idx, (int) (reinsertAmount * size));
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par extends AbstractPartialReinsert.Par {
    @Override
    public CloseReinsert make() {
      return new CloseReinsert(reinsertAmount, distance);
    }
  }
}