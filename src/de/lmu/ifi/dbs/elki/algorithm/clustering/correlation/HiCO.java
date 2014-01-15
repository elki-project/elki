package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.GeneralizedOPTICS;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.PCACorrelationDistance;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.preprocessed.localpca.FilteredLocalPCAIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.localpca.KNNQueryFilteredPCAIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.result.optics.GenericClusterOrderEntry;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ChainedParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Implementation of the HiCO algorithm, an algorithm for detecting hierarchies
 * of correlation clusters.
 * <p>
 * Reference: E. Achtert, C. Böhm, P. Kröger, A. Zimek: Mining Hierarchies of
 * Correlation Clusters. <br>
 * In: Proc. Int. Conf. on Scientific and Statistical Database Management
 * (SSDBM'06), Vienna, Austria, 2006.
 * </p>
 * 
 * @author Elke Achtert
 * 
 * @apiviz.uses KNNQueryFilteredPCAIndex
 * @apiviz.uses PCABasedCorrelationDistanceFunction
 * 
 * @param <V> the type of NumberVector handled by the algorithm
 */
@Title("Mining Hierarchies of Correlation Clusters")
@Description("Algorithm for detecting hierarchies of correlation clusters.")
@Reference(authors = "E. Achtert, C. Böhm, P. Kröger, A. Zimek", title = "Mining Hierarchies of Correlation Clusters", booktitle = "Proc. Int. Conf. on Scientific and Statistical Database Management (SSDBM'06), Vienna, Austria, 2006", url = "http://dx.doi.org/10.1109/SSDBM.2006.35")
public class HiCO<V extends NumberVector<?>> extends GeneralizedOPTICS<V, PCACorrelationDistance> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(HiCO.class);

  /**
   * The default value for {@link Parameterizer#DELTA_ID}.
   */
  public static final double DEFAULT_DELTA = 0.25;

  /**
   * The default value for {@link Parameterizer#ALPHA_ID}.
   */
  public static final double DEFAULT_ALPHA = 0.85;

  /**
   * Factory to produce
   */
  private IndexFactory<V, FilteredLocalPCAIndex<NumberVector<?>>> indexfactory;

  /**
   * Instantiated index.
   */
  private FilteredLocalPCAIndex<NumberVector<?>> index;

  /**
   * Delta parameter
   */
  double delta;

  /**
   * Constructor.
   * 
   * @param indexfactory Index factory
   * @param mu Mu parameter
   */
  public HiCO(IndexFactory<V, FilteredLocalPCAIndex<NumberVector<?>>> indexfactory, int mu, double delta) {
    super(mu);
    this.indexfactory = indexfactory;
    this.delta = delta;
  }

  @Override
  public ClusterOrderResult<PCACorrelationDistance> run(Relation<V> relation) {
    assert (this.index == null) : "Running algorithm instance multiple times in parallel is not supported.";
    this.index = indexfactory.instantiate(relation);
    ClusterOrderResult<PCACorrelationDistance> result = super.run(relation);
    this.index = null;
    return result;
  }

  @Override
  protected ClusterOrderEntry<PCACorrelationDistance> makeSeedEntry(Relation<V> relation, DBID objectID) {
    return new GenericClusterOrderEntry<>(objectID, null, PCACorrelationDistance.FACTORY.infiniteDistance());
  }

  @Override
  protected Collection<ClusterOrderEntry<PCACorrelationDistance>> getNeighborsForDBID(Relation<V> relation, DBID id) {
    DBID id1 = DBIDUtil.deref(id);
    PCAFilteredResult pca1 = index.getLocalProjection(id1);
    V dv1 = relation.get(id1);
    final int dim = dv1.getDimensionality();

    ArrayList<ClusterOrderEntry<PCACorrelationDistance>> result = new ArrayList<>();
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      PCAFilteredResult pca2 = index.getLocalProjection(iter);
      V dv2 = relation.get(iter);

      int correlationDistance = correlationDistance(pca1, pca2, dim);
      double euclideanDistance = EuclideanDistanceFunction.STATIC.doubleDistance(dv1, dv2);

      PCACorrelationDistance reachability = new PCACorrelationDistance(correlationDistance, euclideanDistance);
      result.add(new GenericClusterOrderEntry<>(DBIDUtil.deref(iter), id1, reachability));
    }
    Collections.sort(result);
    // This is a hack, but needed to enforce core-distance of OPTICS:
    if(result.size() >= getMinPts()) {
      PCACorrelationDistance coredist = result.get(getMinPts() - 1).getReachability();
      for(int i = 0; i < getMinPts() - 1; i++) {
        result.set(i, new GenericClusterOrderEntry<>(result.get(i).getID(), id1, coredist));
      }
    }
    return result;
  }

  /**
   * Computes the correlation distance between the two subspaces defined by the
   * specified PCAs.
   * 
   * @param pca1 first PCA
   * @param pca2 second PCA
   * @param dimensionality the dimensionality of the data space
   * @return the correlation distance between the two subspaces defined by the
   *         specified PCAs
   */
  public int correlationDistance(PCAFilteredResult pca1, PCAFilteredResult pca2, int dimensionality) {
    // TODO nur in eine Richtung?
    // pca of rv1
    Matrix v1 = pca1.getEigenvectors().copy();
    Matrix v1_strong = pca1.adapatedStrongEigenvectors().copy();
    Matrix e1_czech = pca1.selectionMatrixOfStrongEigenvectors().copy();
    int lambda1 = pca1.getCorrelationDimension();

    // pca of rv2
    Matrix v2 = pca2.getEigenvectors().copy();
    Matrix v2_strong = pca2.adapatedStrongEigenvectors().copy();
    Matrix e2_czech = pca2.selectionMatrixOfStrongEigenvectors().copy();
    int lambda2 = pca2.getCorrelationDimension();

    // for all strong eigenvectors of rv2
    Matrix m1_czech = pca1.dissimilarityMatrix();
    for(int i = 0; i < v2_strong.getColumnDimensionality(); i++) {
      Vector v2_i = v2_strong.getCol(i);
      // check, if distance of v2_i to the space of rv1 > delta
      // (i.e., if v2_i spans up a new dimension)
      double dist = Math.sqrt(v2_i.transposeTimes(v2_i) - v2_i.transposeTimesTimes(m1_czech, v2_i));

      // if so, insert v2_i into v1 and adjust v1
      // and compute m1_czech new, increase lambda1
      if(lambda1 < dimensionality && dist > delta) {
        adjust(v1, e1_czech, v2_i, lambda1++);
        m1_czech = v1.times(e1_czech).timesTranspose(v1);
      }
    }

    // for all strong eigenvectors of rv1
    Matrix m2_czech = pca2.dissimilarityMatrix();
    for(int i = 0; i < v1_strong.getColumnDimensionality(); i++) {
      Vector v1_i = v1_strong.getCol(i);
      // check, if distance of v1_i to the space of rv2 > delta
      // (i.e., if v1_i spans up a new dimension)
      double dist = Math.sqrt(v1_i.transposeTimes(v1_i) - v1_i.transposeTimes(m2_czech).times(v1_i).get(0));

      // if so, insert v1_i into v2 and adjust v2
      // and compute m2_czech new , increase lambda2
      if(lambda2 < dimensionality && dist > delta) {
        adjust(v2, e2_czech, v1_i, lambda2++);
        m2_czech = v2.times(e2_czech).timesTranspose(v2);
      }
    }

    int correlationDistance = Math.max(lambda1, lambda2);

    // TODO delta einbauen
    // Matrix m_1_czech = pca1.dissimilarityMatrix();
    // double dist_1 = normalizedDistance(dv1, dv2, m1_czech);
    // Matrix m_2_czech = pca2.dissimilarityMatrix();
    // double dist_2 = normalizedDistance(dv1, dv2, m2_czech);
    // if (dist_1 > delta || dist_2 > delta) {
    // correlationDistance++;
    // }

    return correlationDistance;
  }

  /**
   * Inserts the specified vector into the given orthonormal matrix
   * <code>v</code> at column <code>corrDim</code>. After insertion the matrix
   * <code>v</code> is orthonormalized and column <code>corrDim</code> of matrix
   * <code>e_czech</code> is set to the <code>corrDim</code>-th unit vector.
   * 
   * @param v the orthonormal matrix of the eigenvectors
   * @param e_czech the selection matrix of the strong eigenvectors
   * @param vector the vector to be inserted
   * @param corrDim the column at which the vector should be inserted
   */
  private void adjust(Matrix v, Matrix e_czech, Vector vector, int corrDim) {
    int dim = v.getRowDimensionality();

    // set e_czech[corrDim][corrDim] := 1
    e_czech.set(corrDim, corrDim, 1);

    // normalize v
    Vector v_i = vector.copy();
    Vector sum = new Vector(dim);
    for(int k = 0; k < corrDim; k++) {
      Vector v_k = v.getCol(k);
      sum.plusTimesEquals(v_k, v_i.transposeTimes(v_k));
    }
    v_i.minusEquals(sum);
    v_i.normalize();
    v.setCol(corrDim, v_i);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(indexfactory.getInputTypeRestriction());
  }

  @Override
  public PCACorrelationDistance getDistanceFactory() {
    return PCACorrelationDistance.FACTORY;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
    /**
     * Parameter to specify the smoothing factor, must be an integer greater
     * than 0. The {link {@link #MU_ID}-nearest neighbor is used to compute the
     * correlation reachability of an object.
     * 
     * <p>
     * Key: {@code -hico.mu}
     * </p>
     */
    public static final OptionID MU_ID = new OptionID("hico.mu", "Specifies the smoothing factor. The mu-nearest neighbor is used to compute the correlation reachability of an object.");

    /**
     * Optional parameter to specify the number of nearest neighbors considered
     * in the PCA, must be an integer greater than 0. If this parameter is not
     * set, k is set to the value of {@link #MU_ID}.
     * <p>
     * Key: {@code -hico.k}
     * </p>
     * <p>
     * Default value: {@link #MU_ID}
     * </p>
     */
    public static final OptionID K_ID = new OptionID("hico.k", "Optional parameter to specify the number of nearest neighbors considered in the PCA. If this parameter is not set, k is set to the value of parameter mu.");

    /**
     * Parameter to specify the threshold of a distance between a vector q and a
     * given space that indicates that q adds a new dimension to the space, must
     * be a double equal to or greater than 0.
     * <p>
     * Default value: {@code 0.25}
     * </p>
     * <p>
     * Key: {@code -hico.delta}
     * </p>
     */
    public static final OptionID DELTA_ID = new OptionID("hico.delta", "Threshold of a distance between a vector q and a given space that indicates that " + "q adds a new dimension to the space.");

    /**
     * The threshold for 'strong' eigenvectors: the 'strong' eigenvectors
     * explain a portion of at least alpha of the total variance.
     * <p>
     * Default value: {@link #DEFAULT_ALPHA}
     * </p>
     * <p>
     * Key: {@code -hico.alpha}
     * </p>
     */
    public static final OptionID ALPHA_ID = new OptionID("hico.alpha", "The threshold for 'strong' eigenvectors: the 'strong' eigenvectors explain a portion of at least alpha of the total variance.");

    /**
     * Mu parameter
     */
    int mu = -1;

    /**
     * Delta parameter
     */
    double delta;

    /**
     * Factory to produce
     */
    private IndexFactory<V, FilteredLocalPCAIndex<NumberVector<?>>> indexfactory;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter muP = new IntParameter(MU_ID);
      muP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(muP)) {
        mu = muP.getValue();
      }

      IntParameter kP = new IntParameter(K_ID);
      kP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      kP.setOptional(true);
      final int k = config.grab(kP) ? kP.getValue() : mu;

      DoubleParameter deltaP = new DoubleParameter(DELTA_ID, DEFAULT_DELTA);
      deltaP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      delta = DEFAULT_DELTA;
      if(config.grab(deltaP)) {
        delta = deltaP.doubleValue();
      }

      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, DEFAULT_ALPHA);
      alphaP.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      alphaP.addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
      double alpha = DEFAULT_ALPHA;
      if(config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }

      // Configure Distance function
      ListParameterization params = new ListParameterization();
      // preprocessor
      params.addParameter(KNNQueryFilteredPCAIndex.Factory.K_ID, k);
      params.addParameter(PercentageEigenPairFilter.ALPHA_ID, alpha);

      ChainedParameterization chain = new ChainedParameterization(params, config);
      chain.errorsTo(config);
      final Class<? extends IndexFactory<V, FilteredLocalPCAIndex<NumberVector<?>>>> cls = ClassGenericsUtil.uglyCrossCast(KNNQueryFilteredPCAIndex.Factory.class, IndexFactory.class);
      indexfactory = chain.tryInstantiate(cls);
    }

    @Override
    protected HiCO<V> makeInstance() {
      return new HiCO<>(indexfactory, mu, delta);
    }
  }
}
