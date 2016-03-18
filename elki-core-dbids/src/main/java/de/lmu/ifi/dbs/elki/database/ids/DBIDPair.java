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
 * Immutable pair of two DBIDs. This can be stored more efficiently than when
 * using {@link de.lmu.ifi.dbs.elki.utilities.pairs.Pair}
 *
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @apiviz.composedOf de.lmu.ifi.dbs.elki.database.ids.DBID
 */
public interface DBIDPair extends ArrayDBIDs {
  /**
   * Getter for first.
   *
   * @return first element in pair
   * @deprecated This method can be expensive. The use of a {@link DBIDVar} is
   *             recommended when many such accesses are needed.
   */
  @Deprecated
  public DBID getFirst();

  /**
   * Getter for second element in pair
   *
   * @return second element in pair
   * @deprecated This method can be expensive. The use of a {@link DBIDVar} is
   *             recommended when many such accesses are needed.
   */
  @Deprecated
  public DBID getSecond();
}