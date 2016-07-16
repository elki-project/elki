package de.lmu.ifi.dbs.elki.math.dimensionsimilarity;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.VectorUtil.SortDBIDsBySingleDimension;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Compute dimension similarity by using a nested means discretization.
 *
 * Reference:
 * <p>
 * D. Guo<br />
 * Coordinating computational and visual approaches for interactive feature
 * selection and multivariate clustering<br />
 * Information Visualization, 2(4), 2003.
 * </p>
 *
 * @author Erich Schubert
 * @since 0.5.5
 */
@Reference(authors = "D. Guo", //
title = "Coordinating computational and visual approaches for interactive feature selection and multivariate clustering", //
booktitle = "Information Visualization, 2(4)", //
url = "http://dx.doi.org/10.1057/palgrave.ivs.9500053")
public class MCEDimensionSimilarity implements DimensionSimilarity<NumberVector> {
  /**
   * Static instance.
   */
  public static final MCEDimensionSimilarity STATIC = new MCEDimensionSimilarity();

  /**
   * Desired size: 35 observations.
   *
   * While this could trivially be made parameterizable, it is a reasonable rule
   * of thumb and not expected to have a major effect.
   */
  public static final int TARGET = 35;

  /**
   * Constructor. Use static instance instead!
   */
  protected MCEDimensionSimilarity() {
    super();
  }

  @Override
  public void computeDimensionSimilarites(Relation<? extends NumberVector> relation, DBIDs subset, DimensionSimilarityMatrix matrix) {
    final int dim = matrix.size();

    // Find a number of bins as recommended by Cheng et al.
    double p = MathUtil.log2(subset.size() / (double) TARGET);
    // As we are in 2d, take the root (*.5) But let's use at least 1, too.
    // Check: for 10000 this should give 4, for 150 it gives 1.
    int power = Math.max(1, (int) Math.floor(p * .5));
    int gridsize = 1 << power;
    double loggrid = Math.log((double) gridsize);

    ArrayList<ArrayList<DBIDs>> parts = buildPartitions(relation, subset, power, matrix);

    // Partition sizes
    int[][] psizes = new int[dim][gridsize];
    for(int d = 0; d < dim; d++) {
      final ArrayList<DBIDs> partsd = parts.get(d);
      final int[] sizesp = psizes[d];
      for(int i = 0; i < gridsize; i++) {
        sizesp[i] = partsd.get(i).size();
      }
    }

    int[][] res = new int[gridsize][gridsize];
    for(int x = 0; x < dim; x++) {
      ArrayList<DBIDs> partsi = parts.get(x);
      for(int y = x + 1; y < dim; y++) {
        ArrayList<DBIDs> partsj = parts.get(y);
        // Fill the intersection matrix
        intersectionMatrix(res, partsi, partsj, gridsize);
        matrix.set(x, y, 1. - getMCEntropy(res, psizes[x], psizes[y], subset.size(), gridsize, loggrid));
      }
    }
  }

  /**
   * Calculates "index structures" for every attribute, i.e. sorts a
   * ModifiableArray of every DBID in the database for every dimension and
   * stores them in a list.
   *
   * @param relation Relation to index
   * @param ids IDs to use
   * @param matrix Matrix for dimension information
   * @return List of sorted objects
   */
  private ArrayList<ArrayList<DBIDs>> buildPartitions(Relation<? extends NumberVector> relation, DBIDs ids, int depth, DimensionSimilarityMatrix matrix) {
    final int dim = matrix.size();
    ArrayList<ArrayList<DBIDs>> subspaceIndex = new ArrayList<>(dim);
    SortDBIDsBySingleDimension comp = new VectorUtil.SortDBIDsBySingleDimension(relation);
    double[] tmp = new double[ids.size()];
    Mean mean = new Mean();

    for(int i = 0; i < dim; i++) {
      final int d = matrix.dim(i);
      // Index for a single dimension:
      ArrayList<DBIDs> idx = new ArrayList<>(1 << depth);
      // First, we need a copy of the DBIDs and sort it.
      ArrayModifiableDBIDs sids = DBIDUtil.newArray(ids);
      comp.setDimension(d);
      sids.sort(comp);
      // Now we build the temp array, and compute the first mean.
      DBIDArrayIter it = sids.iter();
      for(int j = 0; j < tmp.length; j++, it.advance()) {
        assert (it.valid());
        tmp[j] = relation.get(it).doubleValue(d);
      }
      divide(it, tmp, idx, 0, tmp.length, depth, mean);
      assert (idx.size() == (1 << depth));
      subspaceIndex.add(idx);
    }

    return subspaceIndex;
  }

  /**
   * Recursive call to further subdivide the array.
   *
   * @param it Iterator (will be reset!)
   * @param data 1D data, sorted
   * @param idx Output index
   * @param start Interval start
   * @param end Interval end
   * @param depth Depth
   * @param mean Mean working variable (will be reset!)
   */
  private void divide(DBIDArrayIter it, double[] data, ArrayList<DBIDs> idx, int start, int end, int depth, Mean mean) {
    final int count = end - start;
    if(depth == 0) {
      if(count > 0) {
        ModifiableDBIDs out = DBIDUtil.newHashSet(count);
        it.seek(start);
        for(int i = count; i > 0; i--, it.advance()) {
          out.add(it);
        }
        idx.add(out);
      }
      else {
        idx.add(DBIDUtil.EMPTYDBIDS);
      }
      return;
    }
    else {
      if(count > 0) {
        mean.reset();
        for(int i = start; i < end; i++) {
          mean.put(data[i]);
        }
        final double m = mean.getMean();
        int pos = Arrays.binarySearch(data, start, end, m);
        if(pos >= 0) {
          // Ties: try to choose the most central element.
          int opt = (start + end) >> 1;
          while(Double.compare(data[pos], m) == 0) {
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
        divide(it, data, idx, start, pos, depth - 1, mean);
        divide(it, data, idx, pos, end, depth - 1, mean);
      }
      else {
        // Corner case, that should barely happen. But for ties, we currently
        // Do not yet assure that it doesn't happen!
        divide(it, data, idx, start, end, depth - 1, mean);
        divide(it, data, idx, start, end, depth - 1, mean);
      }
    }
  }

  /**
   * Intersect the two 1d grid decompositions, to obtain a 2d matrix.
   *
   * @param res Output matrix to fill
   * @param partsx Partitions in first component
   * @param partsy Partitions in second component.
   * @param gridsize Size of partition decomposition
   */
  private void intersectionMatrix(int[][] res, ArrayList<? extends DBIDs> partsx, ArrayList<? extends DBIDs> partsy, int gridsize) {
    for(int x = 0; x < gridsize; x++) {
      final DBIDs px = partsx.get(x);
      final int[] rowx = res[x];
      for(int y = 0; y < gridsize; y++) {
        rowx[y] = DBIDUtil.intersectionSize(px, partsy.get(y));
      }
    }
  }

  /**
   * Compute the MCE entropy value.
   *
   * @param mat Partition size matrix
   * @param psizesx Partition sizes on X
   * @param psizesy Partition sizes on Y
   * @param size Data set size
   * @param gridsize Size of grids
   * @param loggrid Logarithm of grid sizes, for normalization
   * @return MCE score.
   */
  private double getMCEntropy(int[][] mat, int[] psizesx, int[] psizesy, int size, int gridsize, double loggrid) {
    // Margin entropies:
    double[] mx = new double[gridsize];
    double[] my = new double[gridsize];

    for(int i = 0; i < gridsize; i++) {
      // Note: indexes are a bit tricky here, because we compute both margin
      // entropies at the same time!
      final double sumx = (double) psizesx[i];
      final double sumy = (double) psizesy[i];
      for(int j = 0; j < gridsize; j++) {
        double px = mat[i][j] / sumx;
        double py = mat[j][i] / sumy;

        if(px > 0.) {
          mx[i] -= px * Math.log(px);
        }
        if(py > 0.) {
          my[i] -= py * Math.log(py);
        }
      }
    }

    // Weighted sums of margin entropies.
    double sumx = 0., sumy = 0.;
    for(int i = 0; i < gridsize; i++) {
      sumx += mx[i] * psizesx[i];
      sumy += my[i] * psizesy[i];
    }

    double max = ((sumx > sumy) ? sumx : sumy);
    return max / (size * loggrid);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected MCEDimensionSimilarity makeInstance() {
      return STATIC;
    }
  }
}
