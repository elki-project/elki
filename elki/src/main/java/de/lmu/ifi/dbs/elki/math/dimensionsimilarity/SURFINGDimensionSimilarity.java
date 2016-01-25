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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.SubspaceEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Compute the similarity of dimensions using the SURFING score. The parameter k
 * for the k nearest neighbors is currently hard-coded to 10% of the set size.
 *
 * Note that the complexity is roughly O(n n k) * O(d^2), so this is a rather
 * slow method without index support, and with k at 10% of n, is actually cubic.
 * So try to use an appropriate index!
 *
 * Reference:
 * <p>
 * Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek:<br />
 * Interactive Data Mining with 3D-Parallel-Coordinate-Trees.<br />
 * Proceedings of the 2013 ACM International Conference on Management of Data
 * (SIGMOD), New York City, NY, 2013.
 * </p>
 *
 * Based on:
 * <p>
 * Christian Baumgartner, Claudia Plant, Karin Kailing, Hans-Peter Kriegel, and
 * Peer Kröger<br />
 * Subspace Selection for Clustering High-Dimensional Data<br />
 * In IEEE International Conference on Data Mining, 2004.
 * </p>
 *
 * TODO: make the subspace distance function and k parameterizable.
 *
 * @author Robert Rödler
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @apiviz.uses SubspaceEuclideanDistanceFunction
 */
@Reference(authors = "Elke Achtert, Hans-Peter Kriegel, Erich Schubert, Arthur Zimek", title = "Interactive Data Mining with 3D-Parallel-Coordinate-Trees", booktitle = "Proc. of the 2013 ACM International Conference on Management of Data (SIGMOD)", url = "http://dx.doi.org/10.1145/2463676.2463696")
public class SURFINGDimensionSimilarity implements DimensionSimilarity<NumberVector> {
  /**
   * Static instance.
   */
  public static final SURFINGDimensionSimilarity STATIC = new SURFINGDimensionSimilarity();

  /**
   * Constructor. Use static instance instead!
   */
  protected SURFINGDimensionSimilarity() {
    super();
  }

  @Reference(authors = "Christian Baumgartner, Claudia Plant, Karin Kailing, Hans-Peter Kriegel, and Peer Kröger", //
  title = "Subspace Selection for Clustering High-Dimensional Data", //
  booktitle = "IEEE International Conference on Data Mining, 2004", //
  url = "http://dx.doi.org/10.1109/ICDM.2004.10112")
  @Override
  public void computeDimensionSimilarites(Relation<? extends NumberVector> relation, DBIDs subset, DimensionSimilarityMatrix matrix) {
    final int dim = matrix.size();
    Mean kdistmean = new Mean();
    final int k = Math.max(1, subset.size() / 10);

    double[] knns = new double[subset.size()];

    // TODO: optimize by using 1d indexes?
    for(int x = 0; x < dim; x++) {
      final int i = matrix.dim(x);
      for(int y = x + 1; y < dim; y++) {
        final int j = matrix.dim(y);
        long[] dims = BitsUtil.zero(dim);
        BitsUtil.setI(dims, i);
        BitsUtil.setI(dims, j);
        final SubspaceEuclideanDistanceFunction df = new SubspaceEuclideanDistanceFunction(dims);
        KNNQuery<? extends NumberVector> knnq = relation.getKNNQuery(df, k);

        kdistmean.reset();
        int knn = 0;
        for(DBIDIter id1 = subset.iter(); id1.valid(); id1.advance(), knn++) {
          final double kdist = knnq.getKNNForDBID(id1, k).getKNNDistance();
          kdistmean.put(kdist);
          knns[knn] = kdist;
        }
        double mean = kdistmean.getMean();
        // Deviation from mean:
        double diff = 0.;
        int below = 0;
        for(int l = 0; l < knns.length; l++) {
          diff += Math.abs(mean - knns[l]);
          if(knns[l] < mean) {
            below++;
          }
        }
        matrix.set(x, y, (below > 0) ? diff / (2. * mean * below) : 0);
      }
    }
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
    protected SURFINGDimensionSimilarity makeInstance() {
      return STATIC;
    }
  }
}
