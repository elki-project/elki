package de.lmu.ifi.dbs.elki.database.query.rknn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Default linear scan RKNN query class.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public class LinearScanRKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractRKNNQuery<O, D> {
  /**
   * Constructor.
   * 
   * @param database Database to query
   * @param distanceQuery Distance function to use
   */
  public LinearScanRKNNQuery(Database<? extends O> database, DistanceQuery<O, D> distanceQuery) {
    super(database, distanceQuery);
  }

  @Override
  public List<DistanceResultPair<D>> getRKNNForObject(O obj, int k) {
    ArrayList<DistanceResultPair<D>> rNNlist = new ArrayList<DistanceResultPair<D>>();
    
    // TODO: verify.
    ArrayDBIDs allIDs = DBIDUtil.ensureArray(database.getIDs());
    // FIXME: resolve this cast.
    KNNQuery<O, D> knnQuery = ((Database<O>)database).getKNNQuery(distanceQuery, k, DatabaseQuery.HINT_BULK);
    List<List<DistanceResultPair<D>>> kNNLists = knnQuery.getKNNForBulkDBIDs(allIDs, k);

    int i = 0;
    for(DBID qid : allIDs) {
      List<DistanceResultPair<D>> knn = kNNLists.get(i);
      int last = Math.min(k - 1, knn.size() - 1);
      D dist = distanceQuery.distance(obj, qid);
      if (last < k - 1 || dist.compareTo(knn.get(last).getDistance()) < 1) {
        rNNlist.add(new DistanceResultPair<D>(dist, qid));
      }
      i++;
    }
    Collections.sort(rNNlist);
    return rNNlist;
  }

  @Override
  public List<DistanceResultPair<D>> getRKNNForDBID(DBID id, int k) {
    return getRKNNForBulkDBIDs(id, k).get(0);
  }

  @Override
  public List<List<DistanceResultPair<D>>> getRKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    List<List<DistanceResultPair<D>>> rNNList = new ArrayList<List<DistanceResultPair<D>>>(ids.size());
    for(int i = 0; i < ids.size(); i++) {
      rNNList.add(new ArrayList<DistanceResultPair<D>>());
    }

    ArrayDBIDs allIDs = DBIDUtil.ensureArray(database.getIDs());
    // FIXME: resolve this cast.
    KNNQuery<O, D> knnQuery = ((Database<O>)database).getKNNQuery(distanceQuery, k, DatabaseQuery.HINT_BULK);
    List<List<DistanceResultPair<D>>> kNNList = knnQuery.getKNNForBulkDBIDs(allIDs, k);

    int i = 0;
    for(DBID qid : allIDs) {
      List<DistanceResultPair<D>> knn = kNNList.get(i);
      for(DistanceResultPair<D> n : knn) {
        int j = 0;
        for(DBID id : ids) {
          if(n.getID() == id) {
            List<DistanceResultPair<D>> rNN = rNNList.get(j);
            rNN.add(new DistanceResultPair<D>(n.getDistance(), qid));
          }
          j++;
        }
      }
      i++;
    }
    for(int j = 0; j < ids.size(); j++) {
      List<DistanceResultPair<D>> rNN = rNNList.get(j);
      Collections.sort(rNN);
    }
    return rNNList;
  }
}