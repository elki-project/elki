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
package de.lmu.ifi.dbs.elki.algorithm.itemsetmining;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.result.FrequentItemsetsResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Abstract base class for frequent itemset mining.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - produces - FrequentItemsetsResult
 */
public abstract class AbstractFrequentItemsetAlgorithm extends AbstractAlgorithm<FrequentItemsetsResult> {
  /**
   * Minimum support.
   */
  private double minsupp;

  /**
   * Parameter for minimum and maximum length.
   */
  protected int minlength = 0, maxlength = Integer.MAX_VALUE;

  /**
   * Constructor.
   *
   * @param minsupp Minimum support
   * @param minlength Minimum length
   * @param maxlength Maximum length
   */
  public AbstractFrequentItemsetAlgorithm(double minsupp, int minlength, int maxlength) {
    super();
    this.minsupp = minsupp;
    this.minlength = minlength;
    this.maxlength = maxlength > 0 ? maxlength : Integer.MAX_VALUE;
  }

  /**
   * Constructor.
   *
   * @param minsupp Minimum support
   */
  public AbstractFrequentItemsetAlgorithm(double minsupp) {
    this(minsupp, 0, Integer.MAX_VALUE);
  }

  /**
   * Get the minimum support for a given data set size.
   * 
   * Converts relative minimum support to absolute minimum support.
   * 
   * @param size Data set size
   * @return Minimum support
   */
  public int getMinimumSupport(int size) {
    return (int) ((minsupp < 1.) ? Math.ceil(minsupp * size) : minsupp);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter to specify the minimum support, in absolute or relative terms.
     */
    public static final OptionID MINSUPP_ID = new OptionID("itemsetmining.minsupp", //
    "Threshold for minimum support as minimally required number of transactions (if > 1) " //
        + "or the minimum frequency (if <= 1).");

    /**
     * Parameter to specify the minimum itemset length.
     */
    public static final OptionID MINLENGTH_ID = new OptionID("itemsetmining.minlength", //
    "Minimum length of frequent itemsets to report. This can help to reduce the output size to only the most interesting patterns.");

    /**
     * Parameter to specify the maximum itemset length.
     */
    public static final OptionID MAXLENGTH_ID = new OptionID("itemsetmining.maxlength", //
    "Maximum length of frequent itemsets to report. This can help to reduce the output size to only the most interesting patterns.");

    /**
     * Parameter for minimum support.
     */
    protected double minsupp;

    /**
     * Parameter for minimum and maximum length.
     */
    protected int minlength = 0, maxlength = Integer.MAX_VALUE;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter minsuppP = new DoubleParameter(MINSUPP_ID) //
      .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
      if(config.grab(minsuppP)) {
        minsupp = minsuppP.getValue();
      }
      IntParameter minlengthP = new IntParameter(MINLENGTH_ID) //
      .setOptional(true) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minlengthP)) {
        minlength = minlengthP.getValue();
      }
      IntParameter maxlengthP = new IntParameter(MAXLENGTH_ID) //
      .setOptional(true) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(maxlengthP)) {
        maxlength = maxlengthP.getValue();
      }
    }
  }
}
