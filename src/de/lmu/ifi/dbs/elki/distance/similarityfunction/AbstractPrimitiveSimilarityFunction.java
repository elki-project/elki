package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.query.similarity.PrimitiveSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Base implementation of a similarity function.
 * 
 * @author Arthur Zimek
 * 
 * @param <O> object type
 * @param <D> distance type
 */
public abstract class AbstractPrimitiveSimilarityFunction<O, D extends Distance<D>> implements PrimitiveSimilarityFunction<O, D> {
  /**
   * Constructor.
   */
  protected AbstractPrimitiveSimilarityFunction() {
    super();
  }

  @Override
  public boolean isSymmetric() {
    // Assume symmetric by default!
    return true;
  }

  @Override
  abstract public SimpleTypeInformation<? super O> getInputTypeRestriction();

  @Override
  abstract public D similarity(O o1, O o2);

  @Override
  public <T extends O> SimilarityQuery<T, D> instantiate(Relation<T> relation) {
    return new PrimitiveSimilarityQuery<T, D>(relation, this);
  }
}