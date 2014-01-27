package experimentalcode.erich.parallel.mapper;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.datastore.DoubleDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import experimentalcode.erich.parallel.MapExecutor;
import experimentalcode.erich.parallel.SharedDouble;

/**
 * Mapper for the "local reachability density" of LOF.
 * 
 * @author Erich Schubert
 */
public class LRDMapper extends AbstractDoubleMapper {
  /**
   * KNN store
   */
  private DataStore<? extends KNNList> knns;

  /**
   * k-distance store
   */
  private DoubleDataStore kdists;

  /**
   * Constructor.
   * 
   * @param knns k nearest neighbors
   * @param kdists k distances
   */
  public LRDMapper(DataStore<? extends KNNList> knns, DoubleDataStore kdists) {
    super();
    this.knns = knns;
    this.kdists = kdists;
  }

  @Override
  public Instance instantiate(MapExecutor mapper) {
    return new Instance(output.instantiate(mapper));
  }

  /**
   * Mapper instance
   * 
   * @author Erich Schubert
   */
  private class Instance extends AbstractDoubleMapper.Instance {
    /**
     * Constructor.
     * 
     * @param output Output variable
     */
    protected Instance(SharedDouble.Instance output) {
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
        lrd += Math.max(kdists.doubleValue(n), n.doubleValue());
        size += 1;
      }
      // Avoid division by 0:
      output.set(lrd > 0 ? size / lrd : Double.POSITIVE_INFINITY);
    }
  }
}