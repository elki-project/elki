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
package elki.math.linearalgebra.pca;

import static elki.math.linearalgebra.VMath.inverse;
import static elki.math.linearalgebra.VMath.minusEquals;
import static elki.math.linearalgebra.VMath.transposeTimesTimes;

import java.util.Random;

import elki.data.NumberVector;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.ids.ModifiableDBIDs;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.math.linearalgebra.CovarianceMatrix;
import elki.math.statistics.distribution.ChiSquaredDistribution;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * RANSAC based approach to a more robust covariance matrix computation.
 * <p>
 * This is an <b>experimental</b> adoption of RANSAC to this problem, not a
 * generic RANSAC implementation!
 * <p>
 * While using RANSAC for PCA at first sounds like a good idea, <b>it does not
 * work very well in high-dimensional spaces</b>. The problem is that PCA has
 * O(n²) degrees of freedom, so we need to sample very many objects, then
 * perform an O(n³) matrix operation to compute PCA, for each attempt.
 * <p>
 * References:
 * <p>
 * RANSAC for PCA was a side note in:
 * <p>
 * Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek<br>
 * Outlier Detection in Arbitrarily Oriented Subspaces<br>
 * In: Proc. IEEE International Conference on Data Mining (ICDM 2012)
 * <p>
 * The basic RANSAC idea was explained in:
 * <p>
 * Random sample consensus: a paradigm for model fitting with applications to
 * image analysis and automated cartography<br>
 * M. A. Fischler, R. C. Bolles<br>
 * Communications of the ACM 24(6)
 * 
 * @author Erich Schubert
 * @since 0.5.5
 */
@Reference(authors = "Hans-Peter Kriegel, Peer Kröger, Erich Schubert, Arthur Zimek", //
    title = "Outlier Detection in Arbitrarily Oriented Subspaces", //
    booktitle = "Proc. IEEE Int. Conf. on Data Mining (ICDM 2012)", //
    url = "https://doi.org/10.1109/ICDM.2012.21", //
    bibkey = "DBLP:conf/icdm/KriegelKSZ12")
@Reference(title = "Random sample consensus: a paradigm for model fitting with applications to image analysis and automated cartography", //
    authors = "M. A. Fischler, R. C. Bolles", //
    booktitle = "Communications of the ACM 24(6)", //
    url = "https://doi.org/10.1145/358669.358692", //
    bibkey = "DBLP:journals/cacm/FischlerB81")
public class RANSACCovarianceMatrixBuilder implements CovarianceMatrixBuilder {
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

  @Override
  public double[][] processIds(DBIDs ids, Relation<? extends NumberVector> relation) {
    final int dim = RelationUtil.dimensionality(relation);

    ModifiableDBIDs best = DBIDUtil.newHashSet(),
        support = DBIDUtil.newHashSet();
    double tresh = ChiSquaredDistribution.quantile(0.85, dim);

    CovarianceMatrix cv = new CovarianceMatrix(dim);
    Random random = rnd.getSingleThreadedRandom();
    for(int i = 0; i < iterations; i++) {
      DBIDs sample = DBIDUtil.randomSample(ids, dim + 1, random);
      cv.reset();
      for(DBIDIter it = sample.iter(); it.valid(); it.advance()) {
        cv.put(relation.get(it));
      }
      double[] centroid = cv.getMeanVector();
      double[][] p = inverse(cv.destroyToSampleMatrix());

      support.clear();
      for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
        double[] vec = minusEquals(relation.get(id).toArray(), centroid);
        double sqlen = transposeTimesTimes(vec, p, vec);
        if(sqlen < tresh) {
          support.add(id);
        }
      }

      if(support.size() > best.size()) {
        ModifiableDBIDs swap = best;
        best = support;
        support = swap;
      }
      if(support.size() >= ids.size()) {
        break; // Can't get better than this!
      }
    }
    // logger.warning("Consensus size: "+best.size()+" of "+ids.size());
    // Fall back to regular PCA if too few samples.
    return CovarianceMatrix.make(relation, best.size() > dim ? best : ids).destroyToSampleMatrix();
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Number of iterations.
     */
    public static final OptionID ITER_ID = new OptionID("ransacpca.iterations", "The number of iterations to perform.");

    /**
     * Random seed
     */
    public static final OptionID SEED_ID = new OptionID("ransacpca.seed", "Random seed (optional).");

    /**
     * Number of iterations to perform
     */
    int iterations = 1000;

    /**
     * Random generator
     */
    RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(ITER_ID, 1000) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> iterations = x);
      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
    }

    @Override
    public RANSACCovarianceMatrixBuilder make() {
      return new RANSACCovarianceMatrixBuilder(iterations, rnd);
    }
  }
}
