package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
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
 * @apiviz.has DistanceQuery
 */
public class LinearScanDoubleDistanceKNNQuery<O> extends LinearScanKNNQuery<O, DoubleDistance> implements LinearScanQuery {
  /**
   * Constructor.
   * 
   * @param relation Data to query
   * @param distanceQuery Distance function to use
   */
  public LinearScanDoubleDistanceKNNQuery(Relation<? extends O> relation, DistanceQuery<O, DoubleDistance> distanceQuery) {
    super(relation, distanceQuery);
  }

  @Override
  public List<DistanceResultPair<DoubleDistance>> getKNNForDBID(DBID id, int k) {
    if(PrimitiveDistanceQuery.class.isInstance(distanceQuery) && RawDoubleDistance.class.isInstance(distanceQuery.getDistanceFunction())) {
      // Optimization for double distances.
      KNNHeap<DoubleDistance> heap = new KNNHeap<DoubleDistance>(k);
      O obj = relation.get(id);
      double max = Double.POSITIVE_INFINITY;
      @SuppressWarnings("unchecked")
      RawDoubleDistance<O> rawdist = (RawDoubleDistance<O>) distanceQuery.getDistanceFunction();
      for(DBID candidateID : relation.iterDBIDs()) {
        O oobj = relation.get(candidateID);
        double distance = rawdist.doubleDistance(obj, oobj);
        if(distance <= max) {
          final DoubleDistance distobj = new DoubleDistance(distance);
          heap.add(distobj, candidateID);
          if(heap.size() >= heap.getK()) {
            max = heap.getMaximumDistance().doubleValue();
          }
        }
      }
      return heap.toSortedArrayList();
    }
    else {
      // Fall back to regular linear scan.
      return super.getKNNForDBID(id, k);
    }
  }

  @Override
  public List<DistanceResultPair<DoubleDistance>> getKNNForObject(O obj, int k) {
    if(PrimitiveDistanceQuery.class.isInstance(distanceQuery) && RawDoubleDistance.class.isInstance(distanceQuery.getDistanceFunction())) {
      // Optimization for double distances.
      KNNHeap<DoubleDistance> heap = new KNNHeap<DoubleDistance>(k);
      double max = Double.POSITIVE_INFINITY;
      @SuppressWarnings("unchecked")
      RawDoubleDistance<O> rawdist = (RawDoubleDistance<O>) distanceQuery.getDistanceFunction();
      for(DBID candidateID : relation.iterDBIDs()) {
        O oobj = relation.get(candidateID);
        double distance = rawdist.doubleDistance(obj, oobj);
        if(distance <= max) {
          final DoubleDistance distobj = new DoubleDistance(distance);
          heap.add(distobj, candidateID);
          if(heap.size() >= heap.getK()) {
            max = heap.getMaximumDistance().doubleValue();
          }
        }
      }
      return heap.toSortedArrayList();
    }
    else {
      // Fall back to regular linear scan.
      return super.getKNNForObject(obj, k);
    }
  }
}