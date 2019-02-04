/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.evaluation.index;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.index.tree.IndexTree;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Simple index analytics, which includes the toString() dump of index
 * information.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @opt nodefillcolor LemonChiffon
 * @navhas - create - IndexMetaResult
 */
public class IndexStatistics implements Evaluator {
  /**
   * Constructor.
   */
  public IndexStatistics() {
    super();
  }

  @Override
  public void processNewResult(ResultHierarchy hier, Result newResult) {
    Database db = ResultUtil.findDatabase(hier);
    Collection<String> header = null;
    final ArrayList<IndexTree<?, ?>> indexes = ResultUtil.filterResults(hier, newResult, IndexTree.class);
    if (indexes == null || indexes.isEmpty()) {
      return;
    }
    for(IndexTree<?, ?> index : indexes) {
      header = new ArrayList<>();
      header.add(index.toString());
    }
    Collection<Pair<String, String>> col = new ArrayList<>();
    IndexMetaResult analysis = new IndexMetaResult(col, header);
    db.getHierarchy().add(db, analysis);
  }

  /**
   * Result class.
   * 
   * @author Erich Schubert
   */
  public class IndexMetaResult extends CollectionResult<Pair<String, String>> {
    /**
     * Constructor.
     * 
     * @param col key value pairs
     * @param header header
     */
    public IndexMetaResult(Collection<Pair<String, String>> col, Collection<String> header) {
      super("Index Statistics", "index-meta", col, header);
    }
  }
}