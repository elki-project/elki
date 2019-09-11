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
package elki.evaluation.classification.holdout;

import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * A holdout providing a seed for randomized operations.
 * 
 * @author Arthur Zimek
 * @since 0.7.0
 */
public abstract class RandomizedHoldout extends AbstractHoldout {
  /**
   * The random generator.
   */
  protected RandomFactory random;

  /**
   * Sets the parameter seed to the parameterToDescription map.
   */
  public RandomizedHoldout(RandomFactory random) {
    super();
    this.random = random;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public abstract static class Par implements Parameterizer {
    /**
     * Random seeding for holdout evaluation.
     */
    public static final OptionID SEED_ID = new OptionID("holdout.seed", "Random generator seed for holdout evaluation.");

    /**
     * The random generator.
     */
    protected RandomFactory random;

    @Override
    public void configure(Parameterization config) {
      new RandomParameter(SEED_ID).grab(config, x -> random = x);
    }
  }
}
