package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.deliclu;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk.BulkSplit;
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
   * @param insertionCandidates
   */
  public DeLiCluTreeFactory(String fileName, int pageSize, long cacheSize, BulkSplit bulkSplitter, int insertionCandidates) {
    super(fileName, pageSize, cacheSize, bulkSplitter, insertionCandidates);
  }

  @Override
  public DeLiCluTreeIndex<O> instantiate(Relation<O> relation) {
    PageFile<DeLiCluNode> pagefile = makePageFile(getNodeClass());
    return new DeLiCluTreeIndex<O>(relation, pagefile, bulkSplitter, insertionCandidates);
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
      return new DeLiCluTreeFactory<O>(fileName, pageSize, cacheSize, bulkSplitter, insertionCandidates);
    }
  }
}