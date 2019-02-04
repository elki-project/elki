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
package de.lmu.ifi.dbs.elki.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Produce a database of random double vectors with each dimension in [0:1].
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public class RandomDoubleVectorDatabaseConnection extends AbstractDatabaseConnection {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(RandomDoubleVectorDatabaseConnection.class);

  /**
   * Dimensionality.
   */
  protected int dim = -1;

  /**
   * Size of database to generate.
   */
  protected int size = -1;

  /**
   * Random generator
   */
  protected RandomFactory rnd;

  /**
   * Constructor.
   * 
   * @param dim Dimensionality
   * @param size Database size
   * @param rnd Random generator
   * @param filters Filters to use
   */
  public RandomDoubleVectorDatabaseConnection(int dim, int size, RandomFactory rnd, List<ObjectFilter> filters) {
    super(filters);
    this.dim = dim;
    this.size = size;
    this.rnd = rnd;
  }

  @Override
  public MultipleObjectsBundle loadData() {
    VectorFieldTypeInformation<DoubleVector> type = new VectorFieldTypeInformation<>(DoubleVector.FACTORY, dim);
    List<DoubleVector> vectors = new ArrayList<>(size);

    // Setup random generator
    final Random rand = rnd.getSingleThreadedRandom();

    // Produce random vectors
    for(int i = 0; i < size; i++) {
      vectors.add(VectorUtil.randomVector(DoubleVector.FACTORY, dim, rand));
    }

    return MultipleObjectsBundle.makeSimple(type, vectors);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDatabaseConnection.Parameterizer {
    /**
     * Random generator seed.
     */
    public static final OptionID SEED_ID = new OptionID("dbc.genseed", "Seed for randomly generating vectors");

    /**
     * Database to specify the random vector dimensionality.
     */
    public static final OptionID DIM_ID = new OptionID("dbc.dim", "Dimensionality of the vectors to generate.");

    /**
     * Parameter to specify the database size to generate.
     */
    public static final OptionID SIZE_ID = new OptionID("dbc.size", "Database size to generate.");

    /**
     * Dimensionality.
     */
    int dim = -1;

    /**
     * Database size.
     */
    int size = -1;

    /**
     * Random generator.
     */
    RandomFactory rnd;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configFilters(config);
      IntParameter dimParam = new IntParameter(DIM_ID);
      if(config.grab(dimParam)) {
        dim = dimParam.intValue();
      }
      IntParameter sizeParam = new IntParameter(SIZE_ID);
      if(config.grab(sizeParam)) {
        size = sizeParam.intValue();
      }
      RandomParameter rndP = new RandomParameter(SEED_ID);
      if(config.grab(rndP)) {
        rnd = rndP.getValue();
      }
    }

    @Override
    protected RandomDoubleVectorDatabaseConnection makeInstance() {
      return new RandomDoubleVectorDatabaseConnection(dim, size, rnd, filters);
    }
  }
}
