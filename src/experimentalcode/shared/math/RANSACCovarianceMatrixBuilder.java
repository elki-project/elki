package experimentalcode.shared.math;

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

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.AbstractCovarianceMatrixBuilder;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.ChiSquaredDistribution;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * RANSAC based approach to a more robust covariance matrix computation.
 * 
 * This is an adoption of RANSAC to this problem, not a generic RANSAC
 * implementation!
 * 
 * Reference:
 * <p>
 * Random sample consensus: a paradigm for model fitting with applications to
 * image analysis and automated cartography<br />
 * M.A. Fischler, R.C. Bolles<br />
 * Communications of the ACM, Vol. 24 Issue 6
 * </p>
 * 
 * TODO: make random seed parameterizable
 * 
 * TODO: make iterations parameteriable OR compute a good estimate. Performance
 * in high dimensionality seems to be an issue!
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
@Reference(title = "Random sample consensus: a paradigm for model fitting with applications to image analysis and automated cartography", authors = "M.A. Fischler, R.C. Bolles", booktitle = "Communications of the ACM, Vol. 24 Issue 6", url = "http://dx.doi.org/10.1145/358669.358692")
public class RANSACCovarianceMatrixBuilder<V extends NumberVector<V, ?>> extends AbstractCovarianceMatrixBuilder<V> {
  int iterations = 1000;

  Random random = new Random(0);

  @Override
  public Matrix processIds(DBIDs ids, Relation<? extends V> relation) {
    final int dim = DatabaseUtil.dimensionality(relation);

    DBIDs best = DBIDUtil.EMPTYDBIDS;
    double tresh = ChiSquaredDistribution.quantile(0.85, dim);

    for(int i = 0; i < iterations; i++) {
      DBIDs sample = DBIDUtil.randomSample(ids, dim + 1, random.nextLong());
      CovarianceMatrix cv = CovarianceMatrix.make(relation, sample);
      Vector centroid = cv.getMeanVector();
      Matrix p = cv.destroyToSampleMatrix().inverse();

      ModifiableDBIDs support = DBIDUtil.newHashSet();
      for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
        Vector vec = relation.get(id).getColumnVector().minusEquals(centroid);
        double sqlen = vec.transposeTimesTimes(p, vec);
        if(sqlen < tresh) {
          support.add(id);
        }
      }

      if(support.size() > best.size()) {
        best = support;
      }
      if(support.size() >= ids.size()) {
        break; // Can't get better than this!
      }
    }
    // logger.warning("Consensus size: "+best.size()+" of "+ids.size());
    // Fall back to regular PCA
    if(best.size() <= dim) {
      return CovarianceMatrix.make(relation, ids).destroyToSampleMatrix();
    }
    // Return estimation based on consensus set.
    return CovarianceMatrix.make(relation, best).destroyToSampleMatrix();
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <V> Vector type
   */
  public static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
    @Override
    protected RANSACCovarianceMatrixBuilder<V> makeInstance() {
      return new RANSACCovarianceMatrixBuilder<V>();
    }
  }
}