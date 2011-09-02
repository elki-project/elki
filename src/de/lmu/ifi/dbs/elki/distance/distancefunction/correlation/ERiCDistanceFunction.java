package de.lmu.ifi.dbs.elki.distance.distancefunction.correlation;

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
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractIndexBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.FilteredLocalPCABasedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.WeightedDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.BitDistance;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.preprocessed.localpca.FilteredLocalPCAIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.localpca.KNNQueryFilteredPCAIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides a distance function for building the hierarchy in the ERiC
 * algorithm.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has Instance
 */
public class ERiCDistanceFunction extends AbstractIndexBasedDistanceFunction<NumberVector<?, ?>, FilteredLocalPCAIndex<NumberVector<?, ?>>, BitDistance> implements FilteredLocalPCABasedDistanceFunction<NumberVector<?, ?>, FilteredLocalPCAIndex<NumberVector<?, ?>>, BitDistance> {
  /**
   * Logger for debug.
   */
  static Logging logger = Logging.getLogger(PCABasedCorrelationDistanceFunction.class);

  /**
   * Parameter to specify the threshold for approximate linear dependency: the
   * strong eigenvectors of q are approximately linear dependent from the strong
   * eigenvectors p if the following condition holds for all strong eigenvectors
   * q_i of q (lambda_q < lambda_p): q_i' * M^check_p * q_i <= delta^2, must be
   * a double equal to or greater than 0.
   * <p>
   * Default value: {@code 0.1}
   * </p>
   * <p>
   * Key: {@code -ericdf.delta}
   * </p>
   */
  public static final OptionID DELTA_ID = OptionID.getOrCreateOptionID("ericdf.delta", "Threshold for approximate linear dependency: " + "the strong eigenvectors of q are approximately linear dependent " + "from the strong eigenvectors p if the following condition " + "holds for all stroneg eigenvectors q_i of q (lambda_q < lambda_p): " + "q_i' * M^check_p * q_i <= delta^2.");

  /**
   * Parameter to specify the threshold for the maximum distance between two
   * approximately linear dependent subspaces of two objects p and q (lambda_q <
   * lambda_p) before considering them as parallel, must be a double equal to or
   * greater than 0.
   * <p>
   * Default value: {@code 0.1}
   * </p>
   * <p>
   * Key: {@code -ericdf.tau}
   * </p>
   */
  public static final OptionID TAU_ID = OptionID.getOrCreateOptionID("ericdf.tau", "Threshold for the maximum distance between two approximately linear " + "dependent subspaces of two objects p and q " + "(lambda_q < lambda_p) before considering them as parallel.");

  /**
   * Holds the value of {@link #DELTA_ID}.
   */
  private double delta;

  /**
   * Holds the value of {@link #TAU_ID}.
   */
  private double tau;

  /**
   * Constructor.
   * 
   * @param indexFactory Index factory.
   * @param delta Delta parameter
   * @param tau Tau parameter
   */
  public ERiCDistanceFunction(IndexFactory<NumberVector<?, ?>, FilteredLocalPCAIndex<NumberVector<?, ?>>> indexFactory, double delta, double tau) {
    super(indexFactory);
    this.delta = delta;
    this.tau = tau;
  }

  @Override
  public BitDistance getDistanceFactory() {
    return BitDistance.FACTORY;
  }

  @Override
  public <T extends NumberVector<?, ?>> Instance<T> instantiate(Relation<T> database) {
    // We can't really avoid these warnings, due to a limitation in Java
    // Generics (AFAICT)
    @SuppressWarnings("unchecked")
    FilteredLocalPCAIndex<T> indexinst = (FilteredLocalPCAIndex<T>) indexFactory.instantiate((Relation<NumberVector<?, ?>>) database);
    return new Instance<T>(database, indexinst, this, delta, tau);
  }

  /**
   * Returns true, if the strong eigenvectors of the two specified pcas span up
   * the same space. Note, that the first pca must have equal ore more strong
   * eigenvectors than the second pca.
   * 
   * @param pca1 first PCA
   * @param pca2 second PCA
   * @return true, if the strong eigenvectors of the two specified pcas span up
   *         the same space
   */
  private boolean approximatelyLinearDependent(PCAFilteredResult pca1, PCAFilteredResult pca2) {
    Matrix m1_czech = pca1.dissimilarityMatrix();
    Matrix v2_strong = pca2.adapatedStrongEigenvectors();
    for(int i = 0; i < v2_strong.getColumnDimensionality(); i++) {
      Matrix v2_i = v2_strong.getColumn(i);
      // check, if distance of v2_i to the space of pca_1 > delta
      // (i.e., if v2_i spans up a new dimension)
      double dist = Math.sqrt(v2_i.transposeTimes(v2_i).get(0, 0) - v2_i.transposeTimes(m1_czech).times(v2_i).get(0, 0));

      // if so, return false
      if(dist > delta) {
        return false;
      }
    }

    return true;
  }

  /**
   * Computes the distance between two given DatabaseObjects according to this
   * distance function. Note, that the first pca must have equal or more strong
   * eigenvectors than the second pca.
   * 
   * @param v1 first DatabaseObject
   * @param v2 second DatabaseObject
   * @param pca1 first PCA
   * @param pca2 second PCA
   * @return the distance between two given DatabaseObjects according to this
   *         distance function
   */
  public BitDistance distance(NumberVector<?, ?> v1, NumberVector<?, ?> v2, PCAFilteredResult pca1, PCAFilteredResult pca2) {
    if(pca1.getCorrelationDimension() < pca2.getCorrelationDimension()) {
      throw new IllegalStateException("pca1.getCorrelationDimension() < pca2.getCorrelationDimension(): " + pca1.getCorrelationDimension() + " < " + pca2.getCorrelationDimension());
    }

    boolean approximatelyLinearDependent;
    if(pca1.getCorrelationDimension() == pca2.getCorrelationDimension()) {
      approximatelyLinearDependent = approximatelyLinearDependent(pca1, pca2) && approximatelyLinearDependent(pca2, pca1);
    }
    else {
      approximatelyLinearDependent = approximatelyLinearDependent(pca1, pca2);
    }

    if(!approximatelyLinearDependent) {
      return new BitDistance(true);
    }

    else {
      double affineDistance;

      if(pca1.getCorrelationDimension() == pca2.getCorrelationDimension()) {
        WeightedDistanceFunction df1 = new WeightedDistanceFunction(pca1.similarityMatrix());
        WeightedDistanceFunction df2 = new WeightedDistanceFunction(pca2.similarityMatrix());
        affineDistance = Math.max(df1.distance(v1, v2).doubleValue(), df2.distance(v1, v2).doubleValue());
      }
      else {
        WeightedDistanceFunction df1 = new WeightedDistanceFunction(pca1.similarityMatrix());
        affineDistance = df1.distance(v1, v2).doubleValue();
      }

      if(affineDistance > tau) {
        return new BitDistance(true);
      }

      return new BitDistance(false);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if (!this.getClass().equals(obj.getClass())) {
      return false;
    }
    ERiCDistanceFunction other = (ERiCDistanceFunction) obj;
    return (this.delta == other.delta) && (this.tau == other.tau);
  }

  /**
   * The actual instance bound to a particular database.
   * 
   * @author Erich Schubert
   */
  public static class Instance<V extends NumberVector<?, ?>> extends AbstractIndexBasedDistanceFunction.Instance<V, FilteredLocalPCAIndex<V>, BitDistance, ERiCDistanceFunction> implements FilteredLocalPCABasedDistanceFunction.Instance<V, FilteredLocalPCAIndex<V>, BitDistance> {
    /**
     * Holds the value of {@link #DELTA_ID}.
     */
    private final double delta;

    /**
     * Holds the value of {@link #TAU_ID}.
     */
    private final double tau;

    /**
     * Constructor.
     * 
     * @param database Database
     * @param index Index
     * @param parent Parent distance
     * @param delta Delta parameter
     * @param tau Tau parameter
     */
    public Instance(Relation<V> database, FilteredLocalPCAIndex<V> index, ERiCDistanceFunction parent, double delta, double tau) {
      super(database, index, parent);
      this.delta = delta;
      this.tau = tau;
    }

    /**
     * Note, that the pca of o1 must have equal ore more strong eigenvectors
     * than the pca of o2.
     */
    @Override
    public BitDistance distance(DBID id1, DBID id2) {
      PCAFilteredResult pca1 = index.getLocalProjection(id1);
      PCAFilteredResult pca2 = index.getLocalProjection(id2);
      V v1 = relation.get(id1);
      V v2 = relation.get(id2);
      return parent.distance(v1, v2, pca1, pca2);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractIndexBasedDistanceFunction.Parameterizer<IndexFactory<NumberVector<?, ?>, FilteredLocalPCAIndex<NumberVector<?, ?>>>> {
    double delta = 0.0;

    double tau = 0.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configIndexFactory(config, FilteredLocalPCAIndex.Factory.class, KNNQueryFilteredPCAIndex.Factory.class);

      final DoubleParameter deltaP = new DoubleParameter(DELTA_ID, new GreaterEqualConstraint(0), 0.1);
      if(config.grab(deltaP)) {
        delta = deltaP.getValue();
      }

      final DoubleParameter tauP = new DoubleParameter(TAU_ID, new GreaterEqualConstraint(0), 0.1);
      if(config.grab(tauP)) {
        tau = tauP.getValue();
      }
    }

    @Override
    protected ERiCDistanceFunction makeInstance() {
      return new ERiCDistanceFunction(factory, delta, tau);
    }
  }
}