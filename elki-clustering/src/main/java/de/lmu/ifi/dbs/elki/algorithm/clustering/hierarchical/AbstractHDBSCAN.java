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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.*;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.math.geometry.PrimsMinimumSpanningTree;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.DoubleLongHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Abstract base class for HDBSCAN variations.
 * <p>
 * Reference:
 * <p>
 * R. J. G. B. Campello, D. Moulavi, J. Sander<br>
 * Density-Based Clustering Based on Hierarchical Density Estimates<br>
 * Pacific-Asia Conf. Advances in Knowledge Discovery and Data Mining (PAKDD)
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @composed - - - HDBSCANAdapter
 * @composed - - - HeapMSTCollector
 *
 * @param <O> Input object type
 * @param <R> Output result type
 */
@Reference(authors = "R. J. G. B. Campello, D. Moulavi, J. Sander", //
    title = "Density-Based Clustering Based on Hierarchical Density Estimates", //
    booktitle = "Pacific-Asia Conf. Advances in Knowledge Discovery and Data Mining (PAKDD)", //
    url = "https://doi.org/10.1007/978-3-642-37456-2_14", //
    bibkey = "DBLP:conf/pakdd/CampelloMS13")
public abstract class AbstractHDBSCAN<O, R extends Result> extends AbstractDistanceBasedAlgorithm<O, R> {
  /**
   * MinPts parameter.
   */
  protected final int minPts;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param minPts Minimum number of points for density
   */
  public AbstractHDBSCAN(DistanceFunction<? super O> distanceFunction, int minPts) {
    super(distanceFunction);
    this.minPts = minPts;
  }

  /**
   * Compute the core distances for all objects.
   *
   * @param ids Objects
   * @param knnQ kNN query
   * @param minPts Minimum neighborhood size
   * @return Data store with core distances
   */
  protected WritableDoubleDataStore computeCoreDists(DBIDs ids, KNNQuery<O> knnQ, int minPts) {
    final Logging LOG = getLogger();
    final WritableDoubleDataStore coredists = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB);
    FiniteProgress cprog = LOG.isVerbose() ? new FiniteProgress("Computing core sizes", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      coredists.put(iter, knnQ.getKNNForDBID(iter, minPts).getKNNDistance());
      LOG.incrementProcessed(cprog);
    }
    LOG.ensureCompleted(cprog);
    return coredists;
  }

  /**
   * Class for processing the HDBSCAN G_mpts graph.
   *
   * @author Erich Schubert
   */
  protected static class HDBSCANAdapter implements PrimsMinimumSpanningTree.Adapter<ArrayDBIDs> {
    /**
     * IDs to process.
     */
    private ArrayDBIDs ids;

    /**
     * Iterators for accessing the data objects.
     */
    private DBIDArrayIter q, p;

    /**
     * Core distance storage.
     */
    private DoubleDataStore coredists;

    /**
     * Distance query for exact distances.
     */
    private DistanceQuery<?> distq;

    /**
     * Constructor.
     *
     * @param ids Ids to process.
     * @param coredists Core distances
     * @param distq Distance query
     */
    public HDBSCANAdapter(ArrayDBIDs ids, DoubleDataStore coredists, DistanceQuery<?> distq) {
      this.ids = ids;
      this.q = ids.iter();
      this.p = ids.iter();
      this.coredists = coredists;
      this.distq = distq;
    }

    @Override
    public double distance(ArrayDBIDs data, int ip, int iq) {
      p.seek(ip);
      q.seek(iq);
      double coreP = coredists.doubleValue(p);
      double coreQ = coredists.doubleValue(q);
      return MathUtil.max(coreP, coreQ, distq.distance(p, q));
    }

    @Override
    public int size(ArrayDBIDs data) {
      assert (data == ids);
      return ids.size();
    }
  };

  /**
   * Class for collecting the minimum spanning tree edges into a heap.
   *
   * @author Erich Schubert
   */
  public static class HeapMSTCollector implements PrimsMinimumSpanningTree.Collector {
    /**
     * Output heap.
     */
    private DoubleLongHeap heap;

    /**
     * Progress, for progress logging.
     */
    private FiniteProgress prog;

    /**
     * Logger, for progress logging.
     */
    private Logging log;

    /**
     * Constructor.
     *
     * @param heap Output heap.
     * @param prog Progress for logging. May be {@code null}.
     * @param log Logger for logging. May be {@code null}.
     */
    public HeapMSTCollector(DoubleLongHeap heap, FiniteProgress prog, Logging log) {
      this.heap = heap;
      this.prog = prog;
      this.log = log;
    }

    @Override
    public void addEdge(double length, int i, int j) {
      // Note: j is a unique key.
      heap.add(length, (((long) i) << 31) | j);
      if(log != null && prog != null) {
        log.incrementProcessed(prog);
      }
    }
  }

  /**
   * Convert spanning tree to a pointer representation.
   *
   * Note: the heap must use the correct encoding of indexes.
   *
   * @param ids IDs indexed
   * @param heap Heap
   * @param pi Parent array
   * @param lambda Distance array
   */
  protected void convertToPointerRepresentation(ArrayDBIDs ids, DoubleLongHeap heap, WritableDBIDDataStore pi, WritableDoubleDataStore lambda) {
    final Logging LOG = getLogger();
    // Initialize parent array:
    for(DBIDArrayIter iter = ids.iter(); iter.valid(); iter.advance()) {
      pi.put(iter, iter); // Initialize
    }
    DBIDVar p = DBIDUtil.newVar(), q = DBIDUtil.newVar(), n = DBIDUtil.newVar();
    FiniteProgress pprog = LOG.isVerbose() ? new FiniteProgress("Converting MST to pointer representation", heap.size(), LOG) : null;
    while(!heap.isEmpty()) {
      final double dist = heap.peekKey();
      final long pair = heap.peekValue();
      final int i = (int) (pair >>> 31), j = (int) (pair & 0x7FFFFFFFL);
      ids.assignVar(i, p);
      // Follow p to its parent.
      while(!DBIDUtil.equal(p, pi.assignVar(p, n))) {
        p.set(n);
      }
      // Follow q to its parent.
      ids.assignVar(j, q);
      while(!DBIDUtil.equal(q, pi.assignVar(q, n))) {
        q.set(n);
      }
      // By definition of the pointer representation, the largest element in
      // each cluster is the cluster lead.
      // The extraction methods currently rely on this!
      int c = DBIDUtil.compare(p, q);
      if(c < 0) {
        // p joins q:
        pi.put(p, q);
        lambda.put(p, dist);
      }
      else {
        assert (c != 0) : "This should never happen!";
        // q joins p:
        pi.put(q, p);
        lambda.put(q, dist);
      }

      heap.poll();
      LOG.incrementProcessed(pprog);
    }
    LOG.ensureCompleted(pprog);
    // Hack to ensure a valid pointer representation:
    // If distances are tied, the heap may return edges such that the n-way join
    // does not fulfill the property that the last element has the largest id.
    for(DBIDArrayIter iter = ids.iter(); iter.valid(); iter.advance()) {
      double d = lambda.doubleValue(iter);
      // Parent:
      pi.assignVar(iter, p);
      q.set(p);
      // Follow parent while tied.
      while(d >= lambda.doubleValue(q) && !DBIDUtil.equal(q, pi.assignVar(q, n))) {
        q.set(n);
      }
      if(!DBIDUtil.equal(p, q)) {
        if(LOG.isDebuggingFinest()) {
          LOG.finest("Correcting parent: " + p + " -> " + q);
        }
        pi.put(iter, q);
      }
    }
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public abstract static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Option ID for linkage parameter.
     */
    public static final OptionID MIN_PTS_ID = new OptionID("hdbscan.minPts", "Threshold for minimum number of points in the epsilon-neighborhood of a point (including this point).");

    /**
     * Minimum size of core.
     */
    protected int minPts;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config); // distanceFunction

      IntParameter minptsP = new IntParameter(MIN_PTS_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
      if(config.grab(minptsP)) {
        minPts = minptsP.getValue();
      }
    }
  }
}
