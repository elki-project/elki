package de.lmu.ifi.dbs.elki.database.relation;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;

/**
 * Interface for double-valued relations.
 * 
 * @author Erich Schubert
 */
public interface DoubleRelation extends Relation<Double> {
  /**
   * Get the representation of an object.
   * 
   * @param id Object ID
   * @return object instance
   */
  public double doubleValue(DBIDRef id);

  /**
   * Set an object representation.
   * 
   * @param id Object ID
   * @param val Value
   */
  // TODO: remove / move to a writable API?
  public void set(DBIDRef id, double val);

  /**
   * @deprecated use {@link #doubleValue} instead.
   */
  @Deprecated
  @Override
  public Double get(DBIDRef id);

  /**
   * @deprecated use {@link #set(id, double)} instead.
   */
  @Deprecated
  @Override
  public void set(DBIDRef id, Double val);
}
