package de.lmu.ifi.dbs.elki.database.ids;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
 * Some object referencing a {@link DBID}. Could be a {@link DBID}, a
 * {@link DBIDIter}, for example.
 * 
 * Important note: <em>do not assume this reference to be stable</em>. Iterators
 * are a good example how the DBIDRef may change.
 * 
 * @author Erich Schubert
 */
public interface DBIDRef {
  /**
   * Get the referenced {@link DBID}.
   * 
   * Efficiency note: this may require materialization of a DBID object.
   * 
   * @return referenced DBID
   */
  public DBID getDBID();

  /**
   * Return the integer value of the object ID, if possible.
   * 
   * @deprecated Scheduled for removal.
   * 
   * @return integer id
   */
  @Deprecated
  public int getIntegerID();

  /**
   * WARNING: Hash codes of this interface <b>might not be stable</b> (e.g. for
   * iterators).
   * 
   * @return current hash code (<b>may change!</b>)
   * 
   * @deprecated Do not use this hash code. Some implementations will not offer
   *             stable hash codes!
   */
  @Override
  @Deprecated
  public int hashCode();

  /**
   * WARNING: calling equality on a reference may be an indicator of incorrect
   * usage, as it is not clear whether the programmer meant the references to be
   * the same or the DBIDs.
   * 
   * @param obj Object to compare with
   * @return True when they are the same object
   */
  @Override
  @Deprecated
  public boolean equals(Object obj);
}