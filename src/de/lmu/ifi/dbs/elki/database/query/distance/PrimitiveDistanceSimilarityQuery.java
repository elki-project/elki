package de.lmu.ifi.dbs.elki.database.query.distance;

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
import de.lmu.ifi.dbs.elki.database.query.DistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.PrimitiveSimilarityFunction;

/**
 * Combination query class, for convenience.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.distance.similarityfunction.PrimitiveSimilarityFunction
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class PrimitiveDistanceSimilarityQuery<O, D extends Distance<D>> extends PrimitiveDistanceQuery<O, D> implements DistanceSimilarityQuery<O, D> {
  /**
   * Typed reference to the similarity function (usually the same as the
   * distance function!)
   */
  private PrimitiveSimilarityFunction<? super O, D> similarityFunction;

  /**
   * Constructor.
   * 
   * @param relation Representation
   * @param distanceFunction distance function
   * @param similarityFunction similarity function (usually the same as the
   *        distance function!)
   */
  public PrimitiveDistanceSimilarityQuery(Relation<? extends O> relation, PrimitiveDistanceFunction<? super O, D> distanceFunction, PrimitiveSimilarityFunction<? super O, D> similarityFunction) {
    super(relation, distanceFunction);
    this.similarityFunction = similarityFunction;
  }

  @Override
  public D similarity(DBIDRef id1, DBIDRef id2) {
    O o1 = relation.get(id1);
    O o2 = relation.get(id2);
    return similarity(o1, o2);
  }

  @Override
  public D similarity(O o1, DBIDRef id2) {
    O o2 = relation.get(id2);
    return similarity(o1, o2);
  }

  @Override
  public D similarity(DBIDRef id1, O o2) {
    O o1 = relation.get(id1);
    return similarity(o1, o2);
  }

  @Override
  public D similarity(O o1, O o2) {
    return this.similarityFunction.similarity(o1, o2);
  }
}