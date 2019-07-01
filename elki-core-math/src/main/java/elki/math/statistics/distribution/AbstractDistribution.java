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
package elki.math.statistics.distribution;

import java.util.Random;

import elki.utilities.optionhandling.AbstractParameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;

/**
 * Abstract base class for distributions.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public abstract class AbstractDistribution implements Distribution {
  /**
   * Random source.
   */
  protected Random random;

  /**
   * Constructor.
   * 
   * @param rnd Random source (may be {@code null}, but then
   *        {@code nextRandom()} must not be used).
   */
  public AbstractDistribution(RandomFactory rnd) {
    this(rnd != null ? rnd.getRandom() : null);
  }

  /**
   * Constructor.
   * 
   * @param rnd Random source (may be {@code null}, but then
   *        {@code nextRandom()} must not be used).
   */
  public AbstractDistribution(Random rnd) {
    super();
    this.random = rnd;
  }

  @Override
  public double nextRandom() {
    assert random != null : "Provide a random generator during class initialization or via setRandomSource.";
    return quantile(random.nextDouble());
  }


  /**
   * Change random source.
   *
   * @param rnd Random source
   */
  public void setRandomSource(Random rnd) {
    assert rnd != null;
    this.random = rnd;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the random seeding source.
     */
    public static final OptionID RANDOM_ID = new OptionID("distribution.random", "Random generation data source.");

    /**
     * Location parameter.
     */
    public static final OptionID LOCATION_ID = new OptionID("distribution.location", "Distribution location parameter");

    /**
     * Scale parameter.
     */
    public static final OptionID SCALE_ID = new OptionID("distribution.scale", "Distribution scale parameter");

    /**
     * Shape parameter.
     */
    public static final OptionID SHAPE_ID = new OptionID("distribution.shape", "Distribution shape parameter");

    /**
     * Random source.
     */
    RandomFactory rnd;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      RandomParameter randomP = new RandomParameter(RANDOM_ID);
      if(config.grab(randomP)) {
        rnd = randomP.getValue();
      }
    }

    @Override
    abstract protected Distribution makeInstance();
  }
}
