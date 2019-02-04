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
package de.lmu.ifi.dbs.elki.database;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.NoSupportedDataTypeException;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.PreprocessorKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.ConvertToStringView;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.index.distancematrix.PrecomputedDistanceMatrix;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Class with Database-related utility functions such as centroid computation,
 * covariances etc.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public final class DatabaseUtil {
  /**
   * Fake constructor: Do not instantiate!
   */
  private DatabaseUtil() {
    // Do not instantiate!
  }

  /**
   * Guess a potentially label-like representation, preferring class labels.
   * 
   * @param database
   * @return string representation
   */
  public static Relation<String> guessLabelRepresentation(Database database) throws NoSupportedDataTypeException {
    try {
      Relation<? extends ClassLabel> classrep = database.getRelation(TypeUtil.CLASSLABEL);
      if(classrep != null) {
        return new ConvertToStringView(classrep);
      }
    }
    catch(NoSupportedDataTypeException e) {
      // retry.
    }
    try {
      Relation<? extends LabelList> labelsrep = database.getRelation(TypeUtil.LABELLIST);
      if(labelsrep != null) {
        return new ConvertToStringView(labelsrep);
      }
    }
    catch(NoSupportedDataTypeException e) {
      // retry.
    }
    try {
      Relation<String> stringrep = database.getRelation(TypeUtil.STRING);
      if(stringrep != null) {
        return stringrep;
      }
    }
    catch(NoSupportedDataTypeException e) {
      // retry.
    }
    throw new NoSupportedDataTypeException("No label-like representation was found.");
  }

  /**
   * Guess a potentially object label-like representation.
   * 
   * @param database
   * @return string representation
   */
  public static Relation<String> guessObjectLabelRepresentation(Database database) throws NoSupportedDataTypeException {
    try {
      Relation<? extends LabelList> labelsrep = database.getRelation(TypeUtil.LABELLIST);
      if(labelsrep != null) {
        return new ConvertToStringView(labelsrep);
      }
    }
    catch(NoSupportedDataTypeException e) {
      // retry.
    }
    try {
      Relation<String> stringrep = database.getRelation(TypeUtil.STRING);
      if(stringrep != null) {
        return stringrep;
      }
    }
    catch(NoSupportedDataTypeException e) {
      // retry.
    }
    try {
      Relation<? extends ClassLabel> classrep = database.getRelation(TypeUtil.CLASSLABEL);
      if(classrep != null) {
        return new ConvertToStringView(classrep);
      }
    }
    catch(NoSupportedDataTypeException e) {
      // retry.
    }
    throw new NoSupportedDataTypeException("No label-like representation was found.");
  }

  /**
   * Retrieves all class labels within the database.
   * 
   * @param database the database to be scanned for class labels
   * @return a set comprising all class labels that are currently set in the
   *         database
   */
  public static SortedSet<ClassLabel> getClassLabels(Relation<? extends ClassLabel> database) {
    SortedSet<ClassLabel> labels = new TreeSet<>();
    for(DBIDIter it = database.iterDBIDs(); it.valid(); it.advance()) {
      labels.add(database.get(it));
    }
    return labels;
  }

  /**
   * Retrieves all class labels within the database.
   * 
   * @param database the database to be scanned for class labels
   * @return a set comprising all class labels that are currently set in the
   *         database
   */
  public static SortedSet<ClassLabel> getClassLabels(Database database) {
    final Relation<ClassLabel> relation = database.getRelation(TypeUtil.CLASSLABEL);
    return getClassLabels(relation);
  }

  /**
   * Find object by matching their labels.
   * 
   * @param database Database to search in
   * @param name_pattern Name to match against class or object label
   * @return found cluster or it throws an exception.
   */
  public static ArrayModifiableDBIDs getObjectsByLabelMatch(Database database, Pattern name_pattern) {
    Relation<String> relation = guessLabelRepresentation(database);
    if(name_pattern == null) {
      return DBIDUtil.newArray();
    }
    ArrayModifiableDBIDs ret = DBIDUtil.newArray();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      if(name_pattern.matcher(relation.get(iditer)).find()) {
        ret.add(iditer);
      }
    }
    return ret;
  }

  /**
   * Get (or create) a precomputed kNN query for the database.
   * 
   * @param database Database
   * @param relation Relation
   * @param dq Distance query
   * @param k required number of neighbors
   * @return KNNQuery for the given relation, that is precomputed.
   */
  public static <O> KNNQuery<O> precomputedKNNQuery(Database database, Relation<O> relation, DistanceQuery<O> dq, int k) {
    // "HEAVY" flag for knn query since it is used more than once
    KNNQuery<O> knnq = database.getKNNQuery(dq, k, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY);
    // No optimized kNN query - use a preprocessor!
    if(knnq instanceof PreprocessorKNNQuery) {
      return knnq;
    }
    MaterializeKNNPreprocessor<O> preproc = new MaterializeKNNPreprocessor<>(relation, dq.getDistanceFunction(), k);
    preproc.initialize();
    // TODO: attach weakly persistent to the relation?
    return preproc.getKNNQuery(dq, k);
  }

  /**
   * Get (or create) a precomputed kNN query for the database.
   * 
   * @param database Database
   * @param relation Relation
   * @param distf Distance function
   * @param k required number of neighbors
   * @return KNNQuery for the given relation, that is precomputed.
   */
  public static <O> KNNQuery<O> precomputedKNNQuery(Database database, Relation<O> relation, DistanceFunction<? super O> distf, int k) {
    DistanceQuery<O> dq = database.getDistanceQuery(relation, distf);
    // "HEAVY" flag for knn query since it is used more than once
    KNNQuery<O> knnq = database.getKNNQuery(dq, k, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY);
    // No optimized kNN query - use a preprocessor!
    if(knnq instanceof PreprocessorKNNQuery) {
      return knnq;
    }
    MaterializeKNNPreprocessor<O> preproc = new MaterializeKNNPreprocessor<>(relation, dq.getDistanceFunction(), k);
    preproc.initialize();
    // TODO: attach weakly persistent to the relation?
    return preproc.getKNNQuery(dq, k);
  }

  /**
   * Get (or create) a precomputed distance query for the database.
   * 
   * This will usually force computing a distance matrix, unless there already
   * is one.
   * 
   * @param database Database
   * @param relation Relation
   * @param distf Distance function
   * @param log Logger
   * @return KNNQuery for the given relation, that is precomputed.
   */
  public static <O> DistanceQuery<O> precomputedDistanceQuery(Database database, Relation<O> relation, DistanceFunction<? super O> distf, Logging log) {
    DistanceQuery<O> dq = database.getDistanceQuery(relation, distf, DatabaseQuery.HINT_HEAVY_USE, DatabaseQuery.HINT_OPTIMIZED_ONLY);
    if(dq == null) {
      DBIDs ids = relation.getDBIDs();
      if(ids instanceof DBIDRange) {
        log.verbose("Precomputing a distance matrix for acceleration.");
        PrecomputedDistanceMatrix<O> idx = new PrecomputedDistanceMatrix<O>(relation, (DBIDRange) ids, distf);
        idx.initialize();
        // TODO: attach weakly persistent to the relation?
        dq = idx.getDistanceQuery(distf);
      }
    }
    if(dq == null) {
      dq = database.getDistanceQuery(relation, distf, DatabaseQuery.HINT_HEAVY_USE);
      log.warning("We could not automatically use a distance matrix, expect a performance degradation.");
    }
    return dq;
  }
}
