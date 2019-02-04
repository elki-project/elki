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
package de.lmu.ifi.dbs.elki.algorithm.outlier.lof.parallel;

import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.parallel.Executor;
import de.lmu.ifi.dbs.elki.parallel.processor.AbstractDoubleProcessor;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedDouble;

/**
 * Processor for the "local reachability density" of LOF.
 * 
 * Note: we compute 1/lrd, the local reachability distance.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - - - Instance
 */
public class SimplifiedLRDProcessor extends AbstractDoubleProcessor {
  /**
   * KNN store
   */
  private DataStore<? extends KNNList> knns;

  /**
   * Constructor.
   * 
   * @param knns k nearest neighbors
   */
  public SimplifiedLRDProcessor(DataStore<? extends KNNList> knns) {
    super();
    this.knns = knns;
  }

  @Override
  public Instance instantiate(Executor master) {
    return new Instance(master.getInstance(output));
  }

  /**
   * Instance
   * 
   * @author Erich Schubert
   */
  private class Instance extends AbstractDoubleProcessor.Instance {
    /**
     * Constructor.
     * 
     * @param output Output variable
     */
    public Instance(SharedDouble.Instance output) {
      super(output);
    }

    @Override
    public void map(DBIDRef id) {
      KNNList knn = knns.get(id);
      double lrd = 0.0;
      int size = 0;
      for(DoubleDBIDListIter n = knn.iter(); n.valid(); n.advance()) {
        // Do not include the query object
        if(DBIDUtil.equal(n, id)) {
          continue;
        }
        lrd += n.doubleValue();
        size++;
      }
      // Avoid division by zero.
      output.set(lrd > 0 ? size / lrd : 0);
    }
  }
}