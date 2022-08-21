/*
 * Copyright (C) 2015
 * Johannes Schneider, ABB Research, Switzerland,
 * johannes.schneider@alumni.ethz.ch
 * 
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
package elki.clustering.optics;

import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.*;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.minkowski.EuclideanDistance;
import elki.index.preprocessed.fastoptics.RandomProjectedNeighborsAndDensities;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.result.Metadata;
import elki.utilities.ClassGenericsUtil;
import elki.utilities.datastructures.heap.UpdatableHeap;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * FastOPTICS algorithm (Fast approximation of OPTICS)
 * <p>
 * Note that this is <em>not</em> FOPTICS as in "Fuzzy OPTICS"!
 * <p>
 * Reference:
 * <p>
 * J. Schneider, M. Vlachos<br>
 * Fast parameterless density-based clustering via random projections<br>
 * Proc. 22nd ACM Int. Conf. on Information and Knowledge Management (CIKM 2013)
 * <p>
 * This is based on the original code provided by Johannes Schneider, with
 * ELKIfications and optimizations by Erich Schubert.
 *
 * @author Johannes Schneider
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @composed - - - RandomProjectedNeighborsAndDensities
 * 
 * @param <V> Input vector type
 */
@Reference(authors = "J. Schneider, M. Vlachos", //
    title = "Fast parameterless density-based clustering via random projections", //
    booktitle = "Proc. 22nd ACM Int. Conf. on Information & Knowledge Management (CIKM 2013)", //
    url = "https://doi.org/10.1145/2505515.2505590", //
    bibkey = "DBLP:conf/cikm/SchneiderV13")
public class FastOPTICS<V extends NumberVector> implements OPTICSTypeAlgorithm {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(FastOPTICS.class);

  /**
   * undefined value for (reachability/average) distance
   */
  public static final double UNDEFINED_DISTANCE = -0.1f;

  /**
   * Result: output order of points
   */
  ClusterOrder order;

  /**
   * Result: reachability distances
   */
  WritableDoubleDataStore reachDist;

  /**
   * processed points
   */
  ModifiableDBIDs processed;

  /**
   * neighbors of a point
   */
  DataStore<? extends DBIDs> neighs;

  /**
   * Inverse Densities correspond to average distances in point set of
   * projections
   */
  DoubleDataStore inverseDensities;

  /**
   * MinPts parameter.
   */
  int minPts;

  /**
   * Index.
   */
  RandomProjectedNeighborsAndDensities index;

  /**
   * Constructor.
   *
   * @param minpts Minimum number of neighbors.
   * @param index Index
   */
  public FastOPTICS(int minpts, RandomProjectedNeighborsAndDensities index) {
    super();
    this.minPts = minpts;
    this.index = index;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Run the algorithm.
   *
   * @param relation Relation
   */
  public ClusterOrder run(Relation<V> relation) {
    DBIDs ids = relation.getDBIDs();
    DistanceQuery<V> dq = new QueryBuilder<>(relation, EuclideanDistance.STATIC).distanceQuery();

    // initialize points used and reachability distance
    reachDist = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, UNDEFINED_DISTANCE);

    // compute projections, density estimates and neighborhoods
    index.computeSetsBounds(relation, minPts, ids); // project points
    inverseDensities = index.computeAverageDistInSet(); // compute densities
    neighs = index.getNeighs(); // get neighbors of points

    // compute ordering as for OPTICS
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("FastOPTICS clustering", ids.size(), LOG) : null;
    processed = DBIDUtil.newHashSet(ids.size());
    order = new ClusterOrder(ids);
    Metadata.of(order).setLongName("FastOPTICS Cluster Order");
    for(DBIDIter it = ids.iter(); it.valid(); it.advance()) {
      if(!processed.contains(it)) {
        expandClusterOrder(DBIDUtil.deref(it), order, dq, prog);
      }
    }
    index.logStatistics();
    LOG.ensureCompleted(prog);
    return order;
  }

  /**
   * OPTICS algorithm for processing a point, but with different density
   * estimates
   *
   * @param ipt Point
   * @param order Cluster order (output)
   * @param dq Distance query
   * @param prog Progress for logging.
   */
  protected void expandClusterOrder(DBID ipt, ClusterOrder order, DistanceQuery<V> dq, FiniteProgress prog) {
    UpdatableHeap<OPTICSHeapEntry> heap = new UpdatableHeap<>();
    heap.add(new OPTICSHeapEntry(ipt, null, Double.POSITIVE_INFINITY));
    while(!heap.isEmpty()) {
      final OPTICSHeapEntry current = heap.poll();
      DBID currPt = current.objectID;
      order.add(currPt, current.reachability, current.predecessorID);
      processed.add(currPt);
      double coredist = inverseDensities.doubleValue(currPt);
      for(DBIDIter it = neighs.get(currPt).iter(); it.valid(); it.advance()) {
        if(processed.contains(it)) {
          continue;
        }
        double nrdist = dq.distance(currPt, it);
        if(coredist > nrdist) {
          nrdist = coredist;
        }
        if(reachDist.doubleValue(it) == UNDEFINED_DISTANCE || nrdist < reachDist.doubleValue(it)) {
          reachDist.put(it, nrdist);
        }
        heap.add(new OPTICSHeapEntry(DBIDUtil.deref(it), currPt, nrdist));
      }
      LOG.incrementProcessed(prog);
    }
  }

  @Override
  public int getMinPts() {
    return minPts;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <V> Vector type
   */
  public static class Par<V extends NumberVector> implements Parameterizer {
    /**
     * Minimum number of neighbors for density estimation.
     */
    int minpts;

    /**
     * Random projection index.
     */
    RandomProjectedNeighborsAndDensities index;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(AbstractOPTICS.Par.MINPTS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> minpts = x);
      Class<RandomProjectedNeighborsAndDensities> clz = ClassGenericsUtil.uglyCastIntoSubclass(RandomProjectedNeighborsAndDensities.class);
      index = config.tryInstantiate(clz);
    }

    @Override
    public FastOPTICS<V> make() {
      return new FastOPTICS<>(minpts, index);
    }
  }
}
