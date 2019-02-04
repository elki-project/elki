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
package de.lmu.ifi.dbs.elki.data.uncertain.uncertainifier;

import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
   * Parameterizer.
   *
   * @author Erich Schubert
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
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
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<Uncertainifier<?>> innerP = new ObjectParameter<>(INNER_ID, Uncertainifier.class);
      if(config.grab(innerP)) {
        inner = innerP.instantiateClass(config);
        if(inner instanceof AbstractDiscreteUncertainifier) {
          LoggingUtil.warning("Using a discrete uncertainifier inside a discrete uncertainifier is likely a configuration error, and is likely to produce too many duplicate points. Did you mean to use a uniform or gaussian distribution instead?");
        }
      }
      IntParameter pmultMax = new IntParameter(MULT_MAX_ID, DEFAULT_SAMPLE_SIZE);
      if(config.grab(pmultMax)) {
        maxQuant = pmultMax.intValue();
      }
      IntParameter pmultMin = new IntParameter(MULT_MIN_ID) //
      .setOptional(true);
      minQuant = config.grab(pmultMin) ? pmultMin.intValue() : maxQuant;
    }
  }
}