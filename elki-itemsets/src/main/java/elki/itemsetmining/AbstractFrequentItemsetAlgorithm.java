/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.itemsetmining;

import elki.Algorithm;
import elki.database.Database;
import elki.result.FrequentItemsetsResult;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Abstract base class for frequent itemset mining.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @has - produces - FrequentItemsetsResult
 */
public abstract class AbstractFrequentItemsetAlgorithm implements Algorithm {
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

  @Override
  public FrequentItemsetsResult autorun(Database database) {
    return (FrequentItemsetsResult) Algorithm.super.autorun(database);
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
  public abstract static class Par implements Parameterizer {
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
    public void configure(Parameterization config) {
      new DoubleParameter(MINSUPP_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> minsupp = x);
      new IntParameter(MINLENGTH_ID) //
          .setOptional(true) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> minlength = x);
      new IntParameter(MAXLENGTH_ID) //
          .setOptional(true) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> maxlength = x);
    }
  }
}
