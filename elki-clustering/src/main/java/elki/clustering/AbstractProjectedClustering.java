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
package elki.clustering;

import elki.clustering.subspace.PROCLUS;
import elki.data.Clustering;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Abstract superclass for projected clustering algorithms, like {@link PROCLUS}
 * and {@link elki.clustering.correlation.ORCLUS}.
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @param <R> the result we return
 */
public abstract class AbstractProjectedClustering<R extends Clustering<?>> implements ClusteringAlgorithm<R> {
  /**
   * The number of clusters to find
   */
  protected int k;

  /**
   * Multiplier for the number of initial seeds
   */
  protected int k_i;

  /**
   * Dimensionality of clusters to find
   */
  protected int l;

  /**
   * Internal constructor.
   *
   * @param k The number of clusters to find
   * @param k_i Multiplier for the number of initial seeds
   * @param l Dimensionality of clusters to find
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
  public abstract static class Par implements Parameterizer {
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

    /**
     * The number of clusters to find
     */
    protected int k;

    /**
     * Multiplier for the number of initial seeds
     */
    protected int k_i;

    /**
     * Dimensionality of clusters to find
     */
    protected int l;
  }
}
