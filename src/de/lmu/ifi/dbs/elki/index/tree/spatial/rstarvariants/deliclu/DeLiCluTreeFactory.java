package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk.BulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.util.InsertionStrategy;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Factory for DeLiClu R*-Trees.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses DeLiCluTreeIndex oneway - - «create»
 * 
 * @param <O> Object type
 */
public class DeLiCluTreeFactory<O extends NumberVector<O, ?>> extends AbstractRStarTreeFactory<O, DeLiCluTreeIndex<O>> {
  /**
   * Constructor.
   * 
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param bulkSplitter Bulk loading strategy
   * @param insertionStrategy the strategy to find the insertion child
   */
  public DeLiCluTreeFactory(String fileName, int pageSize, long cacheSize, BulkSplit bulkSplitter, InsertionStrategy insertionStrategy) {
    super(fileName, pageSize, cacheSize, bulkSplitter, insertionStrategy);
  }

  @Override
  public DeLiCluTreeIndex<O> instantiate(Relation<O> relation) {
    PageFile<DeLiCluNode> pagefile = makePageFile(getNodeClass());
    return new DeLiCluTreeIndex<O>(relation, pagefile, bulkSplitter, insertionStrategy);
  }

  protected Class<DeLiCluNode> getNodeClass() {
    return DeLiCluNode.class;
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
      return new DeLiCluTreeFactory<O>(fileName, pageSize, cacheSize, bulkSplitter, insertionStrategy);
    }
  }
}