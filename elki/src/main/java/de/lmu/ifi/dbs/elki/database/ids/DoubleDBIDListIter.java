package de.lmu.ifi.dbs.elki.database.ids;


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

/**
 * Iterator over double-DBID pairs results.
 *
 * There is no getter for the DBID, as this implements
 * {@link de.lmu.ifi.dbs.elki.database.ids.DBIDRef}.
 *
 * @author Erich Schubert
 * @since 0.3
 *
 * @apiviz.landmark
 *
 * @apiviz.has DoubleDBIDPair
 */
public interface DoubleDBIDListIter extends DBIDArrayIter {
  /**
   * Get the double value
   *
   * @return double value
   */
  double doubleValue();

  /**
   * Materialize an object pair.
   *
   * Note: currently, this will create a <em>new object</em>. In order to avoid
   * the garbage collection overhead, it is preferable to use
   * {@code #doubleValue()} and exploit that the iterator itself is a
   * {@code DBIDRef} reference.
   *
   * @return object pair
   */
  DoubleDBIDPair getPair();

  @Override
  DoubleDBIDListIter advance();

  @Override
  DoubleDBIDListIter advance(int count);

  @Override
  DoubleDBIDListIter retract();

  @Override
  DoubleDBIDListIter seek(int off);
}