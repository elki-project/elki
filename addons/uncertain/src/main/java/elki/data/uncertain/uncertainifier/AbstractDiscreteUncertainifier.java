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
package elki.data.uncertain.uncertainifier;

import elki.data.uncertain.UncertainObject;
import elki.logging.LoggingUtil;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Factory class for discrete uncertain objects.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <UO> Uncertain object type.
 */
public abstract class AbstractDiscreteUncertainifier<UO extends UncertainObject> implements Uncertainifier<UO> {
  /**
   * Inner class for generating uncertain instances.
   */
  protected Uncertainifier<?> inner;

  /**
   * Minimum and maximum number of samples.
   */
  protected int minQuant, maxQuant;

  /**
   * Constructor.
   *
   * @param inner Inner uncertainifier
   * @param minQuant Minimum number of samples
   * @param maxQuant Maximum number of samples
   */
  public AbstractDiscreteUncertainifier(Uncertainifier<?> inner, int minQuant, int maxQuant) {
    super();
    this.inner = inner;
    this.minQuant = minQuant;
    this.maxQuant = maxQuant;
  }

  /**
   * Par.
   *
   * @author Erich Schubert
   */
  public abstract static class Par implements Parameterizer {
    /**
     * Default sample size for generating finite representations.
     */
    public final static int DEFAULT_SAMPLE_SIZE = 10;

    /**
     * Class to use for generating the uncertain instances.
     */
    public static final OptionID INNER_ID = new OptionID("uo.discrete.generator", "Class to generate the point distribution.");

    /**
     * Maximum quantity of generated samples.
     */
    public static final OptionID MULT_MAX_ID = new OptionID("uo.quantity.max", "Maximum points per uncertain object.");

    /**
     * Minimum quantity of generated samples.
     */
    public static final OptionID MULT_MIN_ID = new OptionID("uo.quantity.min", "Minimum points per uncertain object (defaults to maximum.");

    /**
     * Inner class for generating uncertain instances.
     */
    protected Uncertainifier<?> inner;

    /**
     * Minimum and maximum number of samples.
     */
    protected int minQuant, maxQuant;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Uncertainifier<?>>(INNER_ID, Uncertainifier.class) //
          .grab(config, x -> inner = x);
      if(inner instanceof AbstractDiscreteUncertainifier) {
        LoggingUtil.warning("Using a discrete uncertainifier inside a discrete uncertainifier is likely a configuration error, and is likely to produce too many duplicate points. Did you mean to use a uniform or gaussian distribution instead?");
      }
      new IntParameter(MULT_MAX_ID, DEFAULT_SAMPLE_SIZE) //
          .grab(config, x -> maxQuant = x);
      minQuant = maxQuant;
      new IntParameter(MULT_MIN_ID) //
          .setOptional(true) //
          .grab(config, x -> minQuant = x);
    }
  }
}
