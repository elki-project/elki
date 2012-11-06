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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.Mean;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleMaxHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.visualization.projections.ProjectionParallel;

/**
 * Compute the similarity of dimensions using the SURFING score. The parameter k
 * for the k nearest neighbors is currently hard-coded to 10% of the set size.
 * 
 * Note that the complexity is roughly O(d*n*n*k), so this is a rather slow
 * method without index support, and with k at 10% of n, is actually cubic.
 * 
 * Effectively, we are computing Euclidean distances in the <em>rescaled</em>
 * space used for parallel coordinates projection - but as such, we currently do
 * not use indexes.
 * 
 * Reference:
 * <p>
 * Christian Baumgartner, Claudia Plant, Karin Kailing, Hans-Peter Kriegel, and
 * Peer Kröger<br />
 * Subspace Selection for Clustering High-Dimensional Data<br />
 * In IEEE International Conference on Data Mining, 2004.
 * </p>
 * 
 * @author Robert Rödler
 */
@Reference(authors = "Christian Baumgartner, Claudia Plant, Karin Kailing, Hans-Peter Kriegel, and Peer Kröger", title = "Subspace Selection for Clustering High-Dimensional Data", booktitle = "IEEE International Conference on Data Mining, 2004", url = "http://dx.doi.org/10.1109/ICDM.2004.10112")
public class SURFINGDimensionSimilarity implements DimensionSimilarity<NumberVector<?>> {
  @Override
  public double[][] computeDimensionSimilarites(Relation<? extends NumberVector<?>> relation, ProjectionParallel proj, DBIDs subset) {
    final int dim = RelationUtil.dimensionality(relation);
    double[][] mat = new double[dim][dim];
    Mean kdistmean = new Mean();
    final int k = Math.max(1, subset.size() / 10);
    DoubleMaxHeap heap = new DoubleMaxHeap(k);
    double[] knns = new double[subset.size()];

    for (int i = 0; i < dim - 1; i++) {
      for (int j = i + 1; j < dim; j++) {
        kdistmean.reset();
        for (DBIDIter id1 = subset.iter(); id1.valid(); id1.advance()) {
          final double[] o1vec = proj.fastProjectDataToRenderSpace(relation.get(id1));
          int knn = 0;
          for (DBIDIter id2 = subset.iter(); id2.valid(); id2.advance()) {
            if (DBIDUtil.equal(id1, id2)) {
              continue;
            }
            final double[] o2vec = proj.fastProjectDataToRenderSpace(relation.get(id2));
            final double x = o1vec[i] - o2vec[i];
            final double y = o1vec[j] - o2vec[j];
            final double dist = MathUtil.fastHypot(x, y);
            heap.add(dist, k);
          }
          final double kdist = heap.peek();
          kdistmean.put(kdist);
          knns[knn] = kdist;
          knn++;
          heap.clear();
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
