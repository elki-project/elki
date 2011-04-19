package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;

/**
 * Factory for DeLiClu R*-Trees.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses DeLiCluTree oneway - - «create»
 * 
 * @param <O> Object type
 */
public class DeLiCluTreeFactory<O extends NumberVector<O, ?>> extends AbstractRStarTreeFactory<O, DeLiCluTree<O>> {
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
  public DeLiCluTreeFactory(String fileName, int pageSize, long cacheSize, boolean bulk, Strategy bulkLoadStrategy, int insertionCandidates) {
    super(fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates);
  }

  @Override
  public DeLiCluTree<O> instantiate(Relation<O> representation) {
    return new DeLiCluTree<O>(representation, fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates);
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
    protected DeLiCluTreeFactory<O> makeInstance() {
      return new DeLiCluTreeFactory<O>(fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates);
    }
  }
}