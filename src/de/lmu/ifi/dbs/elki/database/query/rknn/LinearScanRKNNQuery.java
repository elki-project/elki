package de.lmu.ifi.dbs.elki.database.query.rknn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.query.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.database.query.DistanceDBIDResultIter;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Default linear scan RKNN query class.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has KNNQuery
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
// FIXME: Validate this works correctly.
public class LinearScanRKNNQuery<O, D extends Distance<D>> extends AbstractRKNNQuery<O, D> implements LinearScanQuery {
  /**
   * KNN query we use.
   */
  protected final KNNQuery<O, D> knnQuery;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   * @param knnQuery kNN query to use.
   * @param maxk k to use
   */
  public LinearScanRKNNQuery(DistanceQuery<O, D> distanceQuery, KNNQuery<O, D> knnQuery, Integer maxk) {
    super(distanceQuery);
    this.knnQuery = knnQuery;
  }

  @Override
  public DistanceDBIDResult<D> getRKNNForObject(O obj, int k) {
    GenericDistanceDBIDList<D> rNNlist = new GenericDistanceDBIDList<D>();

    ArrayDBIDs allIDs = DBIDUtil.ensureArray(relation.getDBIDs());
    List<? extends KNNResult<D>> kNNLists = knnQuery.getKNNForBulkDBIDs(allIDs, k);

    int i = 0;
    for (DBIDIter iter = allIDs.iter(); iter.valid(); iter.advance()) {
      KNNResult<D> knn = kNNLists.get(i);
      int last = Math.min(k - 1, knn.size() - 1);
      D dist = distanceQuery.distance(obj, iter);
      if(last < k - 1 || dist.compareTo(knn.get(last).getDistance()) < 1) {
        rNNlist.add(dist, iter);
      }
      i++;
    }
    rNNlist.sort();
    return rNNlist;
  }

  @Override
  public DistanceDBIDResult<D> getRKNNForDBID(DBIDRef id, int k) {
    GenericDistanceDBIDList<D> rNNList = new GenericDistanceDBIDList<D>();
    
    ArrayDBIDs allIDs = DBIDUtil.ensureArray(relation.getDBIDs());
    List<? extends KNNResult<D>> kNNList = knnQuery.getKNNForBulkDBIDs(allIDs, k);

    int i = 0;
    for (DBIDIter iter = allIDs.iter(); iter.valid(); iter.advance()) {
      KNNResult<D> knn = kNNList.get(i);
      for(DistanceDBIDResultIter<D> n = knn.iter(); n.valid(); n.advance()) {
        if(DBIDUtil.equal(n, id)) {
          rNNList.add(n.getDistance(), iter);
        }
      }
      i++;
    }
    rNNList.sort();
    return rNNList;
  }

  @Override
  public List<GenericDistanceDBIDList<D>> getRKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    List<GenericDistanceDBIDList<D>> rNNList = new ArrayList<GenericDistanceDBIDList<D>>(ids.size());
    for(int i = 0; i < ids.size(); i++) {
      rNNList.add(new GenericDistanceDBIDList<D>());
    }

    ArrayDBIDs allIDs = DBIDUtil.ensureArray(relation.getDBIDs());
    List<? extends KNNResult<D>> kNNList = knnQuery.getKNNForBulkDBIDs(allIDs, k);

    int i = 0;
    for (DBIDIter iter = allIDs.iter(); iter.valid(); iter.advance()) {
      DBID qid = DBIDUtil.deref(iter);
      KNNResult<D> knn = kNNList.get(i);
      for(DistanceDBIDResultIter<D> n = knn.iter(); n.valid(); n.advance()) {
        int j = 0;
        for (DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
          if(DBIDUtil.equal(n, iter2)) {
            GenericDistanceDBIDList<D> rNN = rNNList.get(j);
            rNN.add(n.getDistance(), qid);
          }
          j++;
        }
      }
      i++;
    }
    for(int j = 0; j < ids.size(); j++) {
      rNNList.get(j).sort();
    }
    return rNNList;
  }
}