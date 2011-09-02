package de.lmu.ifi.dbs.elki.database.query;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
 * Abstract query bound to a certain representation.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 */
public abstract class AbstractDataBasedQuery<O> implements DatabaseQuery {
  /**
   * The data to use for this query
   */
  final protected Relation<? extends O> relation;

  /**
   * Database this query works on.
   * 
   * @param relation Representation
   */
  public AbstractDataBasedQuery(Relation<? extends O> relation) {
    super();
    this.relation = relation;
  }

  /**
   * Give access to the underlying data query.
   * 
   * @return data query instance
   */
  public Relation<? extends O> getRelation() {
    return relation;
  }
}