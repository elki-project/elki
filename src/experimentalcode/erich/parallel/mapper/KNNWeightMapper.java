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
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.KNNList;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import experimentalcode.erich.parallel.MapExecutor;
import experimentalcode.erich.parallel.SharedDouble;
import experimentalcode.erich.parallel.SharedObject;

/**
 * Mapper to compute the kNN weight score.
 * 
 * Needs the k nearest neighbors as input, for example from {@link KNNMapper}.
 * 
 * @author Erich Schubert
 */
public class KNNWeightMapper extends AbstractDoubleMapper {
  /**
   * K parameter
   */
  int k;

  /**
   * Constructor.
   * 
   * @param k K parameter
   */
  public KNNWeightMapper(int k) {
    super();
    this.k = k;
  }

  /**
   * KNN query object
   */
  SharedObject<? extends KNNList<? extends NumberDistance<?, ?>>> input;

  /**
   * Connect the input channel.
   * 
   * @param input Input channel
   */
  public void connectKNNInput(SharedObject<? extends KNNList<? extends NumberDistance<?, ?>>> input) {
    this.input = input;
  }

  @Override
  public Instance instantiate(MapExecutor mapper) {
    return new Instance(k, input.instantiate(mapper), output.instantiate(mapper));
  }

  /**
   * Mapper instance for precomputing the kNN.
   * 
   * @author Erich Schubert
   */
  private static class Instance extends AbstractDoubleMapper.Instance {
    /**
     * k Parameter
     */
    int k;

    /**
     * kNN query
     */
    SharedObject.Instance<? extends KNNList<? extends NumberDistance<?, ?>>> input;

    /**
     * Constructor.
     * 
     * @param k K parameter
     * @param knnq KNN query
     * @param store Datastore to write to
     */
    protected Instance(int k, SharedObject.Instance<? extends KNNList<? extends NumberDistance<?, ?>>> input, SharedDouble.Instance store) {
      super(store);
      this.k = k;
      this.input = input;
    }

    @Override
    public void map(DBIDRef id) {
      final KNNList<? extends NumberDistance<?, ?>> list = input.get();
      int i = 0;
      double sum = 0;
      DistanceDBIDListIter<? extends NumberDistance<?, ?>> iter = list.iter();
      if (iter instanceof DoubleDistanceDBIDListIter) { // Double optimized
        DoubleDistanceDBIDListIter i2 = (DoubleDistanceDBIDListIter) iter;
        for (; i2.valid() && i < k; i2.advance(), ++i) {
          sum += i2.doubleDistance();
        }
      } else { // Generic number distance
        for (; iter.valid() && i < k; iter.advance(), ++i) {
          sum += iter.getDistance().doubleValue();
        }
      }
      output.set(sum);
    }
  }
}
