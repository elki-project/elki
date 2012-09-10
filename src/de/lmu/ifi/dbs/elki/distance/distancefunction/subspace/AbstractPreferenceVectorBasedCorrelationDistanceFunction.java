package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractIndexBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.PreferenceVectorBasedCorrelationDistance;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.preprocessed.preference.PreferenceVectorIndex;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Abstract super class for all preference vector based correlation distance
 * functions.
 * 
 * @author Arthur Zimek
 * @param <V> the type of NumberVector to compute the distances in between
 * @param <P> the type of Preprocessor used
 */
public abstract class AbstractPreferenceVectorBasedCorrelationDistanceFunction<V extends NumberVector<?>, P extends PreferenceVectorIndex<V>> extends AbstractIndexBasedDistanceFunction<V, P, PreferenceVectorBasedCorrelationDistance> {
  /**
   * Parameter to specify the maximum distance between two vectors with equal
   * preference vectors before considering them as parallel, must be a double
   * equal to or greater than 0.
   * <p>
   * Default value: {@code 0.001}
   * </p>
   * <p>
   * Key: {@code -pvbasedcorrelationdf.epsilon}
   * </p>
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("distancefunction.epsilon", "The maximum distance between two vectors with equal preference vectors before considering them as parallel.");

  /**
   * Holds the value of {@link #EPSILON_ID}.
   */
  private double epsilon;

  /**
   * Constructor.
   * 
   * @param indexFactory Index factory
   * @param epsilon Epsilon value
   */
  public AbstractPreferenceVectorBasedCorrelationDistanceFunction(IndexFactory<V, P> indexFactory, double epsilon) {
    super(indexFactory);
    this.epsilon = epsilon;
  }

  @Override
  public PreferenceVectorBasedCorrelationDistance getDistanceFactory() {
    return PreferenceVectorBasedCorrelationDistance.FACTORY;
  }

  /**
   * Returns epsilon.
   * 
   * @return epsilon
   */
  public double getEpsilon() {
    return epsilon;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(!this.getClass().equals(obj.getClass())) {
      return false;
    }
    AbstractPreferenceVectorBasedCorrelationDistanceFunction<?, ?> other = (AbstractPreferenceVectorBasedCorrelationDistanceFunction<?, ?>) obj;
    return (this.indexFactory.equals(other.indexFactory)) && this.epsilon == other.epsilon;
  }
  /**
   * Instance to compute the distances on an actual database.
   * 
   * @author Erich Schubert
   */
  abstract public static class Instance<V extends NumberVector<?>, P extends PreferenceVectorIndex<V>> extends AbstractIndexBasedDistanceFunction.Instance<V, P, PreferenceVectorBasedCorrelationDistance, AbstractPreferenceVectorBasedCorrelationDistanceFunction<? super V, ?>> {
    /**
     * The epsilon value
     */
    final double epsilon;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param preprocessor Preprocessor
     * @param epsilon Epsilon
     * @param distanceFunction parent distance function
     */
    public Instance(Relation<V> database, P preprocessor, double epsilon, AbstractPreferenceVectorBasedCorrelationDistanceFunction<? super V, ?> distanceFunction) {
      super(database, preprocessor, distanceFunction);
      this.epsilon = epsilon;
    }

    @Override
    public PreferenceVectorBasedCorrelationDistance distance(DBIDRef id1, DBIDRef id2) {
      BitSet preferenceVector1 = index.getPreferenceVector(id1);
      BitSet preferenceVector2 = index.getPreferenceVector(id2);
      V v1 = relation.get(id1);
      V v2 = relation.get(id2);
      return correlationDistance(v1, v2, preferenceVector1, preferenceVector2);
    }

    /**
     * Computes the correlation distance between the two specified vectors
     * according to the specified preference vectors.
     * 
     * @param v1 first vector
     * @param v2 second vector
     * @param pv1 the first preference vector
     * @param pv2 the second preference vector
     * @return the correlation distance between the two specified vectors
     */
    public abstract PreferenceVectorBasedCorrelationDistance correlationDistance(V v1, V v2, BitSet pv1, BitSet pv2);

    /**
     * Computes the weighted distance between the two specified vectors
     * according to the given preference vector.
     * 
     * @param v1 the first vector
     * @param v2 the second vector
     * @param weightVector the preference vector
     * @return the weighted distance between the two specified vectors according
     *         to the given preference vector
     */
    public double weightedDistance(V v1, V v2, BitSet weightVector) {
      if(v1.getDimensionality() != v2.getDimensionality()) {
        throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
      }

      double sqrDist = 0;
      for(int i = 1; i <= v1.getDimensionality(); i++) {
        if(weightVector.get(i - 1)) {
          double manhattanI = v1.doubleValue(i) - v2.doubleValue(i);
          sqrDist += manhattanI * manhattanI;
        }
      }
      return Math.sqrt(sqrDist);
    }

    /**
     * Computes the weighted distance between the two specified vectors
     * according to the given preference vector.
     * 
     * @param id1 the id of the first vector
     * @param id2 the id of the second vector
     * @param weightVector the preference vector
     * @return the weighted distance between the two specified vectors according
     *         to the given preference vector
     */
    public double weightedDistance(DBID id1, DBID id2, BitSet weightVector) {
      return weightedDistance(relation.get(id1), relation.get(id2), weightVector);
    }

    /**
     * Computes the weighted distance between the two specified data vectors
     * according to their preference vectors.
     * 
     * @param id1 the id of the first vector
     * @param id2 the id of the second vector
     * @return the weighted distance between the two specified vectors according
     *         to the preference vector of the first data vector
     */
    public double weightedPrefereneceVectorDistance(DBID id1, DBID id2) {
      final V v1 = relation.get(id1);
      final V v2 = relation.get(id2);
      double d1 = weightedDistance(v1, v2, index.getPreferenceVector(id1));
      double d2 = weightedDistance(v2, v1, index.getPreferenceVector(id2));
      return Math.max(d1, d2);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public abstract static class Parameterizer<F extends IndexFactory<?, ?>> extends AbstractIndexBasedDistanceFunction.Parameterizer<F> {
    protected double epsilon = 0.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configEpsilon(config);
    }

    protected void configEpsilon(Parameterization config) {
      final DoubleParameter epsilonP = new DoubleParameter(EPSILON_ID, new GreaterEqualConstraint(0), 0.001);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.getValue();
      }
    }
  }
}