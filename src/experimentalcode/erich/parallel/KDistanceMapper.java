package experimentalcode.erich.parallel;

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

/**
 * Mapper to compute the kNN distance.
 * 
 * Needs the k nearest neighbors as input, for example from {@link KNNMapper}.
 * 
 * @author Erich Schubert
 * 
 * @param <D> Distance type
 */
public class KDistanceMapper<D extends Distance<D>> implements Mapper {
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
  InputChannel<KNNResult<D>> input;

  /**
   * Output channel to write to
   */
  OutputChannel<D> out;

  /**
   * Connect the input channel.
   * 
   * @param input Input channel
   */
  public void connectInput(InputChannel<KNNResult<D>> input) {
    this.input = input;
  }

  /**
   * Connect the output channel.
   * 
   * @param output Output channel
   */
  public void connectOutput(OutputChannel<D> output) {
    this.out = output;
  }

  @Override
  public Instance<D> instantiate(MapExecutor mapper) {
    return new Instance<D>(k, mapper.instantiate(input), mapper.instantiate(out));
  }

  /**
   * Mapper instance for precomputing the kNN.
   * 
   * @author Erich Schubert
   */
  public static class Instance<D extends Distance<D>> implements Mapper.Instance {
    /**
     * k Parameter
     */
    int k;

    /**
     * kNN query
     */
    InputChannel.Instance<KNNResult<D>> input;

    /**
     * Output data store
     */
    OutputChannel.Instance<D> store;

    /**
     * Constructor.
     * 
     * @param k K parameter
     * @param knnq KNN query
     * @param store Datastore to write to
     */
    protected Instance(int k, InputChannel.Instance<KNNResult<D>> input, OutputChannel.Instance<D> store) {
      super();
      this.k = k;
      this.input = input;
      this.store = store;
    }

    @Override
    public void map(DBIDRef id) {
      store.put(id, input.get(id).get(k).getDistance());
    }

    @Override
    public void cleanup() {
      // Nothing to do.
    }
  }
}