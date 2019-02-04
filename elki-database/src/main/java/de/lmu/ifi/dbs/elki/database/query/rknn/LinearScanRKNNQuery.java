/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
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
package de.lmu.ifi.dbs.elki.database.query.rknn;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.LinearScanQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;

/**
 * Default linear scan RKNN query class.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - KNNQuery
 *
 * @param <O> Database object type
 */
// FIXME: Validate this works correctly.
public class LinearScanRKNNQuery<O> extends AbstractRKNNQuery<O> implements LinearScanQuery {
  /**
   * KNN query we use.
   */
  protected final KNNQuery<O> knnQuery;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   * @param knnQuery kNN query to use.
   * @param maxk k to use
   */
  public LinearScanRKNNQuery(DistanceQuery<O> distanceQuery, KNNQuery<O> knnQuery, Integer maxk) {
    super(distanceQuery);
    this.knnQuery = knnQuery;
  }

  @Override
  public DoubleDBIDList getRKNNForObject(O obj, int k) {
    ModifiableDoubleDBIDList rNNlist = DBIDUtil.newDistanceDBIDList();

    ArrayDBIDs allIDs = DBIDUtil.ensureArray(relation.getDBIDs());
    List<? extends KNNList> kNNLists = knnQuery.getKNNForBulkDBIDs(allIDs, k);

    for(DBIDArrayIter iter = allIDs.iter(); iter.valid(); iter.advance()) {
      KNNList knn = kNNLists.get(iter.getOffset());
      final double dist = distanceQuery.distance(obj, iter);
      final int last = Math.min(k - 1, knn.size() - 1);
      if(last < k - 1 || dist <= knn.doubleValue(last)) {
        rNNlist.add(dist, iter);
      }
    }
    rNNlist.sort();
    return rNNlist;
  }

  @Override
  public DoubleDBIDList getRKNNForDBID(DBIDRef id, int k) {
    ModifiableDoubleDBIDList rNNList = DBIDUtil.newDistanceDBIDList();

    ArrayDBIDs allIDs = DBIDUtil.ensureArray(relation.getDBIDs());
    List<? extends KNNList> kNNList = knnQuery.getKNNForBulkDBIDs(allIDs, k);

    int i = 0;
    for(DBIDIter iter = allIDs.iter(); iter.valid(); iter.advance()) {
      KNNList knn = kNNList.get(i);
      for(DoubleDBIDListIter n = knn.iter(); n.valid(); n.advance()) {
        if(DBIDUtil.equal(n, id)) {
          rNNList.add(n.doubleValue(), iter);
        }
      }
      i++;
    }
    rNNList.sort();
    return rNNList;
  }

  @Override
  public List<? extends DoubleDBIDList> getRKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    List<ModifiableDoubleDBIDList> rNNList = new ArrayList<>(ids.size());
    for(int i = 0; i < ids.size(); i++) {
      rNNList.add(DBIDUtil.newDistanceDBIDList());
    }

    ArrayDBIDs allIDs = DBIDUtil.ensureArray(relation.getDBIDs());
    List<? extends KNNList> kNNList = knnQuery.getKNNForBulkDBIDs(allIDs, k);

    int i = 0;
    for(DBIDIter iter = allIDs.iter(); iter.valid(); iter.advance()) {
      KNNList knn = kNNList.get(i);
      for(DoubleDBIDListIter n = knn.iter(); n.valid(); n.advance()) {
        int j = 0;
        for(DBIDIter iter2 = ids.iter(); iter2.valid(); iter2.advance()) {
          if(DBIDUtil.equal(n, iter2)) {
            ModifiableDoubleDBIDList rNN = rNNList.get(j);
            rNN.add(n.doubleValue(), iter);
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
