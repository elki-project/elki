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
package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.*;

import java.util.Arrays;
import java.util.Comparator;

import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.CorrelationClusterOrder;
import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.GeneralizedOPTICS;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDoubleDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.index.preprocessed.localpca.FilteredLocalPCAIndex;
import de.lmu.ifi.dbs.elki.index.preprocessed.localpca.KNNQueryFilteredPCAIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.PCAFilteredResult;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.EigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
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
 * Reference:
 * <p>
 * Elke Achtert, Christian Böhm, Peer Kröger, Arthur Zimek<br>
 * Mining Hierarchies of Correlation Clusters.<br>
 * Proc. Int. Conf. on Scientific and Statistical Database Management (SSDBM'06)
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @composed - - - HiCO.Instance
 * @composed - - - KNNQueryFilteredPCAIndex
 * @navhas - produces - CorrelationClusterOrder
 *
 * @param <V> the type of NumberVector handled by the algorithm
 */
@Title("Mining Hierarchies of Correlation Clusters")
@Description("Algorithm for detecting hierarchies of correlation clusters.")
@Reference(authors = "Elke Achtert, Christian Böhm, Peer Kröger, Arthur Zimek", //
    title = "Mining Hierarchies of Correlation Clusters", //
    booktitle = "Proc. Int. Conf. on Scientific and Statistical Database Management (SSDBM'06)", //
    url = "https://doi.org/10.1109/SSDBM.2006.35", //
    bibkey = "DBLP:conf/ssdbm/AchtertBKZ06")
public class HiCO<V extends NumberVector> extends GeneralizedOPTICS<V, CorrelationClusterOrder> {
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
  private KNNQueryFilteredPCAIndex.Factory<V> indexfactory;

  /**
   * Delta parameter
   */
  double deltasq;

  /**
   * Mu parameter.
   */
  int mu;

  /**
   * Constructor.
   *
   * @param indexfactory Index factory
   * @param mu Mu parameter
   */
  public HiCO(KNNQueryFilteredPCAIndex.Factory<V> indexfactory, int mu, double delta) {
    super();
    this.mu = mu;
    this.indexfactory = indexfactory;
    this.deltasq = delta * delta;
  }

  @Override
  public CorrelationClusterOrder run(Database db, Relation<V> relation) {
    if(mu >= relation.size()) {
      throw new AbortException("Parameter mu is chosen unreasonably large. This won't yield meaningful results.");
    }
    return new Instance(db, relation).run();
  }

  /**
   * Instance of the OPTICS algorithm.
   *
   * @author Erich Schubert
   *
   * @assoc - - - FilteredLocalPCAIndex
   */
  private class Instance extends GeneralizedOPTICS.Instance<V, CorrelationClusterOrder> {
    /**
     * Instantiated index.
     */
    private FilteredLocalPCAIndex<V> index;

    /**
     * Data relation.
     */
    private Relation<V> relation;

    /**
     * Cluster order.
     */
    private ArrayModifiableDBIDs clusterOrder;

    /**
     * Correlation value.
     */
    private WritableIntegerDataStore correlationValue;

    /**
     * Temporary storage of correlation values.
     */
    private WritableIntegerDataStore tmpCorrelation;

    /**
     * Temporary storage of distances.
     */
    private WritableDoubleDataStore tmpDistance;

    /**
     * Temporary ids.
     */
    private ArrayModifiableDBIDs tmpIds;

    /**
     * Sort object by the temporary fields.
     */
    Comparator<DBIDRef> tmpcomp = new Comparator<DBIDRef>() {
      @Override
      public int compare(DBIDRef o1, DBIDRef o2) {
        int c = Integer.compare(tmpCorrelation.intValue(o2), tmpCorrelation.intValue(o1));
        return c != 0 ? c : Double.compare(tmpDistance.doubleValue(o1), tmpDistance.doubleValue(o2));
      }
    };

    /**
     * Constructor.
     *
     * @param db Database
     * @param relation Relation
     */
    public Instance(Database db, Relation<V> relation) {
      super(db, relation);
      DBIDs ids = relation.getDBIDs();
      this.clusterOrder = DBIDUtil.newArray(ids.size());
      this.relation = relation;
      this.correlationValue = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_DB, Integer.MAX_VALUE);
      this.tmpIds = DBIDUtil.newArray(ids);
      this.tmpCorrelation = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT);
      this.tmpDistance = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT);
    }

    @Override
    public CorrelationClusterOrder run() {
      this.index = indexfactory.instantiate(relation);
      return super.run();
    }

    @Override
    protected CorrelationClusterOrder buildResult() {
      return new CorrelationClusterOrder("HiCO Cluster Order", "hico-cluster-order", //
          clusterOrder, reachability, predecessor, correlationValue);
    }

    @Override
    protected void initialDBID(DBIDRef id) {
      correlationValue.put(id, Integer.MAX_VALUE);
    }

    @Override
    protected void expandDBID(DBIDRef id) {
      clusterOrder.add(id);

      PCAFilteredResult pca1 = index.getLocalProjection(id);
      V dv1 = relation.get(id);
      final int dim = dv1.getDimensionality();

      DBIDArrayIter iter = tmpIds.iter();
      for(; iter.valid(); iter.advance()) {
        if(processedIDs.contains(iter)) {
          tmpCorrelation.putInt(iter, 0);
          continue;
        }
        PCAFilteredResult pca2 = index.getLocalProjection(iter);
        V dv2 = relation.get(iter);

        tmpCorrelation.putInt(iter, correlationDistance(pca1, pca2, dim));
        tmpDistance.putDouble(iter, EuclideanDistanceFunction.STATIC.distance(dv1, dv2));
      }
      tmpIds.sort(tmpcomp);

      // Core-distance of OPTICS:
      // FIXME: what if there are less than mu points of smallest
      // dimensionality? Then this distance will not be meaningful.
      double coredist = tmpDistance.doubleValue(iter.seek(mu - 1));
      for(iter.seek(0); iter.valid(); iter.advance()) {
        if(processedIDs.contains(iter)) {
          continue;
        }
        int prevcorr = correlationValue.intValue(iter);
        int curcorr = tmpCorrelation.intValue(iter);
        if(prevcorr < curcorr) {
          continue; // No improvement.
        }
        double currdist = MathUtil.max(tmpDistance.doubleValue(iter), coredist);
        if(prevcorr == curcorr) {
          double prevdist = reachability.doubleValue(iter);
          if(prevdist <= currdist) {
            continue; // No improvement.
          }
        }
        correlationValue.putInt(iter, curcorr);
        reachability.putDouble(iter, currdist);
        predecessor.putDBID(iter, id);
        // Add to candidates if not yet seen:
        if(prevcorr == Integer.MAX_VALUE) {
          candidates.add(iter);
        }
      }
    }

    @Override
    public int compare(DBIDRef o1, DBIDRef o2) {
      int c = Integer.compare(correlationValue.intValue(o2), correlationValue.intValue(o1));
      return c != 0 ? c : super.compare(o1, o2);
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }
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
    // TODO: Can we delay copying the matrixes?
    // pca of rv1
    double[][] v1t = copy(pca1.getEigenvectors());
    double[][] v1t_strong = pca1.getStrongEigenvectors();
    int lambda1 = pca1.getCorrelationDimension();

    // pca of rv2
    double[][] v2t = copy(pca2.getEigenvectors());
    double[][] v2t_strong = pca2.getStrongEigenvectors();
    int lambda2 = pca2.getCorrelationDimension();

    // for all strong eigenvectors of rv2
    double[][] m1_czech = pca1.dissimilarityMatrix();
    for(int i = 0; i < v2t_strong.length; i++) {
      double[] v2_i = v2t_strong[i];
      // check, if distance of v2_i to the space of rv1 > delta
      // (i.e., if v2_i spans up a new dimension)
      double distsq = squareSum(v2_i) - transposeTimesTimes(v2_i, m1_czech, v2_i);

      // if so, insert v2_i into v1 and adjust v1
      // and compute m1_czech new, increase lambda1
      if(lambda1 < dimensionality && distsq > deltasq) {
        adjust(v1t, v2_i, lambda1++);
        // TODO: make this incremental?
        double[] e1_czech_d = new double[v1t.length];
        Arrays.fill(e1_czech_d, 0, lambda1, 1);
        m1_czech = transposeDiagonalTimes(v1t, e1_czech_d, v1t);
      }
    }

    // for all strong eigenvectors of rv1
    double[][] m2_czech = pca2.dissimilarityMatrix();
    for(int i = 0; i < v1t_strong.length; i++) {
      double[] v1_i = v1t_strong[i];
      // check, if distance of v1_i to the space of rv2 > delta
      // (i.e., if v1_i spans up a new dimension)
      double distsq = squareSum(v1_i) - transposeTimesTimes(v1_i, m2_czech, v1_i);

      // if so, insert v1_i into v2 and adjust v2
      // and compute m2_czech new , increase lambda2
      if(lambda2 < dimensionality && distsq > deltasq) {
        adjust(v2t, v1_i, lambda2++);
        // TODO: make this incremental?
        double[] e2_czech_d = new double[v1t.length];
        Arrays.fill(e2_czech_d, 0, lambda2, 1);
        m2_czech = transposeDiagonalTimes(v2t, e2_czech_d, v2t);
      }
    }
    return Math.max(lambda1, lambda2);
  }

  /**
   * Inserts the specified vector into the given orthonormal matrix
   * <code>v</code> at column <code>corrDim</code>. After insertion the matrix
   * <code>v</code> is orthonormalized and column <code>corrDim</code> of matrix
   * <code>e_czech</code> is set to the <code>corrDim</code>-th unit vector.
   *
   * @param v the orthonormal matrix of the eigenvectors
   * @param vector the vector to be inserted
   * @param corrDim the column at which the vector should be inserted
   */
  private void adjust(double[][] v, double[] vector, int corrDim) {
    double[] sum = new double[v.length];
    for(int k = 0; k < corrDim; k++) {
      plusTimesEquals(sum, v[k], transposeTimes(vector, v[k]));
    }
    v[corrDim] = normalizeEquals(minus(vector, sum));
  }

  @Override
  public int getMinPts() {
    return mu;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(indexfactory.getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter to specify the smoothing factor, must be an integer greater
     * than 0. The {link {@link #MU_ID}-nearest neighbor is used to compute the
     * correlation reachability of an object.
     */
    public static final OptionID MU_ID = new OptionID("hico.mu", "Specifies the smoothing factor. The mu-nearest neighbor is used to compute the correlation reachability of an object.");

    /**
     * Optional parameter to specify the number of nearest neighbors considered
     * in the PCA, must be an integer greater than 0. If this parameter is not
     * set, k is set to the value of {@link #MU_ID}.
     */
    public static final OptionID K_ID = new OptionID("hico.k", "Optional parameter to specify the number of nearest neighbors considered in the PCA. If this parameter is not set, k is set to the value of parameter mu.");

    /**
     * Parameter to specify the threshold of a distance between a vector q and a
     * given space that indicates that q adds a new dimension to the space, must
     * be a double equal to or greater than 0.
     */
    public static final OptionID DELTA_ID = new OptionID("hico.delta", "Threshold of a distance between a vector q and a given space that indicates that " + "q adds a new dimension to the space.");

    /**
     * The threshold for 'strong' eigenvectors: the 'strong' eigenvectors
     * explain a portion of at least alpha of the total variance.
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
    private KNNQueryFilteredPCAIndex.Factory<V> indexfactory;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter muP = new IntParameter(MU_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(muP)) {
        mu = muP.getValue();
      }

      IntParameter kP = new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT)//
          .setOptional(true);
      final int k = config.grab(kP) ? kP.getValue() : mu;

      DoubleParameter deltaP = new DoubleParameter(DELTA_ID, DEFAULT_DELTA)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(deltaP)) {
        delta = deltaP.doubleValue();
      }

      DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, DEFAULT_ALPHA)//
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE)//
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
      double alpha = DEFAULT_ALPHA;
      if(config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }

      // Configure Distance function
      ListParameterization params = new ListParameterization();
      // preprocessor
      params.addParameter(KNNQueryFilteredPCAIndex.Factory.Parameterizer.K_ID, k);
      params.addParameter(EigenPairFilter.PCA_EIGENPAIR_FILTER, new PercentageEigenPairFilter(alpha));

      ChainedParameterization chain = new ChainedParameterization(params, config);
      chain.errorsTo(config);
      final Class<KNNQueryFilteredPCAIndex.Factory<V>> cls = ClassGenericsUtil.uglyCastIntoSubclass(KNNQueryFilteredPCAIndex.Factory.class);
      indexfactory = chain.tryInstantiate(cls);
    }

    @Override
    protected HiCO<V> makeInstance() {
      return new HiCO<>(indexfactory, mu, delta);
    }
  }
}
