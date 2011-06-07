package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.flat;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
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
   * @param bulk
   * @param bulkLoadStrategy
   * @param insertionCandidates
   */
  public FlatRStarTreeFactory(String fileName, int pageSize, long cacheSize, boolean bulk, Strategy bulkLoadStrategy, int insertionCandidates) {
    super(fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates);
    // TODO Auto-generated constructor stub
  }

  @Override
  public FlatRStarTreeIndex<O> instantiate(Relation<O> relation) {
    PageFile<FlatRStarTreeNode> pagefile = makePageFile(getNodeClass());
    return new FlatRStarTreeIndex<O>(relation, pagefile, bulk, bulkLoadStrategy, insertionCandidates);
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
      return new FlatRStarTreeFactory<O>(fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates);
    }
  }
}