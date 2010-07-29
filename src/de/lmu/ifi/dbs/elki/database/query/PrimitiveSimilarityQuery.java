package de.lmu.ifi.dbs.elki.database.query;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
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
public class PrimitiveSimilarityQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractSimilarityQuery<O, D> {
  /**
   * The distance function we use.
   */
  final protected PrimitiveSimilarityFunction<? super O, D> similarityFunction;
  
  /**
   * Constructor.
   * 
   * @param database Database to use.
   * @param similarityFunction Our similarity function
   */
  public PrimitiveSimilarityQuery(Database<? extends O> database, PrimitiveSimilarityFunction<? super O, D> similarityFunction) {
    super(database);
    this.similarityFunction = similarityFunction;
  }

  @Override
  public D similarity(DBID id1, DBID id2) {
    O o1 = database.get(id1);
    O o2 = database.get(id2);
    return similarity(o1, o2);
  }

  @Override
  public D similarity(O o1, DBID id2) {
    O o2 = database.get(id2);
    return similarity(o1, o2);
  }

  @Override
  public D similarity(DBID id1, O o2) {
    O o1 = database.get(id1);
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