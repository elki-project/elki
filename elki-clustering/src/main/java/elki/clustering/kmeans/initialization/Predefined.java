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
package elki.clustering.kmeans.initialization;

import java.util.List;

import elki.data.Cluster;
import elki.data.NumberVector;
import elki.data.model.MeanModel;
import elki.database.relation.Relation;
import elki.distance.NumberVectorDistance;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleArrayListParameter;

/**
 * Run k-means with prespecified initial means.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class Predefined extends AbstractKMeansInitialization {
  /**
   * Initial means to return.
   */
  double[][] initialMeans;

  /**
   * Constructor.
   *
   * @param initialMeans Initial means
   */
  public Predefined(double[][] initialMeans) {
    super(null);
    this.initialMeans = initialMeans;
  }

  /**
   * Set the initial means.
   *
   * Important notice: Use with care - the means are <em>not copied</em>!
   *
   * @param initialMeans initial means.
   */
  public void setInitialMeans(List<double[]> initialMeans) {
    this.setInitialMeans(initialMeans);
  }

  /**
   * Set the initial means.
   *
   * Important notice: Use with care - the means are <em>not copied</em>!
   *
   * @param initialMeans initial means.
   */
  public void setInitialClusters(List<? extends Cluster<? extends MeanModel>> initialMeans) {
    double[][] vecs = new double[initialMeans.size()][];
    for(int i = 0; i < vecs.length; i++) {
      vecs[i] = initialMeans.get(i).getModel().getMean();
    }
    this.initialMeans = vecs;
  }

  /**
   * Set the initial means.
   *
   * Important notice: Use with care - the means are <em>not copied</em>!
   *
   * @param initialMeans initial means.
   */
  public void setInitialMeans(double[][] initialMeans) {
    double[][] vecs = initialMeans.clone(); // TODO: deep copy?
    this.initialMeans = vecs;
  }

  @Override
  public double[][] chooseInitialMeans(Relation<? extends NumberVector> relation, int k, NumberVectorDistance<?> distance) {
    if(k != initialMeans.length) {
      throw new AbortException("Predefined initial means contained " + initialMeans.length //
          + " means, algorithm requested " + k + " means instead.");
    }
    return initialMeans;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Option to specify the initial means to use.
     */
    public static final OptionID INITIAL_MEANS = new OptionID("kmeans.means", "Initial means for k-means.");

    /**
     * Initial means.
     */
    protected double[][] initialMeans;

    @Override
    public void configure(Parameterization config) {
      new DoubleArrayListParameter(INITIAL_MEANS) //
          .grab(config, x -> initialMeans = x.toArray(new double[0][]));
    }

    @Override
    public Predefined make() {
      return new Predefined(initialMeans);
    }
  }
}
