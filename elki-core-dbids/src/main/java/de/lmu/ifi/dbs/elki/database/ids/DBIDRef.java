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
 * Some object referencing a {@link DBID}. Could be a {@link DBID}, a
 * {@link DBIDIter}, for example.
 * <p>
 * Important note: <em>do not assume this reference to be stable</em>. Iterators
 * are a good example how the DBIDRef may change.
 *
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @opt nodefillcolor LemonChiffon
 * 
 * @navhas - references - DBID
 */
public interface DBIDRef {
  /**
   * <b>Internal only:</b> Get the internal index.
   * <p>
   * <b>NOT FOR PUBLIC USE - ELKI Optimization engine only.</b>
   *
   * @return Internal index
   */
  int internalGetIndex();

  /**
   * <b>WARNING:</b> Hash codes of this interface <b>might not be stable</b>
   * (e.g. for iterators).
   * <p>
   * Use {@link DBIDUtil#deref} to get an object with a stable hash code!
   *
   * @return current hash code (<b>may change!</b>)
   * @deprecated Do not use this hash code. Some implementations will not offer
   *             stable hash codes!
   */
  @Override
  @Deprecated
  int hashCode();

  /**
   * <b>WARNING:</b> calling equality on a reference may be an indicator of
   * incorrect usage, as it is not clear whether the programmer meant the
   * references to be the same or the DBIDs.
   * <p>
   * Use {@link DBIDUtil#equal} or {@link DBIDUtil#compare}!
   *
   * @param obj Object to compare with
   * @return True when they are the same object
   * @deprecated Use {@link DBIDUtil#equal} instead.
   */
  @Override
  @Deprecated
  boolean equals(Object obj);
}
