package experimentalcode.erich.parallel.mapper;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.DistanceDBIDResultIter;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import experimentalcode.erich.parallel.MapExecutor;
import experimentalcode.erich.parallel.SharedDouble;

/**
 * Mapper for the "local reachability density" of LOF.
 * 
 * Note: we compute 1/lrd, the local reachability distance.
 * 
 * @author Erich Schubert
 */
public class LRDMapper<D extends NumberDistance<D, ?>> implements Mapper {
  /**
   * KNN store
   */
  private DataStore<? extends KNNResult<D>> knns;

  /**
   * k-distance store
   */
  private DoubleDataStore kdists;

  /**
   * Output variable
   */
  private SharedDouble output;

  /**
   * Constructor.
   * 
   * @param knns k nearest neighbors
   * @param kdists k distances
   */
  public LRDMapper(DataStore<? extends KNNResult<D>> knns, DoubleDataStore kdists) {
    super();
    this.knns = knns;
    this.kdists = kdists;
  }

  /**
   * Connect the output variable.
   * 
   * @param output Output variable
   */
  public void connectOutput(SharedDouble output) {
    this.output = output;
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
  private class Instance implements Mapper.Instance {
    /**
     * Output variable
     */
    private SharedDouble.Instance output;

    /**
     * Constructor.
     * 
     * @param output Output variable
     */
    public Instance(SharedDouble.Instance output) {
      super();
      this.output = output;
    }

    @Override
    public void map(DBIDRef id) {
      KNNResult<D> knn = knns.get(id);
      final int size = knn.size() - 1;
      double lrd = 0.0;
      for(DistanceDBIDResultIter<D> n = knn.iter(); n.valid(); n.advance()) {
        if(DBIDUtil.equal(n, id)) {
          // TODO: check that the size value is correct!
          continue;
        }
        lrd += Math.max(kdists.doubleValue(n), n.getDistance().doubleValue()) / size;
      }
      output.set(lrd);
    }

    @Override
    public void cleanup() {
      // Nothing to do.
    }
  }
}