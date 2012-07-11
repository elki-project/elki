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
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.distance.distanceresultlist.KNNResult;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import experimentalcode.erich.parallel.MapExecutor;
import experimentalcode.erich.parallel.SharedObject;

/**
 * Mapper to compute the kNN distance.
 * 
 * Needs the k nearest neighbors as input, for example from {@link KNNMapper}.
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
public class KDistanceMapper<D extends Distance<?>> implements Mapper {
  /**
   * K parameter
   */
  int k;

  /**
   * Constructor.
   * 
   * @param k K parameter
   */
  public KDistanceMapper(int k) {
    super();
    this.k = k;
  }

  /**
   * KNN query object
   */
  SharedObject<? extends KNNResult<? extends D>> input;

  /**
   * Output channel to write to
   */
  SharedObject<D> out;

  /**
   * Connect the input channel.
   * 
   * @param input Input channel
   */
  public void connectKNNInput(SharedObject<? extends KNNResult<? extends D>> input) {
    this.input = input;
  }

  /**
   * Connect the output channel.
   * 
   * @param output Output channel
   */
  public void connectDistanceOutput(SharedObject<D> output) {
    this.out = output;
  }

  @Override
  public Instance<D> instantiate(MapExecutor mapper) {
    return new Instance<D>(k, input.instantiate(mapper), out.instantiate(mapper));
  }

  /**
   * Mapper instance for precomputing the kNN.
   * 
   * @author Erich Schubert
   */
  public static class Instance<D extends Distance<?>> implements Mapper.Instance {
    /**
     * k Parameter
     */
    int k;

    /**
     * kNN query
     */
    SharedObject.Instance<? extends KNNResult<? extends D>> input;

    /**
     * Output data store
     */
    SharedObject.Instance<D> store;

    /**
     * Constructor.
     * 
     * @param k K parameter
     * @param knnq KNN query
     * @param store Datastore to write to
     */
    protected Instance(int k, SharedObject.Instance<? extends KNNResult<? extends D>> input, SharedObject.Instance<D> store) {
      super();
      this.k = k;
      this.input = input;
      this.store = store;
    }

    @Override
    public void map(DBIDRef id) {
      store.set(input.get().get(k - 1).getDistance());
    }

    @Override
    public void cleanup() {
      // Nothing to do.
    }
  }
}