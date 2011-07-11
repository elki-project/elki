package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.flat;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk.BulkSplit;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Factory for flat R*-Trees.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.stereotype factory
 * @apiviz.uses FlatRStarTreeIndex oneway - - «create»
 * 
 * @param <O> Object type
 */
public class FlatRStarTreeFactory<O extends NumberVector<O, ?>> extends AbstractRStarTreeFactory<O, FlatRStarTreeIndex<O>> {
  /**
   * Constructor.
   * 
   * @param fileName
   * @param pageSize
   * @param cacheSize
   * @param bulkSplitter Bulk loading strategy
   * @param insertionCandidates
   */
  public FlatRStarTreeFactory(String fileName, int pageSize, long cacheSize, BulkSplit bulkSplitter, int insertionCandidates) {
    super(fileName, pageSize, cacheSize, bulkSplitter, insertionCandidates);
    // TODO Auto-generated constructor stub
  }

  @Override
  public FlatRStarTreeIndex<O> instantiate(Relation<O> relation) {
    PageFile<FlatRStarTreeNode> pagefile = makePageFile(getNodeClass());
    return new FlatRStarTreeIndex<O>(relation, pagefile, bulkSplitter, insertionCandidates);
  }

  protected Class<FlatRStarTreeNode> getNodeClass() {
    return FlatRStarTreeNode.class;
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
    protected FlatRStarTreeFactory<O> makeInstance() {
      return new FlatRStarTreeFactory<O>(fileName, pageSize, cacheSize, bulkSplitter, insertionCandidates);
    }
  }
}