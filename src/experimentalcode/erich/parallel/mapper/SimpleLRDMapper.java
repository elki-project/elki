package experimentalcode.erich.parallel.mapper;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
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
public class SimpleLRDMapper<D extends NumberDistance<D, ?>> implements Mapper {
  /**
   * KNN store
   */
  private DataStore<? extends KNNList<D>> knns;

  /**
   * Output variable
   */
  private SharedDouble output;

  /**
   * Constructor.
   * 
   * @param knns k nearest neighbors
   */
  public SimpleLRDMapper(DataStore<? extends KNNList<D>> knns) {
    super();
    this.knns = knns;
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
      KNNList<D> knn = knns.get(id);
      double lrd = 0.0;
      int size = 0;
      for(DistanceDBIDListIter<D> n = knn.iter(); n.valid(); n.advance()) {
        // Do not include the query object
        if(DBIDUtil.equal(n, id)) {
          continue;
        }
        lrd += n.getDistance().doubleValue();
        size++;
      }
      // Avoid division by zero.
      output.set(lrd > 0 ? size / lrd : 0);
    }

    @Override
    public void cleanup() {
      // Nothing to do.
    }
  }
}