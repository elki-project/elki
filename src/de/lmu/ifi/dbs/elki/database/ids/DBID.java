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
 * Database ID object.
 * 
 * While this currently is just an Integer, it should be avoided to store the
 * object IDs in regular integers to reduce problems if this API ever changes
 * (for example if someone needs to do context tracking for debug purposes!)
 * 
 * In particular, a developer should not make any assumption of these IDs being
 * consistent across multiple results/databases.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 */
public interface DBID extends DBIDRef, Comparable<DBIDRef>, ArrayDBIDs {
  /**
   * In contrast to {@link DBIDRef}, the DBID interface is supposed to have a
   * stable hash code. However, it is generally preferred to use optimized
   * storage classes instead of Java collections!
   * 
   * @return hash code
   */
  @Override
  public int hashCode();

  /**
   * In contrast to {@link DBIDRef}, the DBID interface is supposed to have a
   * stable equals for other DBIDs.
   * 
   * Yet, {@link #sameDBID} is more type safe and explicit.
   * 
   * @return true when the object is the same DBID.
   */
  @Override
  public boolean equals(Object obj);

  /**
   * Part of the DBIDRef API, this <em>must</em> return {@code this} for an
   * actual DBID.
   * 
   * @return {@code this}
   * @deprecated When the object is known to be a DBID, the usage of this method
   *             is pointless, therefore it is marked as deprecated to cause a
   *             warning.
   */
  @Deprecated
  @Override
  public DBID getDBID();

  /**
   * Compare two DBIDs for ordering.
   * 
   * Consider using {@link #compareDBID}, which is more explicit.
   * 
   * @param other Other DBID object
   * @return Comparison result
   */
  @Override
  public int compareTo(DBIDRef other);
}