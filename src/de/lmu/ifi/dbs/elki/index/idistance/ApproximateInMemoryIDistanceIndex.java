package de.lmu.ifi.dbs.elki.index.idistance;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMedoidsInitialization;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNHeap;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.AbstractRefiningIndex;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.statistics.DoubleStatistic;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.math.MeanVarianceMinMax;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.DoubleIntPair;

/**
 * Approximate In-memory iDistance index, using a reference point embedding.
 * 
 * <b>Important note:</b> this currently is an approximative index using
 * incremental computation. The original publication discusses an exact indexing
 * method based on repeated radius queries. As such, we use a substantially
 * different but likely faster query strategy. We also do not use a B+-tree as
 * data structure, but simple in-memory lists. Therefore, we cannot report page
 * accesses needed.
 * 
 * <b>Important note:</b> the current implementation <em>may</em> return less
 * than k neighbors, if you use many reference points.
 * 
 * Feel free to contribute improved query strategies. All the code is
 * essentially here, you only need to query every reference point list, not just
 * the best.
 * 
 * Reference:
 * <p>
 * C. Yu, B. C. Ooi, K. L. Tan, H. V. Jagadish<br />
 * Indexing the Distance: An Efficient Method to KNN Processing.<br />
 * In Proceedings of the 27th International Conference on Very Large Data Bases
 * </p>
 * 
 * <p>
 * H. V. Jagadish, B. C. Ooi, K. L. Tan, C. Yu, R. Zhang<br />
 * iDistance: An adaptive B+-tree based indexing method for nearest neighbor
 * search.<br />
 * ACM Transactions on Database Systems (TODS), 30(2), 364-397.
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
@Reference(authors = "C. Yu, B. C. Ooi, K. L. Tan, H. V. Jagadish", title = "Indexing the distance: An efficient method to knn processing", booktitle = "In Proceedings of the 27th International Conference on Very Large Data Bases", url = "http://www.vldb.org/conf/2001/P421.pdf")
public class ApproximateInMemoryIDistanceIndex<O> extends AbstractRefiningIndex<O> implements RangeIndex<O>, KNNIndex<O> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ApproximateInMemoryIDistanceIndex.class);

  /**
   * Distance query.
   */
  private DistanceQuery<O> distanceQuery;

  /**
   * Initialization method.
   */
  private KMedoidsInitialization<O> initialization;

  /**
   * Number of reference points.
   */
  private int k;

  /**
   * Reference points.
   */
  private ArrayDBIDs referencepoints;

  /**
   * The actual index.
   */
  private ModifiableDoubleDBIDList[] index;

  /**
   * Second reference, for documentation generation.
   */
  @Reference(authors = "H. V. Jagadish, B. C. Ooi, K. L. Tan, C. Yu, R. Zhang", title = "iDistance: An adaptive B+-tree based indexing method for nearest neighbor search", booktitle = "ACM Transactions on Database Systems (TODS), 30(2), 364-397")
  public static final Void SECOND_REFERENCE = null;

  /**
   * Constructor.
   * 
   * @param relation Data relation
   * @param distance Distance
   * @param initialization Initialization method
   * @param k Number of reference points
   */
  public ApproximateInMemoryIDistanceIndex(Relation<O> relation, DistanceQuery<O> distance, KMedoidsInitialization<O> initialization, int k) {
    super(relation);
    this.distanceQuery = distance;
    this.initialization = initialization;
    this.k = k;
    if(!distance.getDistanceFunction().isMetric()) {
      LOG.warning("iDistance assumes metric distance functions.\n" //
          + distance.getDistanceFunction().getClass() + " does not report itself as metric.\n" //
          + "iDistance will run, but may yield approximate results.");
    }
  }

  @Override
  public void initialize() {
    referencepoints = DBIDUtil.ensureArray(initialization.chooseInitialMedoids(k, distanceQuery));
    final int k = referencepoints.size(); // should be the same k anyway.
    index = new ModifiableDoubleDBIDList[k];
    for(int i = 0; i < k; i++) {
      index[i] = DBIDUtil.newDistanceDBIDList(relation.size() / (2 * k));
    }
    // TODO: add optimized codepath for primitive distances.
    DBIDArrayIter riter = referencepoints.iter();
    for(DBIDIter oiter = relation.iterDBIDs(); oiter.valid(); oiter.advance()) {
      double bestd = Double.POSITIVE_INFINITY;
      int besti = -1;
      for(riter.seek(0); riter.valid(); riter.advance()) {
        double dist = distanceQuery.distance(oiter, riter);
        if(dist < bestd) {
          bestd = dist;
          besti = riter.getOffset();
        }
      }
      assert (besti >= 0 && besti < k);
      index[besti].add(bestd, oiter);
    }

    // Sort index.
    for(int i = 0; i < k; i++) {
      index[i].sort();
    }
  }

  @Override
  public KNNQuery<O> getKNNQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    DistanceFunction<? super O> distanceFunction = (DistanceFunction<? super O>) distanceQuery.getDistanceFunction();
    if(!this.getDistanceFunction().equals(distanceFunction)) {
      if(LOG.isDebugging()) {
        LOG.debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    for(Object hint : hints) {
      // Approximative querys only, for now. (TODO)
      if(DatabaseQuery.HINT_EXACT.equals(hint)) {
        return null;
      }
    }
    return new ApproximateIDistanceKNNQuery(distanceQuery);
  }

  @Override
  public RangeQuery<O> getRangeQuery(DistanceQuery<O> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    DistanceFunction<? super O> distanceFunction = (DistanceFunction<? super O>) distanceQuery.getDistanceFunction();
    if(!this.getDistanceFunction().equals(distanceFunction)) {
      if(LOG.isDebugging()) {
        LOG.debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    return new ExactIDistanceRangeQuery(distanceQuery);
  }

  /**
   * Distance function.
   * 
   * @return Distance function
   */
  private DistanceFunction<? super O> getDistanceFunction() {
    return distanceQuery.getDistanceFunction();
  }

  @Override
  public String getLongName() {
    return "iDistance index";
  }

  @Override
  public String getShortName() {
    return "idistance-index";
  }

  @Override
  public Logging getLogger() {
    return LOG;
  }

  @Override
  public void logStatistics() {
    super.logStatistics();
    MeanVarianceMinMax mm = new MeanVarianceMinMax();
    for(int i = 0; i < k; i++) {
      mm.put(index[i].size());
    }
    LOG.statistics(new LongStatistic(ApproximateInMemoryIDistanceIndex.class.getName() + ".size.min", (int) mm.getMin()));
    LOG.statistics(new DoubleStatistic(ApproximateInMemoryIDistanceIndex.class.getName() + ".size.mean", mm.getMean()));
    LOG.statistics(new LongStatistic(ApproximateInMemoryIDistanceIndex.class.getName() + ".size.max", (int) mm.getMax()));
  }

  /**
   * Sort the reference points by distance to the query object
   * 
   * @param distanceQuery Distance query
   * @param obj Query object
   * @param referencepoints Iterator for reference points
   * @return Sorted array.
   */
  protected static <O> DoubleIntPair[] rankReferencePoints(DistanceQuery<O> distanceQuery, O obj, ArrayDBIDs referencepoints) {
    DoubleIntPair[] priority = new DoubleIntPair[referencepoints.size()];
    // Compute distances to reference points.
    for(DBIDArrayIter iter = referencepoints.iter(); iter.valid(); iter.advance()) {
      final int i = iter.getOffset();
      final double dist = distanceQuery.distance(obj, iter);
      priority[i] = new DoubleIntPair(dist, i);
    }
    Arrays.sort(priority);
    return priority;
  }

  /**
   * Seek an iterator to the desired position, using binary search.
   * 
   * @param index Index to search
   * @param iter Iterator
   * @param val Distance to search to
   */
  protected static void binarySearch(ModifiableDoubleDBIDList index, DoubleDBIDListIter iter, double val) {
    // Binary search. TODO: move this into the DoubleDBIDList class.
    int left = 0, right = index.size() - 1;
    while(left <= right) {
      final int mid = (left + right) >>> 1;
      final double curd = iter.seek(mid).doubleValue();
      if(val < curd) {
        right = mid - 1;
      }
      else if(val > curd) {
        left = mid + 1;
      }
      else {
        break;
      }
    }
    iter.seek(left);
  }

  /**
   * kNN query implementation.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class ApproximateIDistanceKNNQuery extends AbstractRefiningIndex<O>.AbstractKNNQuery {
    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query
     */
    public ApproximateIDistanceKNNQuery(DistanceQuery<O> distanceQuery) {
      super(distanceQuery);
    }

    @Override
    public KNNList getKNNForObject(O obj, int k) {
      DoubleIntPair[] priority = rankReferencePoints(distanceQuery, obj, referencepoints);
      // Approximate kNN search. We do not check _every_ list.
      KNNHeap heap = DBIDUtil.newHeap(k);

      for(DoubleIntPair pair : priority) {
        final ModifiableDoubleDBIDList nindex = index[pair.second];
        final double bestd = pair.first;

        final DoubleDBIDListIter ifwd = nindex.iter(), ibwd = nindex.iter();
        binarySearch(nindex, ibwd, bestd);
        ifwd.seek(ibwd.getOffset() + 1);
        // LOG.debug((ibwd.valid() ? ibwd.doubleValue() : 0.) + " <= " + bestd + " <= " + (ifwd.valid() ? ifwd.doubleValue() : Double.POSITIVE_INFINITY));

        // This assumes a metric, as we exploit triangle inequality:
        // Lower bound for candidates further from the reference object:
        // d(query, reference) <= d(query, candidate) + d(candidate, reference)
        // d(query, reference) - d(candidate, reference) <= d(query, candidate)
        double lbfwd = ifwd.valid() ? (bestd - ifwd.doubleValue()) : Double.NaN;
        // Lower bound for candidates closer to the reference object:
        // d(candidate, reference) <= d(candidate, query) + d(query, reference)
        // d(candidate, reference) - d(query, reference) <= d(candidate, query)
        double lbbwd = ibwd.valid() ? (ibwd.doubleValue() - bestd) : Double.NaN;
        while(true) {
          double kdist = heap.getKNNDistance();
          // Handle NaN carefully.
          if(!(lbfwd <= kdist) && !(lbbwd <= kdist)) {
            break;
          }
          // Careful: NaN handling: not NaN and not worse than fwd (may be NaN).
          if(lbfwd <= kdist) {
            heap.insert(refine(ifwd, obj), ifwd);
            // Advance iterator:
            ifwd.advance();
            lbfwd = ifwd.valid() ? (bestd - ifwd.doubleValue()) : Double.NaN;
          }
          if(lbbwd <= kdist) {
            heap.insert(refine(ibwd, obj), ibwd);
            // Retract iterator:
            ibwd.retract();
            lbbwd = ibwd.valid() ? (ibwd.doubleValue() - bestd) : Double.NaN;
          }
        }
        if(heap.size() >= k) {
          break; // Stop searching
        }
      }

      return heap.toKNNList();
    }
  }

  /**
   * Exact Range query implementation.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  protected class ExactIDistanceRangeQuery extends AbstractRefiningIndex<O>.AbstractRangeQuery {
    /**
     * Constructor.
     * 
     * @param distanceQuery Distance query
     */
    public ExactIDistanceRangeQuery(DistanceQuery<O> distanceQuery) {
      super(distanceQuery);
    }

    @Override
    public DoubleDBIDList getRangeForObject(O obj, double range) {
      DoubleIntPair[] priority = rankReferencePoints(distanceQuery, obj, referencepoints);
      ModifiableDoubleDBIDList result = DBIDUtil.newDistanceDBIDList();
      for(DoubleIntPair pair : priority) {
        final ModifiableDoubleDBIDList nindex = index[pair.second];
        final double bestd = pair.first;

        DoubleDBIDListIter ifwd = nindex.iter(), ibwd = nindex.iter();
        binarySearch(nindex, ifwd, bestd);
        ibwd.seek(ifwd.getOffset() + 1);

        // This assumes a metric, as we exploit triangle inequality:
        // Lower bound for candidates further from the reference object:
        // d(query, reference) <= d(query, candidate) + d(candidate, reference)
        // d(query, reference) - d(candidate, reference) <= d(query, candidate)
        double lbfwd = ifwd.valid() ? (bestd - ifwd.doubleValue()) : Double.NaN;
        // Lower bound for candidates closer to the reference object:
        // d(candidate, reference) <= d(candidate, query) + d(query, reference)
        // d(candidate, reference) - d(query, reference) <= d(candidate, query)
        double lbbwd = ibwd.valid() ? (ibwd.doubleValue() - bestd) : Double.NaN;
        while(true) {
          // Handle NaN carefully.
          if(!(lbfwd <= range) && !(lbbwd <= range)) {
            break;
          }
          // Careful: NaN handling: not NaN and not worse than fwd (may be NaN).
          if(lbfwd == lbfwd && !(lbfwd > lbbwd)) {
            result.add(refine(ifwd, obj), ifwd);
            // Advance iterator:
            ifwd.advance();
            lbfwd = ifwd.valid() ? (bestd - ifwd.doubleValue()) : Double.NaN;
          }
          if(lbbwd == lbbwd && !(lbbwd > lbfwd)) {
            result.add(refine(ibwd, obj), ibwd);
            // Retract iterator:
            ibwd.retract();
            lbbwd = ibwd.valid() ? (ibwd.doubleValue() - bestd) : Double.NaN;
          }
        }
      }

      return result;
    }
  }

  /**
   * Index factory for iDistance indexes.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has InMemoryIDistanceIndex
   * 
   * @param <V> Data type.
   */
  public static class Factory<V> implements IndexFactory<V, ApproximateInMemoryIDistanceIndex<V>> {
    /**
     * Distance function to use.
     */
    DistanceFunction<? super V> distance;

    /**
     * Initialization method.
     */
    KMedoidsInitialization<V> initialization;

    /**
     * Number of reference points
     */
    int k;

    /**
     * Constructor.
     * 
     * @param distance Distance function
     * @param initialization Initialization method
     * @param k Number of reference points
     */
    public Factory(DistanceFunction<? super V> distance, KMedoidsInitialization<V> initialization, int k) {
      super();
      this.distance = distance;
      this.initialization = initialization;
      this.k = k;
    }

    @Override
    public ApproximateInMemoryIDistanceIndex<V> instantiate(Relation<V> relation) {
      return new ApproximateInMemoryIDistanceIndex<>(relation, distance.instantiate(relation), initialization, k);
    }

    @Override
    public TypeInformation getInputTypeRestriction() {
      return distance.getInputTypeRestriction();
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     * 
     * @param <V> object type.
     */
    public static class Parameterizer<V> extends AbstractParameterizer {
      /**
       * Parameter for the distance function
       */
      public static final OptionID DISTANCE_ID = new OptionID("idistance.distance", "Distance function to build the index for.");

      /**
       * Initialization method.
       */
      public static final OptionID REFERENCE_ID = new OptionID("idistance.reference", "Method to choose the reference points.");

      /**
       * Number of reference points.
       */
      public static final OptionID K_ID = new OptionID("idistance.k", "Number of reference points to use.");

      /**
       * Distance function to use.
       */
      DistanceFunction<? super V> distance;

      /**
       * Initialization method.
       */
      KMedoidsInitialization<V> initialization;

      /**
       * Number of reference points
       */
      int k;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        ObjectParameter<DistanceFunction<? super V>> distanceP = new ObjectParameter<>(DISTANCE_ID, DistanceFunction.class);
        if(config.grab(distanceP)) {
          distance = distanceP.instantiateClass(config);
        }

        ObjectParameter<KMedoidsInitialization<V>> initializationP = new ObjectParameter<>(REFERENCE_ID, KMedoidsInitialization.class);
        if(config.grab(initializationP)) {
          initialization = initializationP.instantiateClass(config);
        }

        IntParameter kP = new IntParameter(K_ID)//
        .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
        if(config.grab(kP)) {
          k = kP.intValue();
        }
      }

      @Override
      protected ApproximateInMemoryIDistanceIndex.Factory<V> makeInstance() {
        return new ApproximateInMemoryIDistanceIndex.Factory<>(distance, initialization, k);
      }
    }
  }
}
