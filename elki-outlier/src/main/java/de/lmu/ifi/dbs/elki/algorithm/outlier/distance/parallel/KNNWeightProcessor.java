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
package de.lmu.ifi.dbs.elki.algorithm.outlier.distance.parallel;

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.KNNList;
import de.lmu.ifi.dbs.elki.parallel.Executor;
import de.lmu.ifi.dbs.elki.parallel.processor.AbstractDoubleProcessor;
import de.lmu.ifi.dbs.elki.parallel.processor.KNNProcessor;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedDouble;
import de.lmu.ifi.dbs.elki.parallel.variables.SharedObject;

/**
 * Compute the kNN weight score, used by {@link ParallelKNNWeightOutlier}.
 * 
 * Needs the k nearest neighbors as input, for example from {@link KNNProcessor}
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class KNNWeightProcessor extends AbstractDoubleProcessor {
  /**
   * K parameter
   */
  int k;

  /**
   * Constructor.
   * 
   * @param k K parameter
   */
  public KNNWeightProcessor(int k) {
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
  public Instance instantiate(Executor executor) {
    return new Instance(k, executor.getInstance(input), executor.getInstance(output));
  }

  /**
   * Instance for precomputing the kNN.
   * 
   * @author Erich Schubert
   */
  private static class Instance extends AbstractDoubleProcessor.Instance {
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
     * @param input kNN list input
     * @param store Datastore to write to
     */
    protected Instance(int k, SharedObject.Instance<? extends KNNList> input, SharedDouble.Instance store) {
      super(store);
      this.k = k;
      this.input = input;
    }

    @Override
    public void map(DBIDRef id) {
      final KNNList list = input.get();
      int i = 0;
      double sum = 0;
      for(DoubleDBIDListIter iter = list.iter(); iter.valid() && i < k; iter.advance(), ++i) {
        sum += iter.doubleValue();
      }
      output.set(sum);
    }
  }
}
