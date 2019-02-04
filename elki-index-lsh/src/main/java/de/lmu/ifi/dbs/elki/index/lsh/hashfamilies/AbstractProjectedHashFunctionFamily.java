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
package de.lmu.ifi.dbs.elki.index.lsh.hashfamilies;

import java.util.ArrayList;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.projection.random.RandomProjectionFamily;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.index.lsh.hashfunctions.LocalitySensitiveHashFunction;
import de.lmu.ifi.dbs.elki.index.lsh.hashfunctions.MultipleProjectionsLocalitySensitiveHashFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Abstract base class for projection based hash functions.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @assoc - - - RandomProjectionFamily
 * @has - - - MultipleProjectionsLocalitySensitiveHashFunction
 */
public abstract class AbstractProjectedHashFunctionFamily implements LocalitySensitiveHashFunctionFamily<NumberVector> {
  /**
   * Random generator to use.
   */
  protected RandomFactory random;

  /**
   * Projection family to use.
   */
  protected RandomProjectionFamily proj;

  /**
   * Width of each bin.
   */
  protected double width;

  /**
   * The number of projections to use for each hash function.
   */
  protected int k;

  /**
   * Constructor.
   * 
   * @param random Random generator
   * @param proj Projection family
   * @param width Bin width
   * @param k Number of projections for each hash function.
   */
  public AbstractProjectedHashFunctionFamily(RandomFactory random, RandomProjectionFamily proj, double width, int k) {
    super();
    this.random = random;
    this.proj = proj;
    this.width = width;
    this.k = k;
  }

  @Override
  public ArrayList<? extends LocalitySensitiveHashFunction<? super NumberVector>> generateHashFunctions(Relation<? extends NumberVector> relation, int l) {
    int dim = RelationUtil.dimensionality(relation);
    ArrayList<LocalitySensitiveHashFunction<? super NumberVector>> ps = new ArrayList<>(l);
    final Random rnd = random.getSingleThreadedRandom();
    for(int i = 0; i < l; i++) {
      RandomProjectionFamily.Projection mat = proj.generateProjection(dim, k);
      ps.add(new MultipleProjectionsLocalitySensitiveHashFunction(mat, width, rnd));
    }
    return ps;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for fixing the random seed.
     */
    public static final OptionID RANDOM_ID = new OptionID("lsh.projection.random", "Random seed for generating the projections.");

    /**
     * Parameter for choosing the bin width.
     */
    public static final OptionID WIDTH_ID = new OptionID("lsh.projection.width", "Bin width for random projections.");

    /**
     * Number of projections to use in each hash function.
     */
    public static final OptionID NUMPROJ_ID = new OptionID("lsh.projection.projections", "Number of projections to use for each hash function.");

    /**
     * Random generator to use.
     */
    RandomFactory random;

    /**
     * Width of each bin.
     */
    double width;

    /**
     * The number of projections to use for each hash function.
     */
    int k;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      RandomParameter randP = new RandomParameter(RANDOM_ID, RandomFactory.DEFAULT);
      if(config.grab(randP)) {
        random = randP.getValue();
      }

      DoubleParameter widthP = new DoubleParameter(WIDTH_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(widthP)) {
        width = widthP.doubleValue();
      }

      IntParameter lP = new IntParameter(NUMPROJ_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(lP)) {
        k = lP.intValue();
      }
    }
  }
}
