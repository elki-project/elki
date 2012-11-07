package experimentalcode.shared.parallelcoord;

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

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.subspace.SubspaceEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

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
 * 
 * @apiviz.uses SubspaceEuclideanDistanceFunction
 */
@Reference(authors = "Christian Baumgartner, Claudia Plant, Karin Kailing, Hans-Peter Kriegel, and Peer Kröger", title = "Subspace Selection for Clustering High-Dimensional Data", booktitle = "IEEE International Conference on Data Mining, 2004", url = "http://dx.doi.org/10.1109/ICDM.2004.10112")
public class SURFINGDimensionSimilarity implements DimensionSimilarity<NumberVector<?>> {
  @Override
  public double[][] computeDimensionSimilarites(Relation<? extends NumberVector<?>> relation, DBIDs subset) {
    final int dim = RelationUtil.dimensionality(relation);
    final Database db = relation.getDatabase();
    double[][] mat = new double[dim][dim];
    Mean kdistmean = new Mean();
    final int k = Math.max(1, subset.size() / 10);

    double[] knns = new double[subset.size()];

    for (int i = 0; i < dim - 1; i++) {
      for (int j = i + 1; j < dim; j++) {
        BitSet dims = new BitSet(dim);
        dims.set(i);
        dims.set(j);
        DistanceQuery<? extends NumberVector<?>, DoubleDistance> dq = db.getDistanceQuery(relation, new SubspaceEuclideanDistanceFunction(dims));
        KNNQuery<? extends NumberVector<?>, DoubleDistance> knnq = db.getKNNQuery(dq, k);

        kdistmean.reset();
        int knn = 0;
        for (DBIDIter id1 = subset.iter(); id1.valid(); id1.advance(), knn++) {
          final double kdist = knnq.getKNNForDBID(id1, k).getKNNDistance().doubleValue();
          kdistmean.put(kdist);
          knns[knn] = kdist;
        }
        double mean = kdistmean.getMean();
        // Deviation from mean:
        double diff = 0.;
        int below = 0;
        for (int l = 0; l < knns.length; l++) {
          diff += Math.abs(mean - knns[l]);
          if (knns[l] < mean) {
            below++;
          }
        }
        final double quality = (below > 0) ? diff / (2. * mean * below) : 0;
        mat[i][j] = quality;
        mat[j][i] = quality;
      }
    }
    return mat;
  }
}
