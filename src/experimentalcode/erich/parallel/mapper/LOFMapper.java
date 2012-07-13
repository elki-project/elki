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
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import experimentalcode.erich.parallel.MapExecutor;
import experimentalcode.erich.parallel.SharedDouble;

/**
 * Mapper for computing the LOF.
 * 
 * Note: we use 1/lrd, as this avoids a division by 0 earlier.
 * 
 * @author Erich Schubert
 */
public class LOFMapper implements Mapper {
  /**
   * KNN store
   */
  private DataStore<? extends KNNResult<?>> knns;

  /**
   * LRD store
   */
  private DoubleDataStore lrds;

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
  public LOFMapper(DataStore<? extends KNNResult<?>> knns, DoubleDataStore lrds) {
    super();
    this.knns = knns;
    this.lrds = lrds;
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
      KNNResult<?> knn = knns.get(id);
      final int size = knn.size() - 1;
      double avlrd = 0.0;
      int cnt = 0;
      for(DBIDIter n = knn.iter(); n.valid(); n.advance()) {
        if(DBIDUtil.equal(n, id)) {
          cnt++;
          continue;
        }
        avlrd += lrds.doubleValue(n) / size;
      }
      if(cnt != 1) {
        avlrd = avlrd * (size / ((double) knn.size() - cnt));
      }
      final double lrdp = lrds.doubleValue(id);
      if(lrdp > 0) {
        output.set(avlrd / lrdp);
      }
      else {
        output.set(1.0);
      }
    }

    @Override
    public void cleanup() {
      // Nothing to do.
    }
  }
}