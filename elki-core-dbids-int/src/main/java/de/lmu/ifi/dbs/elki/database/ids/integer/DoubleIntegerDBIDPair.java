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
package de.lmu.ifi.dbs.elki.database.ids.integer;

import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;

/**
 * Pair containing a double value and an integer DBID.
 *
 * @author Erich Schubert
 * @since 0.5.5
 */
class DoubleIntegerDBIDPair implements DoubleDBIDPair, IntegerDBIDRef {
  /**
   * The double value.
   */
  double value;

  /**
   * The DB id.
   */
  int id;

  /**
   * Constructor.
   *
   * @param value Double value
   * @param id DBID
   */
  protected DoubleIntegerDBIDPair(double value, int id) {
    super();
    this.value = value;
    this.id = id;
  }

  @Override
  public int internalGetIndex() {
    return id;
  }

  @Override
  public int compareTo(DoubleDBIDPair o) {
    return Double.compare(value, o.doubleValue());
  }

  @Override
  public double doubleValue() {
    return value;
  }

  @Override
  public String toString() {
    return value + ":" + id;
  }
}
