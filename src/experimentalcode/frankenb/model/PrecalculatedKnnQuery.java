/**
 * 
 */
package experimentalcode.frankenb.model;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.log.Log;

public class PrecalculatedKnnQuery<O> implements KNNQuery<O, DoubleDistance> {

  private final DynamicBPlusTree<Integer, DistanceList> resultTree;

  protected PrecalculatedKnnQuery(DynamicBPlusTree<Integer, DistanceList> resultTree) {
    this.resultTree = resultTree;
  }

  private int requested = 0;
  @Override
  public List<DistanceResultPair<DoubleDistance>> getKNNForDBID(DBID id, int k) {
    try {
      DistanceList distanceList = this.resultTree.get(id.getIntegerID());
      List<DistanceResultPair<DoubleDistance>> list = new ArrayList<DistanceResultPair<DoubleDistance>>();
      if (distanceList == null) {
        throw new RuntimeException("This seems not to be the precalculated result for the given database as the id " + id.getIntegerID() + " is not contained in the precalculated results");
      }
      
      if (k > distanceList.getK()) throw new RuntimeException(String.format("Requested k(%d) exceeds the precalculated k(%d)", k, distanceList.getK()));
      
      if (k < distanceList.getK()) {
        DistanceList newDistanceList = new DistanceList(distanceList.getId(), k);
        newDistanceList.addAll(distanceList);
        distanceList = newDistanceList;
      }
      for (Pair<Integer, Double> distance : distanceList) {
        list.add(new DistanceResultPair<DoubleDistance>(new DoubleDistance(distance.second), DBIDUtil.importInteger(distance.first)));
      }
      
      if (++requested % 100000 == 0) {
        Log.debug(String.format("%d distances requested from index", requested));
      }
      
      return list;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }    
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery#getKNNForBulkDBIDs(de.lmu
   * .ifi.dbs.elki.database.ids.ArrayDBIDs, int)
   */
  @Override
  public List<List<DistanceResultPair<DoubleDistance>>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    List<List<DistanceResultPair<DoubleDistance>>> results = new ArrayList<List<DistanceResultPair<DoubleDistance>>>();
    for (DBID dbid : ids) {
      results.add(getKNNForDBID(dbid, k));
    }
    return results;
  }

  @Override
  public List<DistanceResultPair<DoubleDistance>> getKNNForObject(O obj, int k) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DistanceQuery<O, DoubleDistance> getDistanceQuery() {
    return null;
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return null;
  }

  @Override
  public Relation<? extends O> getRepresentation() {
    return dataBase;
  }
}