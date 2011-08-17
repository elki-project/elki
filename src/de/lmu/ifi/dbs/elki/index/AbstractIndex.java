package de.lmu.ifi.dbs.elki.index;
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

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.persistent.PageFileStatistics;

/**
 * Abstract base class for indexes with some implementation defaults.
 * 
 * @author Erich Schubert
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
  
  @Override
  public PageFileStatistics getPageFileStatistics() {
    // TODO: move this into a separate interface?
    // By default, we are not file based - no statistics available
    return null;
  }

  @SuppressWarnings("unused")
  @Override
  public void insert(DBID id) {
    throw new UnsupportedOperationException("This index does not allow dynamic updates.");
  }

  @SuppressWarnings("unused")
  @Override
  public void insertAll(DBIDs ids) {
    throw new UnsupportedOperationException("This index does not allow dynamic updates.");
  }

  @SuppressWarnings("unused")
  @Override
  public boolean delete(DBID id) {
    throw new UnsupportedOperationException("This index does not allow dynamic updates.");
  }

  @SuppressWarnings("unused")
  @Override
  public void deleteAll(DBIDs id) {
    throw new UnsupportedOperationException("This index does not allow dynamic updates.");
  }
}