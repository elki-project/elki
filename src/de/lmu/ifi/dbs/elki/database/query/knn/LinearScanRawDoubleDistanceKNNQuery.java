package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.Arrays;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.DoubleDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;

/**
 * Optimized linear scan query for {@link PrimitiveDoubleDistanceFunction}s.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses PrimitiveDoubleDistanceFunction
 * 
 * @param <O> Object type
 */
public class LinearScanRawDoubleDistanceKNNQuery<O> extends LinearScanPrimitiveDistanceKNNQuery<O, DoubleDistance> {
  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   */
  public LinearScanRawDoubleDistanceKNNQuery(PrimitiveDistanceQuery<O, DoubleDistance> distanceQuery) {
    super(distanceQuery);
    if(!(distanceQuery.getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction)) {
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
    final PrimitiveDoubleDistanceFunction<O> rawdist = (PrimitiveDoubleDistanceFunction<O>) distanceQuery.getDistanceFunction();
    // Optimization for double distances.
    final KNNHeap<DoubleDistance> heap = new KNNHeap<DoubleDistance>(k);
    double max = Double.POSITIVE_INFINITY;
    for(DBID candidateID : relation.iterDBIDs()) {
      final double doubleDistance = rawdist.doubleDistance(obj, relation.get(candidateID));
      if(doubleDistance <= max) {
        heap.add(new DoubleDistanceResultPair(doubleDistance, candidateID));
        // Update cutoff
        if(heap.size() >= heap.getK()) {
          max = ((DoubleDistanceResultPair) heap.peek()).getDoubleDistance();
        }
      }
    }
    return heap.toSortedArrayList();
  }

  @Override
  protected void linearScanBatchKNN(List<O> objs, List<KNNHeap<DoubleDistance>> heaps) {
    final int size = objs.size();
    @SuppressWarnings("unchecked")
    final PrimitiveDoubleDistanceFunction<O> rawdist = (PrimitiveDoubleDistanceFunction<O>) distanceQuery.getDistanceFunction();
    // Track the max ourselves to reduce object access for comparisons.
    final double[] max = new double[size];
    Arrays.fill(max, Double.POSITIVE_INFINITY);

    // The distance is computed on arbitrary vectors, we can reduce object
    // loading by working on the actual vectors.
    for(DBID candidateID : relation.iterDBIDs()) {
      O candidate = relation.get(candidateID);
      for(int index = 0; index < size; index++) {
        final KNNHeap<DoubleDistance> heap = heaps.get(index);
        double doubleDistance = rawdist.doubleDistance(objs.get(index), candidate);
        if(doubleDistance <= max[index]) {
          heap.add(new DoubleDistanceResultPair(doubleDistance, candidateID));
          if(heap.size() >= heap.getK()) {
            max[index] = ((DoubleDistanceResultPair) heap.peek()).getDoubleDistance();
          }
        }
      }
    }
  }
}