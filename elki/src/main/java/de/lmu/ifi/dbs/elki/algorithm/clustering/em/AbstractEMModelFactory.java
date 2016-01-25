package de.lmu.ifi.dbs.elki.algorithm.clustering.em;

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

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.KMeansInitialization;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.RandomlyGeneratedInitialMeans;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract base class for initializing EM.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @param <V> Vector type
 * @param <M> Model type
 */
public abstract class AbstractEMModelFactory<V extends NumberVector, M extends MeanModel> implements EMClusterModelFactory<V, M> {
  /**
   * Class to choose the initial means
   */
  protected KMeansInitialization<V> initializer;

  /**
   * Constructor.
   * 
   * @param initializer Class for choosing the initial seeds.
   */
  public AbstractEMModelFactory(KMeansInitialization<V> initializer) {
    super();
    this.initializer = initializer;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   * 
   * @param <V> vector type
   */
  public abstract static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter to specify the cluster center initialization.
     */
    public static final OptionID INIT_ID = new OptionID("em.centers", //
    "Method to choose the initial cluster centers.");

    /**
     * Initialization method
     */
    protected KMeansInitialization<V> initializer;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<KMeansInitialization<V>> initialP = new ObjectParameter<>(INIT_ID, KMeansInitialization.class, RandomlyGeneratedInitialMeans.class);
      if(config.grab(initialP)) {
        initializer = initialP.instantiateClass(config);
      }
    }
  }
}