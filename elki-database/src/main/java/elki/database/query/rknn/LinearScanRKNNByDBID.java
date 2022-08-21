/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.database.query.rknn;

import elki.database.ids.*;
import elki.database.query.LinearScanQuery;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.knn.KNNSearcher;

/**
 * Default linear scan RKNN query class.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @has - - - KNNSearcher
 *
 * @param <O> relation object type
 */
public class LinearScanRKNNByDBID<O> implements RKNNSearcher<DBIDRef>, LinearScanQuery {
  /**
   * Hold the distance function to be used.
   */
  private DistanceQuery<O> distanceQuery;

  /**
   * KNN query we use.
   */
  private KNNSearcher<DBIDRef> knnQuery;

  /**
   * Constructor.
   * 
   * @param distanceQuery Distance function to use
   * @param knnQuery kNN query to use.
   */
  public LinearScanRKNNByDBID(DistanceQuery<O> distanceQuery, KNNSearcher<DBIDRef> knnQuery) {
    super();
    this.distanceQuery = distanceQuery;
    this.knnQuery = knnQuery;
  }

  @Override
  public DoubleDBIDList getRKNN(DBIDRef id, int k) {
    ModifiableDoubleDBIDList rNNList = DBIDUtil.newDistanceDBIDList();
    for(DBIDIter iter = distanceQuery.getRelation().iterDBIDs(); iter.valid(); iter.advance()) {
      KNNList knn = knnQuery.getKNN(iter, k);
      for(DoubleDBIDListIter n = knn.iter(); n.valid(); n.advance()) {
        if(DBIDUtil.equal(n, id)) {
          rNNList.add(n.doubleValue(), iter);
        }
      }
    }
    return rNNList.sort();
  }
}
