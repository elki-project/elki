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
package de.lmu.ifi.dbs.elki.database.ids;

/**
 * Iterator over double-DBID pairs results.
 * <p>
 * There is no getter for the DBID, as this implements
 * {@link de.lmu.ifi.dbs.elki.database.ids.DBIDRef}.
 *
 * @author Erich Schubert
 * @since 0.5.5
 *
 * @opt nodefillcolor LemonChiffon
 *
 * @has - - - DoubleDBIDPair
 */
public interface DoubleDBIDListIter extends DBIDArrayIter {
  /**
   * Get the double value
   *
   * @return double value
   */
  double doubleValue();

  @Override
  DoubleDBIDListIter advance();

  @Override
  DoubleDBIDListIter advance(int count);

  @Override
  DoubleDBIDListIter retract();

  @Override
  DoubleDBIDListIter seek(int off);
}