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
package de.lmu.ifi.dbs.elki.algorithm.timeseries;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;

/**
 * Single Change Point
 *
 * @author Sebastian Rühl
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ChangePoint {
  /**
   * Data set reference.
   */
  DBID id;

  /**
   * Column id.
   */
  int column;

  /**
   * Score
   */
  double score;

  /**
   * Constructor.
   *
   * @param iter Data set reference
   * @param column Column
   * @param score Score
   */
  public ChangePoint(DBIDRef iter, int column, double score) {
    this.id = DBIDUtil.deref(iter);
    this.column = column;
    this.score = score;
  }

  /**
   * Append to a text buffer.
   * 
   * @param buf Text buffer
   * @return Text buffer
   */
  public StringBuilder appendTo(StringBuilder buf) {
    return buf.append(DBIDUtil.toString((DBIDRef) id)).append(" ").append(column).append(" ").append(score);
  }
}
