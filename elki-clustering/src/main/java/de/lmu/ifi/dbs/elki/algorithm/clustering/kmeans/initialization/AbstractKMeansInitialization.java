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
package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Abstract base class for common k-means initializations.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public abstract class AbstractKMeansInitialization implements KMeansInitialization {
  /**
   * Random number generator
   */
  protected RandomFactory rnd;

  /**
   * Constructor.
   * 
   * @param rnd Random number generator.
   */
  public AbstractKMeansInitialization(RandomFactory rnd) {
    this.rnd = rnd;
  }

  /**
   * Unbox database means to primitive means.
   * 
   * @param means Database means
   * @return List of primitive {@code double[]} means
   */
  public static double[][] unboxVectors(List<? extends NumberVector> means) {
    double[][] ret = new double[means.size()][];
    for(int i = 0; i < ret.length; i++) {
      ret[i] = means.get(i).toArray();
    }
    return ret;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Random generator
     */
    protected RandomFactory rnd;

    @Override
    protected void makeOptions(Parameterization config) {
      RandomParameter rndP = new RandomParameter(KMeans.SEED_ID);
      if(config.grab(rndP)) {
        rnd = rndP.getValue();
      }
    }
  }
}
