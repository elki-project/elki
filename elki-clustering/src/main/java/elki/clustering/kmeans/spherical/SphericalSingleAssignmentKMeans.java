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
package elki.clustering.kmeans.spherical;

import elki.clustering.kmeans.initialization.KMeansInitialization;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.model.KMeansModel;
import elki.database.relation.Relation;
import elki.logging.Logging;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Pseudo-k-Means variations, that assigns each object to the nearest center.
 *
 * @author Alexander Voß
 * @author Erich Schubert
 * @author Andreas Lang
 * @since 0.8.0
 *
 * @navassoc - - - KMeansModel
 *
 * @param <V> vector datatype
 */
public class SphericalSingleAssignmentKMeans<V extends NumberVector> extends SphericalKMeans<V> {
  /**
   * Class logger
   */
  private static final Logging LOG = Logging.getLogger(SphericalSingleAssignmentKMeans.class);

  /**
   * Constructor.
   *
   * @param k Number of clusters
   * @param initializer Initialization class
   */
  public SphericalSingleAssignmentKMeans(int k, KMeansInitialization initializer) {
    super(k, 1, initializer);
  }

  @Override
  public Clustering<KMeansModel> run(Relation<V> relation) {
    Instance instance = new Instance(relation, initialMeans(relation));
    instance.run(1);
    return instance.buildResult();
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Instance for a particular data set.
   * 
   * @author Alexander Voß
   * @author Andreas Lang
   */
  public static class Instance extends SphericalKMeans.Instance {
    /**
     * Constructor.
     *
     * @param relation Data relation
     * @param means Initial cluster means
     */
    public Instance(Relation<? extends NumberVector> relation, double[][] means) {
      super(relation, means);
    }

    @Override
    public int iterate(int iteration) {
      assert (iteration == 1);
      return -assignToNearestCluster();
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Alexander Voß
   * @author Andreas Lang
   */
  public static class Par<V extends NumberVector> extends SphericalKMeans.Par<V> {
    @Override
    public void configure(Parameterization config) {
      getParameterK(config);
      getParameterInitialization(config);
    }

    @Override
    public SphericalSingleAssignmentKMeans<V> make() {
      return new SphericalSingleAssignmentKMeans<V>(k, initializer);
    }
  }
}
