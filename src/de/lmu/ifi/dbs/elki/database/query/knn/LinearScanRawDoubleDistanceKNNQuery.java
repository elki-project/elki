package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.RawDoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;

/**
 * Optimized linear scan query for {@link RawDoubleDistance}s.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses RawDoubleDistance
 * 
 * @param <O> Object type
 */
public class LinearScanRawDoubleDistanceKNNQuery<O> extends LinearScanPrimitiveDistanceKNNQuery<O, DoubleDistance> {
  /**
   * Constructor.
   * 
   * @param relation Data to query
   * @param distanceQuery Distance function to use
   */
  public LinearScanRawDoubleDistanceKNNQuery(Relation<? extends O> relation, PrimitiveDistanceQuery<O, DoubleDistance> distanceQuery) {
    super(relation, distanceQuery);
    if(!(distanceQuery.getDistanceFunction() instanceof RawDoubleDistance)) {
      throw new UnsupportedOperationException("LinearScanRawDoubleDistance instantiated for non-RawDoubleDistance!");
    }
  }

  @Override
  public List<DistanceResultPair<DoubleDistance>> getKNNForDBID(DBID id, int k) {
    return getKNNForObject(relation.get(id), k);
  }

  @Override
  public List<DistanceResultPair<DoubleDistance>> getKNNForObject(O obj, int k) {
    @SuppressWarnings("unchecked")
    final RawDoubleDistance<O> rawdist = (RawDoubleDistance<O>) distanceQuery.getDistanceFunction();
    // Optimization for double distances.
    final KNNHeap<DoubleDistance> heap = new KNNHeap<DoubleDistance>(k);
    double max = Double.POSITIVE_INFINITY;
    for(DBID candidateID : relation.iterDBIDs()) {
      final double doubleDistance = rawdist.doubleDistance(obj, relation.get(candidateID));
      if(doubleDistance <= max) {
        heap.add(new DoubleDistance(doubleDistance), candidateID);
        // Update cutoff
        if(heap.size() >= heap.getK()) {
          max = heap.getMaximumDistance().doubleValue();
        }
      }
    }
    return heap.toSortedArrayList();
  }

  @Override
  public List<List<DistanceResultPair<DoubleDistance>>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    @SuppressWarnings("unchecked")
    final RawDoubleDistance<O> rawdist = (RawDoubleDistance<O>) distanceQuery.getDistanceFunction();

    final List<KNNHeap<DoubleDistance>> heaps = new ArrayList<KNNHeap<DoubleDistance>>(ids.size());
    // TODO: this array can become quite large...
    final List<O> objs = new ArrayList<O>(ids.size());
    for(DBID id : ids) {
      heaps.add(new KNNHeap<DoubleDistance>(k));
      objs.add(relation.get(id));
    }
    final double[] max = new double[ids.size()];
    Arrays.fill(max, Double.POSITIVE_INFINITY);

    // The distance is computed on arbitrary vectors, we can reduce object
    // loading by working on the actual vectors.
    for(DBID candidateID : relation.iterDBIDs()) {
      O candidate = relation.get(candidateID);
      for(int index = 0; index < ids.size(); index++) {
        O object = objs.get(index);
        KNNHeap<DoubleDistance> heap = heaps.get(index);
        double distance = rawdist.doubleDistance(object, candidate);
        if(distance <= max[index]) {
          final DoubleDistance distobj = new DoubleDistance(distance);
          heap.add(distobj, candidateID);
          if(heap.size() >= heap.getK()) {
            max[index] = heap.getMaximumDistance().doubleValue();
          }
        }
      }
    }

    List<List<DistanceResultPair<DoubleDistance>>> result = new ArrayList<List<DistanceResultPair<DoubleDistance>>>(heaps.size());
    for(KNNHeap<DoubleDistance> heap : heaps) {
      result.add(heap.toSortedArrayList());
    }
    return result;
  }
}