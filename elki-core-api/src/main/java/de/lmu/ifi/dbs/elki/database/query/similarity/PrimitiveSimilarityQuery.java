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
package de.lmu.ifi.dbs.elki.database.query.similarity;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.PrimitiveSimilarityFunction;

/**
 * Run a database query in a database context.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @has - - - PrimitiveSimilarityFunction
 * 
 * @param <O> Database object type.
 */
public class PrimitiveSimilarityQuery<O> implements SimilarityQuery<O> {
  /**
   * The data to use for this query
   */
  final protected Relation<? extends O> relation;

  /**
   * The distance function we use.
   */
  final protected PrimitiveSimilarityFunction<? super O> similarityFunction;

  /**
   * Constructor.
   * 
   * @param relation Relation to use.
   * @param similarityFunction Our similarity function
   */
  public PrimitiveSimilarityQuery(Relation<? extends O> relation, PrimitiveSimilarityFunction<? super O> similarityFunction) {
    super();
    this.relation = relation;
    this.similarityFunction = similarityFunction;
  }

  @Override
  public final double similarity(DBIDRef id1, DBIDRef id2) {
    return similarity(relation.get(id1), relation.get(id2));
  }

  @Override
  public final double similarity(O o1, DBIDRef id2) {
    return similarity(o1, relation.get(id2));
  }

  @Override
  public final double similarity(DBIDRef id1, O o2) {
    return similarity(relation.get(id1), o2);
  }

  @Override
  public double similarity(O o1, O o2) {
    return similarityFunction.similarity(o1, o2);
  }

  @Override
  public Relation<? extends O> getRelation() {
    return relation;
  }

  @Override
  public PrimitiveSimilarityFunction<? super O> getSimilarityFunction() {
    return similarityFunction;
  }
}
