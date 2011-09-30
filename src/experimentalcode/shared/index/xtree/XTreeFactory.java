package experimentalcode.shared.index.xtree;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
Ludwig-Maximilians-Universität München
Lehr- und Forschungseinheit für Datenbanksysteme
ELKI Development Team

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialEntry;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.bulk.BulkSplit;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert.InsertionStrategy;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

public class XTreeFactory<O extends NumberVector<O, ?>> extends XTreeBaseFactory<O, XTreeNode, SpatialEntry, XTreeIndex<O>> {
  public XTreeFactory(String fileName, int pageSize, long cacheSize, BulkSplit bulkSplitter, InsertionStrategy insertionStrategy, double relativeMinEntries, double relativeMinFanout, float reinsert_fraction, float max_overlap, int overlap_type) {
    super(fileName, pageSize, cacheSize, bulkSplitter, insertionStrategy, relativeMinEntries, relativeMinFanout, reinsert_fraction, max_overlap, overlap_type);
  }

  @Override
  public XTreeIndex<O> instantiate(Relation<O> relation) {
    PageFile<XTreeNode> pagefile = makePageFile(getNodeClass());
    XTreeIndex<O> index = new XTreeIndex<O>(relation, pagefile, relativeMinEntries, relativeMinFanout, reinsert_fraction, max_overlap, overlap_type);
    index.setBulkStrategy(bulkSplitter);
    index.setInsertionStrategy(insertionStrategy);
    return index;
  }

  protected Class<XTreeNode> getNodeClass() {
    return XTreeNode.class;
  }
  
  public static class Parameterizer<O extends NumberVector<O, ?>> extends XTreeBaseFactory.Parameterizer<O> {
    @Override
    protected AbstractRStarTreeFactory<O, ?, ?, ?> makeInstance() {
      return new XTreeFactory<O>(fileName, pageSize, cacheSize, bulkSplitter, insertionStrategy, relativeMinEntries, relativeMinFanout, reinsert_fraction, max_overlap, overlap_type);
    }
  }
}
