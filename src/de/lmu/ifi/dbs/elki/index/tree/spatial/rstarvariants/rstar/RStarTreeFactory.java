package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Factory for regular R*-Trees.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark factory
 * @apiviz.uses RStarTreeIndex oneway - - «create»
 * 
 * @param <O> Object type
 */
public class RStarTreeFactory<O extends NumberVector<O, ?>> extends AbstractRStarTreeFactory<O, RStarTreeIndex<O>> {
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
  public RStarTreeIndex<O> instantiate(Relation<O> relation) {
    PageFile<RStarTreeNode> pagefile = makePageFile(getNodeClass());
    return new RStarTreeIndex<O>(relation, pagefile, bulk, bulkLoadStrategy, insertionCandidates);
  }

  protected Class<RStarTreeNode> getNodeClass() {
    return RStarTreeNode.class;
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