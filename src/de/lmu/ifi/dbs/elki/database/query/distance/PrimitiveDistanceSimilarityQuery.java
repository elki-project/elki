package de.lmu.ifi.dbs.elki.database.query.distance;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
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
   * @param rep Representation
   * @param distanceFunction distance function
   * @param similarityFunction similarity function (usually the same as the
   *        distance function!)
   */
  public PrimitiveDistanceSimilarityQuery(Relation<? extends O> rep, PrimitiveDistanceFunction<? super O, D> distanceFunction, PrimitiveSimilarityFunction<? super O, D> similarityFunction) {
    super(rep, distanceFunction);
    this.similarityFunction = similarityFunction;
  }

  @Override
  public D similarity(DBID id1, DBID id2) {
    O o1 = rep.get(id1);
    O o2 = rep.get(id2);
    return similarity(o1, o2);
  }

  @Override
  public D similarity(O o1, DBID id2) {
    O o2 = rep.get(id2);
    return similarity(o1, o2);
  }

  @Override
  public D similarity(DBID id1, O o2) {
    O o1 = rep.get(id1);
    return similarity(o1, o2);
  }

  @Override
  public D similarity(O o1, O o2) {
    return this.similarityFunction.similarity(o1, o2);
  }
}