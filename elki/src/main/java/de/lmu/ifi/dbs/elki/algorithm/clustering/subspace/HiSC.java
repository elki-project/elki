package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

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
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.IndexBasedDistanceFunction;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.preprocessed.preference.HiSCPreferenceVectorIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
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

/**
 * Implementation of the HiSC algorithm, an algorithm for detecting hierarchies
 * of subspace clusters.
 * <p>
 * Reference: E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, I. Müller-Gorman,
 * A. Zimek<br />
 * Finding Hierarchies of Subspace Clusters. <br />
 * In: Proc. 10th Europ. Conf. on Principles and Practice of Knowledge Discovery
 * in Databases (PKDD'06), Berlin, Germany, 2006.
 * </p>
 * 
 * @author Elke Achtert
 * @since 0.3
 * 
 * @apiviz.uses HiSCPreferenceVectorIndex
 * 
 * @param <V> the type of NumberVector handled by the algorithm
 */
@Title("Finding Hierarchies of Subspace Clusters")
@Description("Algorithm for detecting hierarchies of subspace clusters.")
@Reference(authors = "E. Achtert, C. Böhm, H.-P. Kriegel, P. Kröger, I. Müller-Gorman, A. Zimek",//
title = "Finding Hierarchies of Subspace Clusters", //
booktitle = "Proc. 10th Europ. Conf. on Principles and Practice of Knowledge Discovery in Databases (PKDD'06), Berlin, Germany, 2006", //
url = "http://www.dbs.ifi.lmu.de/Publikationen/Papers/PKDD06-HiSC.pdf")
public class HiSC<V extends NumberVector> extends GeneralizedOPTICS<V, CorrelationClusterOrder> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(HiSC.class);

  /**
   * Factory to produce
   */
  private IndexFactory<V, HiSCPreferenceVectorIndex<NumberVector>> indexfactory;

  /**
   * Holds the maximum diversion allowed.
   */
  private double alpha;

  /**
   * Constructor.
   * 
   * @param indexfactory HiSC index factory
   */
  public HiSC(IndexFactory<V, HiSCPreferenceVectorIndex<NumberVector>> indexfactory, double epsilon) {
    super();
    this.indexfactory = indexfactory;
    this.alpha = epsilon;
  }

  @Override
  public ClusterOrder run(Database db, Relation<V> relation) {
    return new Instance(db, relation).run();
  }

  /**
   * Algorithm instance for a single data set.
   * 
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  private class Instance extends GeneralizedOPTICS.Instance<V, CorrelationClusterOrder> {
    /**
     * Instantiated index.
     */
    private HiSCPreferenceVectorIndex<NumberVector> index;

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
            StringBuilder msg = new StringBuilder();
            msg.append("dist1 ").append(dist1);
            msg.append("\ndist2 ").append(dist2);
            msg.append("\nsubspaceDim ").append(subspaceDim);
            msg.append("\ncommon pv ").append(BitsUtil.toStringLow(commonPreferenceVector, dim));
            LOG.debugFine(msg.toString());
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
      int c1 = correlationValue.intValue(o1), c2 = correlationValue.intValue(o2);
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
    return Math.sqrt(sqrDist);
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter to specify the maximum distance between two vectors with equal
     * preference vectors before considering them as parallel, must be a double
     * equal to or greater than 0.
     * <p>
     * Default value: {@code 0.001}
     * </p>
     * <p>
     * Key: {@code -hisc.epsilon}
     * </p>
     */
    public static final OptionID EPSILON_ID = new OptionID("hisc.epsilon", "The maximum distance between two vectors with equal preference vectors before considering them as parallel.");

    /**
     * Factory to produce the index.
     */
    private IndexFactory<V, HiSCPreferenceVectorIndex<NumberVector>> indexfactory;

    /**
     * Alpha parameter.
     */
    double alpha;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter alphaP = new DoubleParameter(HiSCPreferenceVectorIndex.Factory.ALPHA_ID, HiSCPreferenceVectorIndex.Factory.DEFAULT_ALPHA);
      alphaP.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      alphaP.addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
      if(config.grab(alphaP)) {
        alpha = alphaP.doubleValue();
      }

      // Configure HiSC distance function
      ListParameterization opticsParameters = new ListParameterization();

      // preprocessor
      opticsParameters.addParameter(IndexBasedDistanceFunction.INDEX_ID, HiSCPreferenceVectorIndex.Factory.class);
      opticsParameters.addParameter(HiSCPreferenceVectorIndex.Factory.ALPHA_ID, alpha);

      ChainedParameterization chain = new ChainedParameterization(opticsParameters, config);
      chain.errorsTo(config);
      final Class<? extends IndexFactory<V, HiSCPreferenceVectorIndex<NumberVector>>> cls = ClassGenericsUtil.uglyCrossCast(HiSCPreferenceVectorIndex.Factory.class, IndexFactory.class);
      indexfactory = chain.tryInstantiate(cls);
    }

    @Override
    protected HiSC<V> makeInstance() {
      return new HiSC<>(indexfactory, alpha);
    }
  }
}