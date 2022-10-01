/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.clustering.kmeans.initialization;

import java.util.List;

import elki.data.NumberVector;
import elki.data.VectorUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRef;
import elki.database.relation.Relation;
import elki.distance.*;
import elki.logging.Logging;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.random.RandomFactory;

/**
 * Spherical K-Means++ initialization with markov chains.
 * <p>
 * FIXME: currently assumes the vectors to be L2 normalized beforehand, but does
 * not ensure that this is true.
 * <p>
 * Reference:
 * <p>
 * R. Pratap, A. A. Deshmukh, P. Nair, T. Dutt<br>
 * A Faster Sampling Algorithm for Spherical k-means<br>
 * Proc. 10th Asian Conference on Machine Learning, ACML
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
@Title("Spherical AFK-MC²")
@Reference(authors = "R. Pratap, A. A. Deshmukh, P. Nair, T. Dutt", //
    title = "A Faster Sampling Algorithm for Spherical k-means", //
    booktitle = "Proc. 10th Asian Conference on Machine Learning, ACML", //
    url = "http://proceedings.mlr.press/v95/pratap18a.html", //
    bibkey = "DBLP:conf/acml/PratapDND18")
public class SphericalAFKMC2 extends AFKMC2 {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(SphericalAFKMC2.class);

  /**
   * Parameter to balance distance vs. uniform sampling.
   */
  protected double alpha;

  /**
   * Constructor.
   *
   * @param m M parameter
   * @param alpha alpha-SKM parameter, usually 1.5
   * @param rnd Random generator.
   */
  public SphericalAFKMC2(int m, double alpha, RandomFactory rnd) {
    super(m, rnd);
    this.alpha = alpha;
  }

  @Override
  public double[][] chooseInitialMeans(Relation<? extends NumberVector> relation, int k, NumberVectorDistance<?> distance) {
    if(relation.size() < k) {
      throw new IllegalArgumentException("Cannot choose k=" + k + " means from N=" + relation.size() + " < k objects.");
    }
    if(distance instanceof CosineDistance || distance instanceof CosineUnitlengthDistance //
        || distance instanceof ArcCosineDistance || distance instanceof ArcCosineUnitlengthDistance) {
      return new Instance(relation, m, alpha, rnd).run(k);
    }
    LOG.warning("Spherical k-means++ was used with an instance of " + distance.getClass() + ". Falling back to regular k-means++.");
    return new AFKMC2.Instance(relation, distance, m, rnd).run(k);
  }

  /**
   * Abstract instance implementing the weight handling.
   *
   * @author Erich Schubert
   */
  protected static class Instance extends AFKMC2.Instance {
    /**
     * Parameter to balance distance vs. uniform sampling.
     */
    protected double alpha;

    /**
     * Constructor.
     *
     * @param relation Data relation
     * @param alpha Alpha parameter
     * @param m M parameter
     * @param rnd Random generator
     */
    public Instance(Relation<? extends NumberVector> relation, int m, double alpha, RandomFactory rnd) {
      super(relation, CosineDistance.STATIC, m, rnd);
      this.alpha = alpha;
    }

    @Override
    protected double initialWeights(NumberVector first) {
      double weightsum = 0.;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        // Distance will usually already be squared
        double weight = alpha - similarity(first, it);
        weights.putDouble(it, weight);
        weightsum += weight;
      }
      return weightsum;
    }

    /**
     * Compute the distance of two objects.
     *
     * @param a First object
     * @param b Second object
     * @return Distance
     */
    protected double similarity(NumberVector a, DBIDRef b) {
      ++diststat;
      return VectorUtil.dot(a, relation.get(b));
    }

    @Override
    protected double distance(DBIDRef cand, List<NumberVector> means) {
      double d = weights.doubleValue(cand); // distance to first
      for(int i = 1; i < means.size(); i++) {
        double d2 = alpha - similarity(means.get(i), cand);
        d = d2 < d ? d2 : d;
      }
      return d;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par extends AFKMC2.Par {
    /**
     * Alpha parameter, usually 1.5
     */
    public static final OptionID ALPHA_ID = SphericalKMeansPlusPlus.Par.ALPHA_ID;

    /**
     * Parameter to balance distance vs. uniform sampling.
     */
    protected double alpha;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new DoubleParameter(ALPHA_ID, 1.5) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_DOUBLE) //
          .grab(config, x -> alpha = x);
    }

    @Override
    public SphericalAFKMC2 make() {
      return new SphericalAFKMC2(m, alpha, rnd);
    }
  }
}
