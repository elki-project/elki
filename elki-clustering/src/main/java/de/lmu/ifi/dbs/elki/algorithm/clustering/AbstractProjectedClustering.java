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
package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.PROCLUS;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Abstract superclass for projected clustering algorithms, like {@link PROCLUS}
 * and {@link de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.ORCLUS}.
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @param <R> the result we return
 * @param <V> the type of FeatureVector handled by this Algorithm
 */
public abstract class AbstractProjectedClustering<R extends Clustering<?>, V extends NumberVector> extends AbstractAlgorithm<R> implements ClusteringAlgorithm<R> {
  /**
   * Holds the value of {@link Parameterizer#K_ID}.
   */
  protected int k;

  /**
   * Holds the value of {@link Parameterizer#K_I_ID}.
   */
  protected int k_i;

  /**
   * Holds the value of {@link Parameterizer#L_ID}.
   */
  protected int l;

  /**
   * Internal constructor.
   *
   * @param k K parameter
   * @param k_i K_i parameter
   * @param l L parameter
   */
  public AbstractProjectedClustering(int k, int k_i, int l) {
    super();
    this.k = k;
    this.k_i = k_i;
    this.l = l;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the number of clusters to find, must be an integer
     * greater than 0.
     */
    public static final OptionID K_ID = new OptionID("projectedclustering.k", "The number of clusters to find.");

    /**
     * Parameter to specify the multiplier for the initial number of seeds, must
     * be an integer greater than 0.
     */
    public static final OptionID K_I_ID = new OptionID("projectedclustering.k_i", "The multiplier for the initial number of seeds.");

    /**
     * Parameter to specify the dimensionality of the clusters to find, must be
     * an integer greater than 0.
     */
    public static final OptionID L_ID = new OptionID("projectedclustering.l", "The dimensionality of the clusters to find.");

    protected int k;

    protected int k_i;

    protected int l;
  }
}
