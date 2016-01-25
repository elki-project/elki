package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeans;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;

/**
 * Abstract base class for common k-means initializations.
 * 
 * @author Erich Schubert
 * @since 0.3
 * 
 * @param <V> Vector type
 */
public abstract class AbstractKMeansInitialization<V extends NumberVector> implements KMeansInitialization<V> {
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
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Random generator
     */
    protected RandomFactory rnd;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      RandomParameter rndP = new RandomParameter(KMeans.SEED_ID);
      if(config.grab(rndP)) {
        rnd = rndP.getValue();
      }
    }
  }
}