package de.lmu.ifi.dbs.elki.database.query.knn;

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
import java.util.Map;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.AbstractDataBasedQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.AbstractMaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Instance for a particular database, invoking the preprocessor.
 * 
 * @author Erich Schubert
 */
public class PreprocessorKNNQuery<O, D extends Distance<D>, T extends KNNResult<D>> extends AbstractDataBasedQuery<O> implements KNNQuery<O, D> {
  /**
   * The last preprocessor result
   */
  final private AbstractMaterializeKNNPreprocessor<O, D, T> preprocessor;

  /**
   * Warn only once.
   */
  private boolean warned = false;

  /**
   * Constructor.
   * 
   * @param database Database to query
   * @param preprocessor Preprocessor instance to use
   */
  public PreprocessorKNNQuery(Relation<O> database, AbstractMaterializeKNNPreprocessor<O, D, T> preprocessor) {
    super(database);
    this.preprocessor = preprocessor;
  }

  /**
   * Constructor.
   * 
   * @param database Database to query
   * @param preprocessor Preprocessor to use
   */
  public PreprocessorKNNQuery(Relation<O> database, AbstractMaterializeKNNPreprocessor.Factory<O, D, T> preprocessor) {
    this(database, preprocessor.instantiate(database));
  }

  @Override
  public KNNResult<D> getKNNForDBID(DBIDRef id, int k) {
    if(!warned && k > preprocessor.getK()) {
      LoggingUtil.warning("Requested more neighbors than preprocessed!");
    }
    if(!warned && k < preprocessor.getK()) {
      KNNResult<D> dr = preprocessor.get(id);
      int subk = k;
      D kdist = dr.get(subk - 1).getDistance();
      while(subk < dr.size()) {
        D ndist = dr.get(subk).getDistance();
        if(kdist.equals(ndist)) {
          // Tie - increase subk.
          subk++;
        }
        else {
          break;
        }
      }
      if(subk < dr.size()) {
        return KNNUtil.subList(dr, subk);
      }
      else {
        return dr;
      }
    }
    return preprocessor.get(id);
  }

  @Override
  public List<KNNResult<D>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    if(!warned && k > preprocessor.getK()) {
      LoggingUtil.warning("Requested more neighbors than preprocessed!");
    }
    List<KNNResult<D>> result = new ArrayList<KNNResult<D>>(ids.size());
    if(k < preprocessor.getK()) {
      for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {      
        KNNResult<D> dr = preprocessor.get(iter.getDBID());
        int subk = k;
        D kdist = dr.get(subk - 1).getDistance();
        while(subk < dr.size()) {
          D ndist = dr.get(subk).getDistance();
          if(kdist.equals(ndist)) {
            // Tie - increase subk.
            subk++;
          }
          else {
            break;
          }
        }
        if(subk < dr.size()) {
          result.add(KNNUtil.subList(dr, subk));
        }
        else {
          result.add(dr);
        }
      }
    }
    else {
      for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {      
        result.add(preprocessor.get(iter.getDBID()));
      }
    }
    return result;
  }

  @Override
  public void getKNNForBulkHeaps(Map<DBID, KNNHeap<D>> heaps) {
    for(Entry<DBID, KNNHeap<D>> ent : heaps.entrySet()) {
      DBID id = ent.getKey();
      KNNHeap<D> heap = ent.getValue();
      for(DistanceResultPair<D> dr : preprocessor.get(id)) {
        heap.add(dr);
      }
    }
  }

  @Override
  public KNNResult<D> getKNNForObject(O obj, int k) {
    throw new AbortException("Preprocessor KNN query only supports ID queries.");
  }

  /**
   * Get the preprocessor instance.
   * 
   * @return preprocessor instance
   */
  public AbstractMaterializeKNNPreprocessor<O, D, T> getPreprocessor() {
    return preprocessor;
  }
}