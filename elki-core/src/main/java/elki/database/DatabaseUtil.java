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
package elki.database;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import elki.data.ClassLabel;
import elki.data.LabelList;
import elki.data.type.NoSupportedDataTypeException;
import elki.data.type.TypeUtil;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.relation.ConvertToStringView;
import elki.database.relation.Relation;

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
  public static Relation<String> guessLabelRepresentation(Database database) {
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
  public static Relation<String> guessObjectLabelRepresentation(Database database) {
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
}
