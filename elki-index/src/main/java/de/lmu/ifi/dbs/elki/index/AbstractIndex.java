package de.lmu.ifi.dbs.elki.index;

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

import de.lmu.ifi.dbs.elki.database.relation.Relation;

/**
 * Abstract base class for indexes with some implementation defaults.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @apiviz.excludeSubtypes
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