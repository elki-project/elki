package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

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
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.CovarianceMatrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.ChiSquaredDistribution;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * RANSAC based approach to a more robust covariance matrix computation.
 * 
 * This is an <b>experimental</b> adoption of RANSAC to this problem, not a
 * generic RANSAC implementation!
 * 
 * While using RANSAC for PCA at first sounds like a good idea, <b>it does not
 * work very well in high-dimensional spaces</b>. The problem is that PCA has
 * O(n^2) degrees of freedom, so we need to sample very many objects, then
 * perform an O(n^3) matrix operation to compute PCA, for each attempt.
 * 
 * References:
 * 
 * RANSAC for PCA was a side note in:
 * <p>
 * Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek<br />
 * Outlier Detection in Arbitrarily Oriented Subspaces<br />
 * in: Proc. IEEE International Conference on Data Mining (ICDM 2012)
 * </p>
 * 
 * The basic RANSAC idea was explained in:
 * <p>
 * Random sample consensus: a paradigm for model fitting with applications to
 * image analysis and automated cartography<br />
 * M.A. Fischler, R.C. Bolles<br />
 * Communications of the ACM, Vol. 24 Issue 6
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", title = "Outlier Detection in Arbitrarily Oriented Subspaces", booktitle = "Proc. IEEE International Conference on Data Mining (ICDM 2012)")
public class RANSACCovarianceMatrixBuilder<V extends NumberVector<?>> extends AbstractCovarianceMatrixBuilder<V> {
  /**
   * Number of iterations to perform
   */
  int iterations = 1000;

  /**
   * Random generator
   */
  RandomFactory rnd;

  /**
   * Constructor.
   * 
   * @param iterations Number of iterations (attempts) to try
   * @param rnd random generator
   */
  public RANSACCovarianceMatrixBuilder(int iterations, RandomFactory rnd) {
    super();
    this.iterations = iterations;
    this.rnd = rnd;
  }

  @Reference(title = "Random sample consensus: a paradigm for model fitting with applications to image analysis and automated cartography", authors = "M.A. Fischler, R.C. Bolles", booktitle = "Communications of the ACM, Vol. 24 Issue 6", url = "http://dx.doi.org/10.1145/358669.358692")
  @Override
  public Matrix processIds(DBIDs ids, Relation<? extends V> relation) {
    final int dim = RelationUtil.dimensionality(relation);

    DBIDs best = DBIDUtil.EMPTYDBIDS;
    double tresh = ChiSquaredDistribution.quantile(0.85, dim);

    for (int i = 0; i < iterations; i++) {
      DBIDs sample = DBIDUtil.randomSample(ids, dim + 1, rnd);
      CovarianceMatrix cv = CovarianceMatrix.make(relation, sample);
      Vector centroid = cv.getMeanVector();
      Matrix p = cv.destroyToSampleMatrix().inverse();

      ModifiableDBIDs support = DBIDUtil.newHashSet();
      for (DBIDIter id = ids.iter(); id.valid(); id.advance()) {
        Vector vec = relation.get(id).getColumnVector().minusEquals(centroid);
        double sqlen = vec.transposeTimesTimes(p, vec);
        if (sqlen < tresh) {
          support.add(id);
        }
      }

      if (support.size() > best.size()) {
        best = support;
      }
      if (support.size() >= ids.size()) {
        break; // Can't get better than this!
      }
    }
    // logger.warning("Consensus size: "+best.size()+" of "+ids.size());
    // Fall back to regular PCA
    if (best.size() <= dim) {
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
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
    /**
     * Number of iterations.
     */
    public static final OptionID ITER_ID = OptionID.getOrCreateOptionID("ransacpca.iterations", "The number of iterations to perform.");

    /**
     * Random seed
     */
    public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("ransacpca.seed", "Random seed (optional).");

    /**
     * Number of iterations to perform
     */
    int iterations = 1000;

    /**
     * Random generator
     */
    RandomFactory rnd;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter iterP = new IntParameter(ITER_ID, 1000);
      iterP.addConstraint(new GreaterConstraint(0));
      if (config.grab(iterP)) {
        iterations = iterP.intValue();
      }
      RandomParameter rndP = new RandomParameter(SEED_ID);
      if (config.grab(rndP)) {
        rnd = rndP.getValue();
      }
    }

    @Override
    protected RANSACCovarianceMatrixBuilder<V> makeInstance() {
      return new RANSACCovarianceMatrixBuilder<V>(iterations, rnd);
    }
  }
}
