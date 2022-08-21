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
package elki.clustering.subspace;

import elki.clustering.optics.ClusterOrder;
import elki.clustering.optics.CorrelationClusterOrder;
import elki.clustering.optics.GeneralizedOPTICS;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDataStore;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.knn.KNNSearcher;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.Duration;
import elki.logging.statistics.MillisTimeDuration;
import elki.result.Metadata;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Implementation of the HiSC algorithm, an algorithm for detecting hierarchies
 * of subspace clusters.
 * <p>
 * Reference:
 * <p>
 * Elke Achtert, Christian Böhm, Hans-Peter Kriegel, Peer Kröger,
 * Ina Müller-Gorman, Arthur Zimek<br>
 * Finding Hierarchies of Subspace Clusters<br>
 * Proc. 10th Europ. Conf. on Principles and Practice of Knowledge Discovery in
 * Databases (PKDD'06)
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @composed - - - HiSCPreferenceVectorIndex
 * @navhas - produces - CorrelationClusterOrder
 */
@Title("Finding Hierarchies of Subspace Clusters")
@Description("Algorithm for detecting hierarchies of subspace clusters.")
@Reference(authors = "Elke Achtert, Christian Böhm, Hans-Petre Kriegel, Peer Kröger, Ina Müller-Gorman, Arthur Zimek", //
    title = "Finding Hierarchies of Subspace Clusters", //
    booktitle = "Proc. 10th Europ. Conf. on Principles and Practice of Knowledge Discovery in Databases (PKDD'06)", //
    url = "https://doi.org/10.1007/11871637_42", //
    bibkey = "DBLP:conf/pkdd/AchtertBKKMZ06")
public class HiSC implements GeneralizedOPTICS {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(HiSC.class);

  /**
   * Holds the maximum diversion allowed.
   */
  private double alpha;

  /**
   * The number of nearest neighbors considered to determine the preference
   * vector.
   */
  protected int k;

  /**
   * Constructor.
   * 
   * @param alpha Alpha
   * @param k k
   */
  public HiSC(double alpha, int k) {
    super();
    this.alpha = alpha;
    this.k = k;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Run the HiSC algorithm
   *
   * @param relation Data relation
   * @return OPTICS cluster order
   */
  public ClusterOrder run(Relation<? extends NumberVector> relation) {
    return new Instance(relation).run();
  }

  /**
   * Algorithm instance for a single data set.
   *
   * @author Erich Schubert
   */
  private class Instance extends GeneralizedOPTICS.Instance<CorrelationClusterOrder> {
    /**
     * The data store.
     */
    protected WritableDataStore<long[]> preferenceVectors = null;

    /**
     * Cluster order.
     */
    private ArrayModifiableDBIDs clusterOrder;

    /**
     * Data relation.
     */
    private Relation<? extends NumberVector> relation;

    /**
     * Correlation dimensionality.
     */
    private WritableIntegerDataStore correlationValue;

    /**
     * Shared preference vectors.
     */
    private WritableDataStore<long[]> commonPreferenceVectors;

    /**
     * Constructor.
     *
     * @param relation Relation
     */
    public Instance(Relation<? extends NumberVector> relation) {
      super(relation.getDBIDs());
      this.clusterOrder = DBIDUtil.newArray(relation.size());
      this.relation = relation;
      this.correlationValue = DataStoreUtil.makeIntegerStorage(relation.getDBIDs(), DataStoreFactory.HINT_DB, Integer.MAX_VALUE);
      this.commonPreferenceVectors = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_TEMP, long[].class);
    }

    @Override
    public CorrelationClusterOrder run() {
      final int usek = k > 0 ? k : 3 * RelationUtil.dimensionality(relation);
      preferenceVectors = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, long[].class);
      KNNSearcher<DBIDRef> knnQuery = new QueryBuilder<>(relation, EuclideanDistance.STATIC).kNNByDBID(usek);

      Duration dur = new MillisTimeDuration(this.getClass() + ".preprocessing-time").begin();
      FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("Preprocessing preference vector", relation.size(), LOG) : null;
      for(DBIDIter it = relation.iterDBIDs(); it.valid(); it.advance()) {
        preferenceVectors.put(it, determinePreferenceVector(it, knnQuery.getKNN(it, usek)));
        LOG.incrementProcessed(progress);
      }
      LOG.ensureCompleted(progress);
      LOG.statistics(dur.end());
      return super.run();
    }

    /**
     * Determines the preference vector according to the specified neighbor ids.
     *
     * @param id the id of the object for which the preference vector should be
     *        determined
     * @param neighborIDs the ids of the neighbors
     * @return the preference vector
     */
    private long[] determinePreferenceVector(DBIDRef id, DBIDs neighborIDs) {
      NumberVector p = relation.get(id);
      // variances
      final int size = neighborIDs.size(), dim = p.getDimensionality();
      double[] sumsq = new double[dim];
      for(DBIDIter iter = neighborIDs.iter(); iter.valid(); iter.advance()) {
        NumberVector o = relation.get(iter);
        for(int d = 0; d < dim; d++) {
          final double diff = o.doubleValue(d) - p.doubleValue(d);
          sumsq[d] += diff * diff;
        }
      }

      // preference vector
      long[] preferenceVector = BitsUtil.zero(dim);
      for(int d = 0; d < dim; d++) {
        if(sumsq[d] < alpha * size) {
          BitsUtil.setI(preferenceVector, d);
        }
      }
      return preferenceVector;
    }

    @Override
    protected CorrelationClusterOrder buildResult() {
      CorrelationClusterOrder result = new CorrelationClusterOrder(clusterOrder, reachability, predecessor, correlationValue);
      Metadata.of(result).setLongName("HiCO Cluster Order");
      return result;
    }

    @Override
    protected void initialDBID(DBIDRef id) {
      correlationValue.put(id, Integer.MAX_VALUE);
      commonPreferenceVectors.put(id, new long[0]);
    }

    @Override
    protected void expandDBID(DBIDRef id) {
      clusterOrder.add(id);

      final long[] pv1 = preferenceVectors.get(id);
      final NumberVector v1 = relation.get(id);
      final int dim = v1.getDimensionality();

      for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
        // We can aggressively ignore processed objects, because we do not
        // have a minPts parameter in HiSC:
        if(processedIDs.contains(iter)) {
          continue;
        }
        long[] pv2 = preferenceVectors.get(iter);
        NumberVector v2 = relation.get(iter);
        final long[] commonPreferenceVector = BitsUtil.andCMin(pv1, pv2);

        // number of zero values in commonPreferenceVector
        int subspaceDim = dim - BitsUtil.cardinality(commonPreferenceVector);

        // special case: v1 and v2 are in parallel subspaces
        double dist1 = weightedDistance(v1, v2, pv1);
        double dist2 = weightedDistance(v1, v2, pv2);

        if(dist1 > alpha || dist2 > alpha) {
          subspaceDim++;
          if(LOG.isDebugging()) {
            LOG.debugFine(new StringBuilder(100) //
                .append("dist1 ").append(dist1) //
                .append("\ndist2 ").append(dist2) //
                .append("\nsubspaceDim ").append(subspaceDim) //
                .append("\ncommon pv ").append(BitsUtil.toStringLow(commonPreferenceVector, dim)) //
                .toString());
          }
        }
        int prevdim = correlationValue.intValue(iter);
        // Cannot be better:
        if(prevdim < subspaceDim) {
          continue;
        }

        // Note: used to be: in orthogonal subspace?
        double orthogonalDistance = weightedDistance(v1, v2, commonPreferenceVector);

        if(prevdim == subspaceDim) {
          double prevdist = reachability.doubleValue(iter);
          if(prevdist <= orthogonalDistance) {
            continue; // Not better.
          }
        }
        correlationValue.putInt(iter, subspaceDim);
        reachability.putDouble(iter, orthogonalDistance);
        predecessor.putDBID(iter, id);
        commonPreferenceVectors.put(iter, commonPreferenceVector);
        if(prevdim == Integer.MAX_VALUE) {
          candidates.add(iter);
        }
      }
    }

    @Override
    public int compare(DBIDRef o1, DBIDRef o2) {
      int c1 = correlationValue.intValue(o1),
          c2 = correlationValue.intValue(o2);
      return (c1 < c2) ? -1 : (c1 > c2) ? +1 : //
          super.compare(o1, o2);
    }

    @Override
    protected Logging getLogger() {
      return LOG;
    }
  }

  /**
   * Computes the weighted distance between the two specified vectors according
   * to the given preference vector.
   * 
   * @param v1 the first vector
   * @param v2 the second vector
   * @param weightVector the preference vector
   * @return the weighted distance between the two specified vectors according
   *         to the given preference vector
   */
  public double weightedDistance(NumberVector v1, NumberVector v2, long[] weightVector) {
    double sqrDist = 0.;
    for(int i = BitsUtil.nextSetBit(weightVector, 0); i >= 0; i = BitsUtil.nextSetBit(weightVector, i + 1)) {
      double manhattanI = v1.doubleValue(i) - v2.doubleValue(i);
      sqrDist += manhattanI * manhattanI;
    }
    return Math.sqrt(sqrDist);
  }

  @Override
  public int getMinPts() {
    return 2;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Parameter to specify the maximum distance between two vectors with equal
     * preference vectors before considering them as parallel, must be a double
     * equal to or greater than 0.
     */
    public static final OptionID EPSILON_ID = new OptionID("hisc.epsilon", "The maximum distance between two vectors with equal preference vectors before considering them as parallel.");

    /**
     * The maximum absolute variance along a coordinate axis.
     * Must be in the range of [0.0, 1.0).
     */
    public static final OptionID ALPHA_ID = new OptionID("hisc.alpha", "The maximum absolute variance along a coordinate axis.");

    /**
     * The default value for alpha.
     */
    public static final double DEFAULT_ALPHA = 0.01;

    /**
     * The number of nearest neighbors considered to determine the preference
     * vector. If this value is not defined, k is set to three times of the
     * dimensionality of the database objects.
     */
    public static final OptionID K_ID = new OptionID("hisc.k", "The number of nearest neighbors considered to determine the preference vector. If this value is not defined, k ist set to three times of the dimensionality of the database objects.");

    /**
     * The maximum absolute variance along a coordinate axis.
     */
    protected double alpha;

    /**
     * The number of nearest neighbors considered to determine the preference
     * vector.
     */
    protected int k = 0;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(HiSC.Par.ALPHA_ID, HiSC.Par.DEFAULT_ALPHA) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE) //
          .grab(config, x -> alpha = x);
      new IntParameter(HiSC.Par.K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .setOptional(true) //
          .grab(config, x -> k = x);
    }

    @Override
    public HiSC make() {
      return new HiSC(alpha, k);
    }
  }
}
