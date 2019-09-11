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
package elki.datasource.filter.selection;

import java.util.Random;

import elki.datasource.bundle.BundleMeta;
import elki.datasource.filter.AbstractStreamFilter;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Subsampling stream filter.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class RandomSamplingStreamFilter extends AbstractStreamFilter {
  /**
   * Probability
   */
  protected double prob;

  /**
   * Random generator
   */
  protected Random random;

  /**
   * Constructor.
   * 
   * @param prob Probability
   * @param rnd Random generator
   */
  public RandomSamplingStreamFilter(double prob, RandomFactory rnd) {
    super();
    this.prob = prob;
    this.random = rnd.getSingleThreadedRandom();
  }

  @Override
  public BundleMeta getMeta() {
    return source.getMeta();
  }

  @Override
  public Object data(int rnum) {
    return source.data(rnum);
  }

  @Override
  public Event nextEvent() {
    while(true) {
      Event ev = source.nextEvent();
      switch(ev){
      case END_OF_STREAM:
        return ev;
      case META_CHANGED:
        return ev;
      case NEXT_OBJECT:
        if(random.nextDouble() < prob) {
          return ev;
        }
        continue;
      }
    }
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Option ID for sampling probability
     */
    public static final OptionID PROB_ID = new OptionID("sampling.p", "Sampling probability. Each object has a chance of being sampled with this probability.");

    /**
     * Option ID for random seed
     */
    public static final OptionID SEED_ID = new OptionID("sampling.seed", "Random generator seed for sampling.");

    /**
     * Probability
     */
    protected double prob;

    /**
     * Random generator
     */
    protected RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(PROB_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE) //
          .addConstraint(CommonConstraints.LESS_EQUAL_ONE_DOUBLE) //
          .grab(config, x -> prob = x);
      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
    }

    @Override
    public RandomSamplingStreamFilter make() {
      return new RandomSamplingStreamFilter(prob, rnd);
    }
  }
}
