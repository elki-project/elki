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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.NumberVector.Factory;
import de.lmu.ifi.dbs.elki.data.model.MeanModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleArrayListParameter;

/**
 * Run k-means with prespecified initial means.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class PredefinedInitialMeans extends AbstractKMeansInitialization<NumberVector> {
  /**
   * Initial means to return.
   */
  List<? extends NumberVector> initialMeans;

  /**
   * Constructor.
   *
   * @param initialMeans Initial means
   */
  public PredefinedInitialMeans(List<? extends NumberVector> initialMeans) {
    super(null);
    this.setInitialMeans(initialMeans);
  }

  /**
   * Constructor.
   *
   * @param initialMeans Initial means
   */
  public PredefinedInitialMeans(double[][] initialMeans) {
    super(null);
    this.setInitialMeans(initialMeans);
  }

  /**
   * Set the initial means.
   *
   * Important notice: Use with care - the means are <em>not copied</em>!
   *
   * @param initialMeans initial means.
   */
  public void setInitialMeans(List<? extends NumberVector> initialMeans) {
    this.initialMeans = initialMeans;
  }

  /**
   * Set the initial means.
   *
   * Important notice: Use with care - the means are <em>not copied</em>!
   *
   * @param initialMeans initial means.
   */
  public void setInitialClusters(List<? extends Cluster<? extends MeanModel>> initialMeans) {
    List<Vector> vecs = new ArrayList<>(initialMeans.size());
    for(Cluster<? extends MeanModel> cluster : initialMeans) {
      vecs.add(cluster.getModel().getMean().copy());
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
    List<Vector> vecs = new ArrayList<>(initialMeans.length);
    for(int i = 0; i < initialMeans.length; ++i) {
      vecs.add(new Vector(initialMeans[i]));
    }
    this.initialMeans = vecs;
  }

  @Override
  public <T extends NumberVector, O extends NumberVector> List<O> chooseInitialMeans(Database database, Relation<T> relation, int k, NumberVectorDistanceFunction<? super T> distanceFunction, Factory<O> factory) {
    if(k != initialMeans.size()) {
      throw new AbortException("Predefined initial means contained " + initialMeans.size() + " means, algorithm requested " + k + " means instead.");
    }
    // Chose first mean
    List<O> means = new ArrayList<>(k);

    for(NumberVector v : initialMeans) {
      means.add(factory.newNumberVector(v));
    }
    return means;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Option to specify the initial means to use.
     */
    public static final OptionID INITIAL_MEANS = new OptionID("kmeans.means", "Initial means for k-means.");

    /**
     * Initial means.
     */
    protected List<Vector> initialMeans;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleArrayListParameter meansP = new DoubleArrayListParameter(INITIAL_MEANS);
      if(config.grab(meansP)) {
        initialMeans = new ArrayList<>(meansP.getValue().size());
        for(double[] v : meansP.getValue()) {
          initialMeans.add(new Vector(v));
        }
      }
    }

    @Override
    protected PredefinedInitialMeans makeInstance() {
      return new PredefinedInitialMeans(initialMeans);
    }
  }
}
