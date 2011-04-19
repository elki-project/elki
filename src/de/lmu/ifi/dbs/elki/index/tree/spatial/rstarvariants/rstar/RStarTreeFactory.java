package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;

/**
 * Factory for regular R*-Trees.
 * 
 * @author Erich Schubert
 *
 * @apiviz.landmark factory
 * @apiviz.uses RStarTree oneway - - «create»
 *
 * @param <O> Object type
 */
public class RStarTreeFactory<O extends NumberVector<O, ?>> extends AbstractRStarTreeFactory<O, RStarTree<O>> {
  /**
   * Constructor.
   *
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param bulk
   * @param bulkLoadStrategy
   * @param insertionCandidates
   */
  public RStarTreeFactory(String fileName, int pageSize, long cacheSize, boolean bulk, Strategy bulkLoadStrategy, int insertionCandidates) {
    super(fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates);
  }

  @Override
  public RStarTree<O> instantiate(Relation<O> representation) {
    return new RStarTree<O>(representation, fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates);
  }
  
  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O extends NumberVector<O, ?>> extends AbstractRStarTreeFactory.Parameterizer<O> {
    @Override
    protected RStarTreeFactory<O> makeInstance() {
      return new RStarTreeFactory<O>(fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates);
    }
  }
}