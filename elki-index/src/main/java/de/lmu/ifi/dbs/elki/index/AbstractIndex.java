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
package de.lmu.ifi.dbs.elki.index;

import de.lmu.ifi.dbs.elki.database.relation.Relation;

/**
 * Abstract base class for indexes with some implementation defaults.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 *
 * @param <O> Object type stored in the index
 */
public abstract class AbstractIndex<O> implements Index {
  /**
   * The representation we are bound to.
   */
  protected final Relation<O> relation;
  
  /**
   * Constructor.
   *
   * @param relation Relation indexed
   */
  public AbstractIndex(Relation<O> relation) {
    super();
    this.relation = relation;
  }

  @Override
  abstract public String getLongName();

  @Override
  abstract public String getShortName();
}