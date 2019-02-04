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
package de.lmu.ifi.dbs.elki.utilities.referencepoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

/**
 * Reference points generated randomly within the used data space.
 *
 * @author Erich Schubert
 * @since 0.3
 */
public class RandomGeneratedReferencePoints implements ReferencePointsHeuristic {
  /**
   * Holds the sample size.
   */
  protected int samplesize;

  /**
   * Holds the scaling factor.
   */
  protected double scale = 1.0;

  /**
   * Random generator.
   */
  protected RandomFactory rnd;

  /**
   * Constructor.
   *
   * @param samplesize Size of desired sample set
   * @param scale Scaling factor
   * @param rnd Random generator
   */
  public RandomGeneratedReferencePoints(int samplesize, double scale, RandomFactory rnd) {
    super();
    this.samplesize = samplesize;
    this.scale = scale;
    this.rnd = rnd;
  }

  @Override
  public Collection<? extends NumberVector> getReferencePoints(Relation<? extends NumberVector> db) {
    double[][] minmax = RelationUtil.computeMinMax(db);
    int dim = RelationUtil.dimensionality(db);

    // Compute mean and extend from minmax.
    double[] mean = minmax[0], delta = minmax[1];
    for(int d = 0; d < dim; d++) {
      delta[d] -= mean[d];
      mean[d] -= delta[d] * .5;
    }

    Random rand = rnd.getSingleThreadedRandom();
    ArrayList<DoubleVector> result = new ArrayList<>(samplesize);
    for(int i = 0; i < samplesize; i++) {
      double[] vec = new double[dim];
      for(int d = 0; d < dim; d++) {
        vec[d] = mean[d] + (rand.nextDouble() - 0.5) * scale * delta[d];
      }
      result.add(DoubleVector.wrap(vec));
    }

    return result;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the number of requested reference points.
     */
    public static final OptionID N_ID = new OptionID("generate.n", "The number of reference points to be generated.");

    /**
     * Parameter for additional scaling of the space, to allow out-of-space
     * reference points.
     */
    public static final OptionID SCALE_ID = new OptionID("generate.scale", "Scale the grid by the given factor. This can be used to obtain reference points outside the used data space.");

    /**
     * Parameter to specify the sample size.
     */
    public static final OptionID RANDOM_ID = new OptionID("generate.random", "Random generator seed.");

    /**
     * Holds the value of {@link #N_ID}.
     */
    protected int samplesize;

    /**
     * Holds the value of {@link #SCALE_ID}.
     */
    protected double scale = 1.0;

    /**
     * Random generator.
     */
    protected RandomFactory rnd;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntParameter samplesizeP = new IntParameter(N_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(samplesizeP)) {
        samplesize = samplesizeP.getValue();
      }

      DoubleParameter scaleP = new DoubleParameter(SCALE_ID, 1.0) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(scaleP)) {
        scale = scaleP.getValue();
      }

      RandomParameter randomP = new RandomParameter(RANDOM_ID);
      if(config.grab(randomP)) {
        rnd = randomP.getValue();
      }
    }

    @Override
    protected RandomGeneratedReferencePoints makeInstance() {
      return new RandomGeneratedReferencePoints(samplesize, scale, rnd);
    }
  }
}
