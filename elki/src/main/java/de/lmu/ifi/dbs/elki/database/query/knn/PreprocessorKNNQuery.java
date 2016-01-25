package de.lmu.ifi.dbs.elki.database.query.knn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.AbstractMaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Instance for a particular database, invoking the preprocessor.
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @param <O> Data object type
 */
public class PreprocessorKNNQuery<O> implements KNNQuery<O> {
  /**
   * The data to use for this query
   */
  final protected Relation<? extends O> relation;

  /**
   * The last preprocessor result
   */
  final private AbstractMaterializeKNNPreprocessor<O> preprocessor;

  /**
   * Warn only once.
   */
  private boolean warned = false;

  /**
   * Constructor.
   *
   * @param relation Relation to query
   * @param preprocessor Preprocessor instance to use
   */
  public PreprocessorKNNQuery(Relation<O> relation, AbstractMaterializeKNNPreprocessor<O> preprocessor) {
    super();
    this.relation = relation;
    this.preprocessor = preprocessor;
  }

  @Override
  public KNNList getKNNForDBID(DBIDRef id, int k) {
    if(!warned && k > preprocessor.getK()) {
      LoggingUtil.warning("Requested more neighbors than preprocessed: requested " + k + " preprocessed " + preprocessor.getK(), new Throwable());
      warned = true;
    }
    if(k < preprocessor.getK()) {
      KNNList dr = preprocessor.get(id);
      int subk = k;
      DoubleDBIDListIter it = dr.iter();
      final double kdist = it.seek(subk - 1).doubleValue();
      for(it.advance(); it.valid() && kdist < it.doubleValue() && subk < k; it.advance()) {
        subk++;
      }
      return subk < dr.size() ? DBIDUtil.subList(dr, subk) : dr;
    }
    return preprocessor.get(id);
  }

  @Override
  public List<KNNList> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    if(!warned && k > preprocessor.getK()) {
      LoggingUtil.warning("Requested more neighbors than preprocessed: requested " + k + " preprocessed " + preprocessor.getK(), new Throwable());
      warned = true;
    }
    if(k < preprocessor.getK()) {
      List<KNNList> result = new ArrayList<>(ids.size());
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        KNNList dr = preprocessor.get(iter);
        int subk = k;
        DoubleDBIDListIter it = dr.iter();
        final double kdist = it.seek(subk - 1).doubleValue();
        for(it.advance(); it.valid() && kdist < it.doubleValue() && subk < k; it.advance()) {
          subk++;
        }
        result.add(subk < dr.size() ? DBIDUtil.subList(dr, subk) : dr);
      }
      return result;
    }
    List<KNNList> result = new ArrayList<>(ids.size());
    for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      result.add(preprocessor.get(iter));
    }
    return result;
  }

  @Override
  public KNNList getKNNForObject(O obj, int k) {
    throw new AbortException("Preprocessor KNN query only supports ID queries.");
  }

  /**
   * Get the preprocessor instance.
   *
   * @return preprocessor instance
   */
  public AbstractMaterializeKNNPreprocessor<O> getPreprocessor() {
    return preprocessor;
  }
}