package de.lmu.ifi.dbs.elki.database.query.similarity;

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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Run a database query in a database context.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type.
 * @param <D> Distance result type.
 */
public abstract class AbstractDBIDSimilarityQuery<O, D extends Distance<D>> extends AbstractSimilarityQuery<O, D> {
  /**
   * Constructor.
   * 
   * @param relation Relation to use.
   */
  public AbstractDBIDSimilarityQuery(Relation<? extends O> relation) {
    super(relation);
  }

  @Override
  public D similarity(O o1, DBIDRef id2) {
    throw new UnsupportedOperationException("This distance function can only be used for objects when referenced by ID.");
  }

  @Override
  public D similarity(DBIDRef id1, O o2) {
    throw new UnsupportedOperationException("This distance function can only be used for objects when referenced by ID.");
  }

  @Override
  public D similarity(O o1, O o2) {
    throw new UnsupportedOperationException("This distance function can only be used for objects when referenced by ID.");
  }
}