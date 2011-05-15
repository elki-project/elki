package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.DoubleDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveNumberDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;

/**
 * Optimized linear scan query for {@link PrimitiveNumberDistanceFunction}s.
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
    if(!(distanceQuery.getDistanceFunction() instanceof PrimitiveNumberDistanceFunction)) {
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
    final PrimitiveNumberDistanceFunction<O, DoubleDistance> rawdist = (PrimitiveNumberDistanceFunction<O, DoubleDistance>) distanceQuery.getDistanceFunction();
    // Optimization for double distances.
    final KNNHeap<DoubleDistance> heap = new KNNHeap<DoubleDistance>(k);
    double max = Double.POSITIVE_INFINITY;
    for(DBID candidateID : relation.iterDBIDs()) {
      final double doubleDistance = rawdist.doubleDistance(obj, relation.get(candidateID));
      if(doubleDistance <= max) {
        heap.add(new DoubleDistanceResultPair(doubleDistance, candidateID));
        // Update cutoff
        if(heap.size() >= heap.getK()) {
          max = ((DoubleDistanceResultPair)heap.peek()).getDoubleDistance();
        }
      }
    }
    return heap.toSortedArrayList();
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<List<DistanceResultPair<DoubleDistance>>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    // We have a couple of casts in this implementation to avoid generics hacks.
    final PrimitiveNumberDistanceFunction<O, DoubleDistance> rawdist = (PrimitiveNumberDistanceFunction<O, DoubleDistance>) distanceQuery.getDistanceFunction();
    final int size = ids.size();
    final Object[] heaps = new Object[size];
    // TODO: this array can become quite large - save it?
    final Object[] objs = new Object[size];
    Iterator<DBID> iditer = ids.iterator();
    for(int i = 0; i < size; i++) {
      DBID id = iditer.next();
      heaps[i] = new KNNHeap<DoubleDistance>(k);
      objs[i] = relation.get(id);
    }
    final double[] max = new double[ids.size()];
    Arrays.fill(max, Double.POSITIVE_INFINITY);

    // The distance is computed on arbitrary vectors, we can reduce object
    // loading by working on the actual vectors.
    for(DBID candidateID : relation.iterDBIDs()) {
      O candidate = relation.get(candidateID);
      for(int index = 0; index < ids.size(); index++) {
        final KNNHeap<DoubleDistance> heap = (KNNHeap<DoubleDistance>) heaps[index];
        double doubleDistance = rawdist.doubleDistance((O) objs[index], candidate);
        if(doubleDistance <= max[index]) {
          heap.add(new DoubleDistanceResultPair(doubleDistance, candidateID));
          if(heap.size() >= heap.getK()) {
            max[index] = ((DoubleDistanceResultPair)heap.peek()).getDoubleDistance();
          }
        }
      }
    }

    List<List<DistanceResultPair<DoubleDistance>>> result = new ArrayList<List<DistanceResultPair<DoubleDistance>>>(size);
    for(int i = 0; i < size; i++) {
      result.add(((KNNHeap<DoubleDistance>)heaps[i]).toSortedArrayList());
    }
    return result;
  }
}