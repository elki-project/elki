package de.lmu.ifi.dbs.elki.datasource;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;

/**
 * Produce a database of random double vectors with each dimension in [0:1]
 * 
 * @author Erich Schubert
 */
public class RandomDoubleVectorDatabaseConnection extends AbstractDatabaseConnection {
  /**
   * Dimensionality
   */
  protected int dim = -1;

  /**
   * Size of database to generate
   */
  protected int size = -1;

  /**
   * Seed to use
   */
  protected Long seed;

  /**
   * Constructor.
   * 
   * @param dim Dimensionality
   * @param size Database size
   * @param seed Random seed
   * @param filters
   */
  public RandomDoubleVectorDatabaseConnection(int dim, int size, Long seed, List<ObjectFilter> filters) {
    super(filters);
    this.dim = dim;
    this.size = size;
    this.seed = seed;
  }

  private static final Logging logger = Logging.getLogger(RandomDoubleVectorDatabaseConnection.class);

  
  @Override
  public MultipleObjectsBundle loadData() {
    VectorFieldTypeInformation<DoubleVector> type = VectorFieldTypeInformation.get(DoubleVector.class, dim);
    List<DoubleVector> vectors = new ArrayList<DoubleVector>(size);

    // Setup random generator
    final Random rand;
    if(seed == null) {
      rand = new Random();
    }
    else {
      rand = new Random(seed);
    }

    // Produce random vectors
    DoubleVector factory = new DoubleVector(new double[dim]);
    for(int i = 0; i < size; i++) {
      vectors.add(VectorUtil.randomVector(factory, rand));
    }

    return MultipleObjectsBundle.makeSimple(type, vectors);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractDatabaseConnection.Parameterizer {
    /**
     * Random generator seed.
     * <p>
     * Key: {@code -dbc.seed}
     * </p>
     */
    public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("dbc.genseed", "Seed for randomly generating vectors");

    /**
     * Database to specify the random vector dimensionality
     * <p>
     * Key: {@code -dbc.dim}
     * </p>
     */
    public static final OptionID DIM_ID = OptionID.getOrCreateOptionID("dbc.dim", "Dimensionality of the vectors to generate.");

    /**
     * Parameter to specify the database size to generate.
     * <p>
     * Key: {@code -dbc.size}
     * </p>
     */
    public static final OptionID SIZE_ID = OptionID.getOrCreateOptionID("dbc.size", "Database size to generate.");

    int dim = -1;

    int size = -1;

    Long seed = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      configFilters(config);
      configDimensionality(config);
      configSize(config);
      configSeed(config);
    }

    protected void configSeed(Parameterization config) {
      LongParameter seedParam = new LongParameter(SEED_ID, true);
      if(config.grab(seedParam)) {
        seed = seedParam.getValue();
      }
    }

    protected void configDimensionality(Parameterization config) {
      IntParameter dimParam = new IntParameter(DIM_ID);
      if(config.grab(dimParam)) {
        dim = dimParam.getValue();
      }
    }

    protected void configSize(Parameterization config) {
      IntParameter sizeParam = new IntParameter(SIZE_ID);
      if(config.grab(sizeParam)) {
        size = sizeParam.getValue();
      }
    }

    @Override
    protected RandomDoubleVectorDatabaseConnection makeInstance() {
      return new RandomDoubleVectorDatabaseConnection(dim, size, seed, filters);
    }
  }
}