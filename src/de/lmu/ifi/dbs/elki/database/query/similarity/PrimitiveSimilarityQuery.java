package de.lmu.ifi.dbs.elki.database.query.similarity;
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
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.PrimitiveSimilarityFunction;

/**
 * Run a database query in a database context.
 * 
 * @author Erich Schubert
 *
 * @param <O> Database object type.
 * @param <D> Distance result type.
 */
public class PrimitiveSimilarityQuery<O, D extends Distance<D>> extends AbstractSimilarityQuery<O, D> {
  /**
   * The distance function we use.
   */
  final protected PrimitiveSimilarityFunction<? super O, D> similarityFunction;
  
  /**
   * Constructor.
   * 
   * @param relation Relation to use.
   * @param similarityFunction Our similarity function
   */
  public PrimitiveSimilarityQuery(Relation<? extends O> relation, PrimitiveSimilarityFunction<? super O, D> similarityFunction) {
    super(relation);
    this.similarityFunction = similarityFunction;
  }

  @Override
  public D similarity(DBID id1, DBID id2) {
    O o1 = relation.get(id1);
    O o2 = relation.get(id2);
    return similarity(o1, o2);
  }

  @Override
  public D similarity(O o1, DBID id2) {
    O o2 = relation.get(id2);
    return similarity(o1, o2);
  }

  @Override
  public D similarity(DBID id1, O o2) {
    O o1 = relation.get(id1);
    return similarity(o1, o2);
  }

  @Override
  public D similarity(O o1, O o2) {
    if (o1 == null) {
      throw new UnsupportedOperationException("This distance function can only be used for object instances.");
    }
    if (o2 == null) {
      throw new UnsupportedOperationException("This distance function can only be used for object instances.");
    }
    return similarityFunction.similarity(o1, o2);
  }

  @Override
  public D getDistanceFactory() {
    return similarityFunction.getDistanceFactory();
  }
}