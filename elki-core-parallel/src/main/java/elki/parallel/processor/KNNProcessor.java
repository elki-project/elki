/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.parallel.processor;

import java.util.function.Supplier;

import elki.database.ids.DBIDRef;
import elki.database.ids.KNNList;
import elki.database.query.knn.KNNSearcher;
import elki.parallel.Executor;
import elki.parallel.variables.SharedObject;

/**
 * Processor to compute the kNN of each object.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @has - - - Instance
 * @assoc - - - KNNSearcher
 * @assoc - - - SharedObject
 * @has - - - KNNList
 */
public class KNNProcessor implements Processor {
  /**
   * K parameter
   */
  int k;

  /**
   * KNN query object
   */
  Supplier<KNNSearcher<DBIDRef>> knnq;

  /**
   * Output channel to write to
   */
  SharedObject<KNNList> out;

  /**
   * Constructor.
   * 
   * @param k K parameter
   * @param knnq Distance query to use
   */
  public KNNProcessor(int k, Supplier<KNNSearcher<DBIDRef>> knnq) {
    super();
    this.k = k;
    this.knnq = knnq;
  }

  /**
   * Connect the output channel.
   * 
   * @param output Output channel
   */
  public void connectKNNOutput(SharedObject<KNNList> output) {
    this.out = output;
  }

  @Override
  public Instance instantiate(Executor executor) {
    return new Instance(k, knnq.get(), executor.getInstance(out));
  }

  @Override
  public void cleanup(Processor.Instance inst) {
    // Nothing to do.
  }

  /**
   * Instance for precomputing the kNN.
   * 
   * @author Erich Schubert
   */
  public static class Instance implements Processor.Instance {
    /**
     * k Parameter
     */
    int k;

    /**
     * kNN query
     */
    KNNSearcher<DBIDRef> knnq;

    /**
     * Output data store
     */
    SharedObject.Instance<KNNList> out;

    /**
     * Constructor.
     * 
     * @param k K parameter
     * @param knnq KNN query
     * @param out Output channel to write to
     */
    protected Instance(int k, KNNSearcher<DBIDRef> knnq, SharedObject.Instance<KNNList> out) {
      super();
      this.k = k;
      this.knnq = knnq;
      this.out = out;
    }

    @Override
    public void map(DBIDRef id) {
      out.set(knnq.getKNN(id, k));
    }
  }
}
