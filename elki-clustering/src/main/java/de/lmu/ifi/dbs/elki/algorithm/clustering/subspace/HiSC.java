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
package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.ClusterOrder;
import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.CorrelationClusterOrder;
import de.lmu.ifi.dbs.elki.algorithm.clustering.optics.GeneralizedOPTICS;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.datastore.WritableIntegerDataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.preprocessed.preference.HiSCPreferenceVectorIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

import net.jafama.FastMath;

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
 *
 * @param <V> the type of NumberVector handled by the algorithm
 */
@Title("Finding Hierarchies of Subspace Clusters")
@Description("Algorithm for detecting hierarchies of subspace clusters.")
@Reference(authors = "Elke Achtert, Christian Böhm, Hans-Petre Kriegel, Peer Kröger, Ina Müller-Gorman, Arthur Zimek", //
    title = "Finding Hierarchies of Subspace Clusters", //
    booktitle = "Proc. 10th Europ. Conf. on Principles and Practice of Knowledge Discovery in Databases (PKDD'06)", //
    url = "https://doi.org/10.1007/11871637_42", //
    bibkey = "DBLP:conf/pkdd/AchtertBKKMZ06")
public class HiSC<V extends NumberVector> extends GeneralizedOPTICS<V, CorrelationClusterOrder> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(HiSC.class);

  /**
   * Factory to produce
   */
  private HiSCPreferenceVectorIndex.Factory<V> indexfactory;

  /**
   * Holds the maximum diversion allowed.
   */
  private double alpha;

  /**
   * Constructor.
   * 
   * @param indexfactory HiSC index factory
   */
  public HiSC(HiSCPreferenceVectorIndex.Factory<V> indexfactory) {
    super();
    this.indexfactory = indexfactory;
    this.alpha = indexfactory.getAlpha();
  }

  @Override
  public ClusterOrder run(Database db, Relation<V> relation) {
    return new Instance(db, relation).run();
  }

  /**
   * Algorithm instance for a single data set.
   *
   * @author Erich Schubert
   */
  private class Instance extends GeneralizedOPTICS.Instance<V, CorrelationClusterOrder> {
    /**
     * Instantiated index.
     */
    private HiSCPreferenceVectorIndex<V> index;

    /**
     * Cluster order.
     */
    private ArrayModifiableDBIDs clusterOrder;

    /**
     * Data relation.
     */
    private Relation<V> relation;

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
     * @param db Database
     * @param relation Relation
     */
    public Instance(Database db, Relation<V> relation) {
      super(db, relation);
      DBIDs ids = relation.getDBIDs();
      this.clusterOrder = DBIDUtil.newArray(ids.size());
      this.relation = relation;
      this.correlationValue = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_DB, Integer.MAX_VALUE);
      this.commonPreferenceVectors = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_TEMP, long[].class);
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
      commonPreferenceVectors.put(id, new long[0]);
    }

    @Override
    protected void expandDBID(DBIDRef id) {
      clusterOrder.add(id);

      final long[] pv1 = index.getPreferenceVector(id);
      final V v1 = relation.get(id);
      final int dim = v1.getDimensionality();

      for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
        // We can aggressively ignore processed objects, because we do not
        // have a minPts parameter in HiSC:
        if(processedIDs.contains(iter)) {
          continue;
        }
        long[] pv2 = index.getPreferenceVector(iter);
        V v2 = relation.get(iter);
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
  public double weightedDistance(V v1, V v2, long[] weightVector) {
    double sqrDist = 0.;
    for(int i = BitsUtil.nextSetBit(weightVector, 0); i >= 0; i = BitsUtil.nextSetBit(weightVector, i + 1)) {
      double manhattanI = v1.doubleValue(i) - v2.doubleValue(i);
      sqrDist += manhattanI * manhattanI;
    }
    return FastMath.sqrt(sqrDist);
  }

  @Override
  public int getMinPts() {
    return 2;
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
     * Parameter to specify the maximum distance between two vectors with equal
     * preference vectors before considering them as parallel, must be a double
     * equal to or greater than 0.
     */
    public static final OptionID EPSILON_ID = new OptionID("hisc.epsilon", "The maximum distance between two vectors with equal preference vectors before considering them as parallel.");

    /**
     * Factory to produce the index.
     */
    private HiSCPreferenceVectorIndex.Factory<V> indexfactory;

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

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Class<HiSCPreferenceVectorIndex.Factory<V>> cls = ClassGenericsUtil.uglyCastIntoSubclass(HiSCPreferenceVectorIndex.Factory.class);
      indexfactory = config.tryInstantiate(cls);
    }

    @Override
    protected HiSC<V> makeInstance() {
      return new HiSC<>(indexfactory);
    }
  }
}
