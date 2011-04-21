package experimentalcode.marisa.index.xtree.common;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import experimentalcode.marisa.index.xtree.XTreeBaseFactory;

public class XTreeFactory<O extends NumberVector<O, ?>> extends XTreeBaseFactory<O, XTree<O>> {
  public XTreeFactory(String fileName, int pageSize, long cacheSize, boolean bulk, Strategy bulkLoadStrategy, int insertionCandidates, double relativeMinEntries, double relativeMinFanout, float reinsert_fraction, float max_overlap, int overlap_type) {
    super(fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates, relativeMinEntries, relativeMinFanout, reinsert_fraction, max_overlap, overlap_type);
  }

  @Override
  public XTree<O> instantiate(Relation<O> relation) {
    return new XTree<O>(relation, fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates, relativeMinEntries, relativeMinFanout, reinsert_fraction, max_overlap, overlap_type);
  }

  public static class Parameterizer<O extends NumberVector<O, ?>> extends XTreeBaseFactory.Parameterizer<O> {
    @Override
    protected AbstractRStarTreeFactory<O, ?> makeInstance() {
      return new XTreeFactory<O>(fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates, relativeMinEntries, relativeMinFanout, reinsert_fraction, max_overlap, overlap_type);
    }
  }
}
