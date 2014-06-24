package de.lmu.ifi.dbs.elki.parallel.mapper;

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
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.parallel.MapExecutor;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedDouble;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedObject;

/**
 * Mapper to compute the kNN distance.
 * 
 * Needs the k nearest neighbors as input, for example from {@link KNNMapper}.
 * 
 * @author Erich Schubert
 */
public class KDistanceMapper extends AbstractDoubleMapper {
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
  SharedObject<? extends KNNList> input;

  /**
   * Connect the input channel.
   * 
   * @param input Input channel
   */
  public void connectKNNInput(SharedObject<? extends KNNList> input) {
    this.input = input;
  }

  @Override
  public Instance instantiate(MapExecutor mapper) {
    return new Instance(k, mapper.getInstance(input), mapper.getInstance(output));
  }

  /**
   * Mapper instance for precomputing the kNN.
   * 
   * @author Erich Schubert
   */
  public static class Instance extends AbstractDoubleMapper.Instance {
    /**
     * k Parameter
     */
    int k;

    /**
     * kNN query
     */
    SharedObject.Instance<? extends KNNList> input;

    /**
     * Constructor.
     * 
     * @param k K parameter
     * @param knnq KNN query
     * @param store Datastore to write to
     */
    protected Instance(int k, SharedObject.Instance<? extends KNNList> input, SharedDouble.Instance store) {
      super(store);
      this.k = k;
      this.input = input;
    }

    @Override
    public void map(DBIDRef id) {
      output.set(input.get().get(k - 1).doubleValue());
    }
  }
}
