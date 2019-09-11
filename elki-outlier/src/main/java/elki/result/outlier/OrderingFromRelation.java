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
package elki.result.outlier;

import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.relation.DoubleRelation;
import elki.database.relation.RelationUtil;
import elki.result.OrderingResult;

/**
 * Ordering obtained from an outlier score.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - - - DoubleRelation
 */
public class OrderingFromRelation implements OrderingResult {
  /**
   * Outlier scores.
   */
  protected DoubleRelation scores;

  /**
   * Factor for ascending (+1) and descending (-1) ordering.
   */
  protected boolean ascending = false;

  /**
   * Constructor for outlier orderings
   * 
   * @param scores outlier score result
   * @param ascending Ascending when {@code true}, descending otherwise
   */
  public OrderingFromRelation(DoubleRelation scores, boolean ascending) {
    super();
    this.scores = scores;
    this.ascending = ascending;
  }

  /**
   * Ascending constructor.
   * 
   * @param scores
   */
  public OrderingFromRelation(DoubleRelation scores) {
    this(scores, false);
  }

  @Override
  public DBIDs getDBIDs() {
    return scores.getDBIDs();
  }

  @Override
  public ArrayModifiableDBIDs order(DBIDs ids) {
    ArrayModifiableDBIDs sorted = DBIDUtil.newArray(ids);
    sorted.sort(ascending ? //
    new RelationUtil.AscendingByDoubleRelation(scores) //
    : new RelationUtil.DescendingByDoubleRelation(scores));
    return sorted;
  }

  // @Override // used to be in Result
  public String getLongName() {
    return scores.getLongName() + " Order";
  }
}
