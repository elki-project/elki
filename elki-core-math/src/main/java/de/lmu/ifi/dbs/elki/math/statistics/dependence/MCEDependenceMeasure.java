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
package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import java.util.ArrayList;
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arrays.IntegerArrayQuickSort;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.FastMath;

/**
 * Compute a mutual information based dependence measure using a nested means
 * discretization, originally proposed for ordering axes in parallel coordinate
 * plots.
 * <p>
 * Reference:
 * <p>
 * D. Guo<br>
 * Coordinating computational and visual approaches for interactive feature
 * selection and multivariate clustering<br>
 * Information Visualization, 2(4), 2003.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "D. Guo", //
    title = "Coordinating computational and visual approaches for interactive feature selection and multivariate clustering", //
    booktitle = "Information Visualization, 2(4)", //
    url = "https://doi.org/10.1057/palgrave.ivs.9500053", //
    bibkey = "DBLP:journals/ivs/Guo03")
public class MCEDependenceMeasure extends AbstractDependenceMeasure {
  /**
   * Static instance.
   */
  public static final MCEDependenceMeasure STATIC = new MCEDependenceMeasure();

  /**
   * Desired size: 35 observations.
   * 
   * While this could trivially be made parameterizable, it is a reasonable rule
   * of thumb and not expected to have a major effect.
   */
  public static final int TARGET = 35;

  @Override
  public <A, B> double dependence(NumberArrayAdapter<?, A> adapter1, A data1, NumberArrayAdapter<?, B> adapter2, B data2) {
    final int len = size(adapter1, data1, adapter2, data2);
    // Find a number of bins as recommended by Cheng et al.
    double p = MathUtil.log2(len / (double) TARGET);
    // As we are in 2d, take the root (*.5) But let's use at least 1, too.
    // Check: for 10000 this should give 4, for 150 it gives 1.
    int power = Math.max(1, (int) Math.floor(p * .5));
    int gridsize = 1 << power;
    double loggrid = FastMath.log((double) gridsize);

    ArrayList<int[]> parts1 = buildPartitions(adapter1, data1, len, power);
    ArrayList<int[]> parts2 = buildPartitions(adapter2, data2, len, power);

    int[][] res = new int[gridsize][gridsize];
    intersectionMatrix(res, parts1, parts2, gridsize);
    return 1. - getMCEntropy(res, parts1, parts2, len, gridsize, loggrid);
  }

  /**
   * Partitions an attribute.
   * 
   * @param adapter1 Data adapter
   * @param data1 Data set
   * @param len Length of data
   * @param depth Splitting depth
   * @return List of sorted objects
   */
  private <A> ArrayList<int[]> buildPartitions(NumberArrayAdapter<?, A> adapter1, A data1, int len, int depth) {
    final int[] idx = new int[len];
    final double[] tmp = new double[len];
    for(int i = 0; i < len; ++i) {
      idx[i] = i;
      tmp[i] = adapter1.getDouble(data1, i);
    }
    // Sort indexes:
    IntegerArrayQuickSort.sort(idx, (x, y) -> Double.compare(tmp[x], tmp[y]));
    Arrays.sort(tmp); // Should yield the same ordering

    ArrayList<int[]> ret = new ArrayList<>(1 << depth);
    divide(idx, tmp, ret, 0, tmp.length, depth);
    return ret;
  }

  /**
   * Recursive call to further subdivide the array.
   * 
   * @param idx Object indexes.
   * @param data 1D data, sorted
   * @param ret Output index
   * @param start Interval start
   * @param end Interval end
   * @param depth Depth
   */
  private void divide(int[] idx, double[] data, ArrayList<int[]> ret, int start, int end, int depth) {
    if(depth == 0) {
      int[] a = Arrays.copyOfRange(idx, start, end);
      Arrays.sort(a);
      ret.add(a);
      return;
    }
    final int count = end - start;
    if(count == 0) {
      // Corner case, that should barely happen. But for ties, we currently
      // Do not yet assure that it doesn't happen!
      for(int j = 1 << depth; j > 0; --j) {
        ret.add(new int[0]);
      }
      return;
    }
    double m = 0.;
    for(int i = start; i < end; i++) {
      m += data[i];
    }
    m /= count;
    int pos = Arrays.binarySearch(data, start, end, m);
    if(pos >= 0) {
      // Ties: try to choose the most central element.
      final int opt = (start + end) >> 1;
      while(data[pos] == m) {
        if(pos < opt) {
          pos++;
        }
        else if(pos > opt) {
          pos--;
        }
        else {
          break;
        }
      }
    }
    else {
      pos = (-pos - 1);
    }
    divide(idx, data, ret, start, pos, depth - 1);
    divide(idx, data, ret, pos, end, depth - 1);
  }

  /**
   * Intersect the two 1d grid decompositions, to obtain a 2d matrix.
   * 
   * @param res Output matrix to fill
   * @param partsx Partitions in first component
   * @param partsy Partitions in second component.
   * @param gridsize Size of partition decomposition
   */
  private void intersectionMatrix(int[][] res, ArrayList<int[]> partsx, ArrayList<int[]> partsy, int gridsize) {
    for(int x = 0; x < gridsize; x++) {
      final int[] px = partsx.get(x);
      final int[] rowx = res[x];
      for(int y = 0; y < gridsize; y++) {
        int[] py = partsy.get(y);
        rowx[y] = intersectionSize(px, py);
      }
    }
  }

  /**
   * Compute the intersection of two sorted integer lists.
   * 
   * @param px First list
   * @param py Second list
   * @return Intersection size.
   */
  private int intersectionSize(int[] px, int[] py) {
    int i = 0, j = 0, c = 0;
    while(i < px.length && j < py.length) {
      final int vx = px[i], vy = py[j];
      if(vx < vy) {
        ++i;
      }
      else if(vx > vy) {
        ++j;
      }
      else {
        ++c;
        ++i;
        ++j;
      }
    }
    return c;
  }

  /**
   * Compute the MCE entropy value.
   * 
   * @param mat Partition size matrix
   * @param partsx Partitions on X
   * @param partsy Partitions on Y
   * @param size Data set size
   * @param gridsize Size of grids
   * @param loggrid Logarithm of grid sizes, for normalization
   * @return MCE score.
   */
  private double getMCEntropy(int[][] mat, ArrayList<int[]> partsx, ArrayList<int[]> partsy, int size, int gridsize, double loggrid) {
    // Margin entropies:
    double[] mx = new double[gridsize];
    double[] my = new double[gridsize];

    for(int i = 0; i < gridsize; i++) {
      // Note: indexes are a bit tricky here, because we compute both margin
      // entropies at the same time!
      final double sumx = (double) partsx.get(i).length;
      final double sumy = (double) partsy.get(i).length;
      for(int j = 0; j < gridsize; j++) {
        double px = mat[i][j] / sumx;
        double py = mat[j][i] / sumy;

        if(px > 0.) {
          mx[i] -= px * FastMath.log(px);
        }
        if(py > 0.) {
          my[i] -= py * FastMath.log(py);
        }
      }
    }

    // Weighted sums of margin entropies.
    double sumx = 0., sumy = 0.;
    for(int i = 0; i < gridsize; i++) {
      sumx += mx[i] * partsx.get(i).length;
      sumy += my[i] * partsy.get(i).length;
    }

    double max = ((sumx > sumy) ? sumx : sumy);
    return max / (size * loggrid);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected MCEDependenceMeasure makeInstance() {
      return STATIC;
    }
  }
}
