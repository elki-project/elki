package de.lmu.ifi.dbs.elki.database.query.range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.RawDoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Default linear scan range query class.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses RawDoubleDistance
 * 
 * @param <O> Database object type
 */
public class LinearScanRawDoubleDistanceRangeQuery<O> extends LinearScanRangeQuery<O, DoubleDistance> implements LinearScanQuery {
  /**
   * Constructor.
   * 
   * @param relation Data to query
   * @param distanceQuery Distance function to use
   */
  public LinearScanRawDoubleDistanceRangeQuery(Relation<? extends O> relation, DistanceQuery<O, DoubleDistance> distanceQuery) {
    super(relation, distanceQuery);
  }

  @Override
  public List<DistanceResultPair<DoubleDistance>> getRangeForDBID(DBID id, DoubleDistance range) {
    if (distanceQuery instanceof PrimitiveDistanceQuery && distanceQuery.getDistanceFunction() instanceof RawDoubleDistance) {
      @SuppressWarnings("unchecked")
      RawDoubleDistance<O> rawdist = (RawDoubleDistance<O>) distanceQuery.getDistanceFunction();
      double epsilon = range.doubleValue();
      
      O qo = relation.get(id);
      List<DistanceResultPair<DoubleDistance>> result = new ArrayList<DistanceResultPair<DoubleDistance>>();
      for(DBID currentID : relation.iterDBIDs()) {
        double doubleDistance = rawdist.doubleDistance(qo, relation.get(currentID));
        if(doubleDistance < epsilon) {
          result.add(new DistanceResultPair<DoubleDistance>(new DoubleDistance(doubleDistance), currentID));
        }
      }
      Collections.sort(result);
      return result;
    } else {
      return super.getRangeForDBID(id, range);
    }
  }

  @Override
  public List<DistanceResultPair<DoubleDistance>> getRangeForObject(O obj, DoubleDistance range) {
    if (distanceQuery instanceof PrimitiveDistanceQuery && distanceQuery.getDistanceFunction() instanceof RawDoubleDistance) {
      @SuppressWarnings("unchecked")
      RawDoubleDistance<O> rawdist = (RawDoubleDistance<O>) distanceQuery.getDistanceFunction();
      double epsilon = range.doubleValue();
      
      List<DistanceResultPair<DoubleDistance>> result = new ArrayList<DistanceResultPair<DoubleDistance>>();
      for(DBID currentID : relation.iterDBIDs()) {
        double doubleDistance = rawdist.doubleDistance(obj, relation.get(currentID));
        if(doubleDistance < epsilon) {
          result.add(new DistanceResultPair<DoubleDistance>(new DoubleDistance(doubleDistance), currentID));
        }
      }
      Collections.sort(result);
      return result;
    } else {
      return super.getRangeForObject(obj, range);
    }
  }
}