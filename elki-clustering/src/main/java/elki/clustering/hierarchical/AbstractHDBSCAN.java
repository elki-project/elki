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
package elki.clustering.hierarchical;

import elki.Algorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.DoubleDataStore;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.math.MathUtil;
import elki.math.geometry.PrimsMinimumSpanningTree;
import elki.utilities.datastructures.heap.DoubleLongHeap;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

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
 */
@Reference(authors = "R. J. G. B. Campello, D. Moulavi, J. Sander", //
    title = "Density-Based Clustering Based on Hierarchical Density Estimates", //
    booktitle = "Pacific-Asia Conf. Advances in Knowledge Discovery and Data Mining (PAKDD)", //
    url = "https://doi.org/10.1007/978-3-642-37456-2_14", //
    bibkey = "DBLP:conf/pakdd/CampelloMS13")
public abstract class AbstractHDBSCAN<O> implements Algorithm {
  /**
   * MinPts parameter.
   */
  protected final int minPts;

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param minPts Minimum number of points for coredists
   */
  public AbstractHDBSCAN(Distance<? super O> distance, int minPts) {
    super();
    this.distance = distance;
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
  protected WritableDoubleDataStore computeCoreDists(DBIDs ids, KNNSearcher<DBIDRef> knnQ, int minPts) {
    final Logging LOG = getLogger();
    final WritableDoubleDataStore coredists = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_DB);
    FiniteProgress cprog = LOG.isVerbose() ? new FiniteProgress("Computing core sizes", ids.size(), LOG) : null;
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      coredists.put(iter, knnQ.getKNN(iter, minPts).getKNNDistance());
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
  }

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
   * <p>
   * Note: the heap must use the correct encoding of indexes.
   *
   * @param ids IDs indexed
   * @param heap Heap
   * @param builder Hierarchy builder
   * @return builder, for method chaining
   */
  protected ClusterMergeHistoryBuilder convertToMergeList(ArrayDBIDs ids, DoubleLongHeap heap, ClusterMergeHistoryBuilder builder) {
    final Logging LOG = getLogger();
    FiniteProgress pprog = LOG.isVerbose() ? new FiniteProgress("Converting MST to pointer representation", heap.size(), LOG) : null;
    while(!heap.isEmpty()) {
      final long pair = heap.peekValue();
      final int i = (int) (pair >>> 31), j = (int) (pair & 0x7FFFFFFFL);
      builder.add(i, heap.peekKey(), j);
      heap.poll();
      LOG.incrementProcessed(pprog);
    }
    LOG.ensureCompleted(pprog);
    return builder;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Get the (STATIC) logger for this class.
   *
   * @return the static logger
   */
  protected abstract Logging getLogger();

  /**
   * Parameterization class
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <O> Object type
   */
  public abstract static class Par<O> implements Parameterizer {
    /**
     * Option ID for linkage parameter.
     */
    public static final OptionID MIN_PTS_ID = new OptionID("hdbscan.minPts", "Threshold for minimum number of points in the epsilon-neighborhood of a point (including this point).");

    /**
     * Minimum size of core.
     */
    protected int minPts;

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(MIN_PTS_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> minPts = x);
    }
  }
}
