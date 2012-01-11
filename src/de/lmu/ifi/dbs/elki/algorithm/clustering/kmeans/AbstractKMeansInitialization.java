package de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;

/**
 * Abstract base class for common k-means initializations.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public abstract class AbstractKMeansInitialization<V extends NumberVector<V, ?>> implements KMeansInitialization<V> {
  /**
   * Holds the value of {@link #SEED_ID}.
   */
  protected Long seed;

  /**
   * Constructor.
   * 
   * @param seed Random seed.
   */
  public AbstractKMeansInitialization(Long seed) {
    this.seed = seed;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public abstract static class Parameterizer<V extends NumberVector<V, ?>> extends AbstractParameterizer {
    protected Long seed;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      LongParameter seedP = new LongParameter(KMeans.SEED_ID, true);
      if(config.grab(seedP)) {
        seed = seedP.getValue();
      }
    }
  }
}