/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2026
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
package elki.sequencemining;

import elki.Algorithm;
import elki.database.Database;
import elki.result.FrequentSubsequencesResult;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Abstract base class for frequent subsequence mining.
 *
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @has - produces - FrequentSubsequencesResult
 */
public abstract class AbstractFrequentSubsequenceAlgorithm implements Algorithm {
  /**
   * Minimum support threshold (relative if <= 1, absolute otherwise).
   */
  protected final double minSuppRaw;

  /**
   * Minimum sequence length to report.
   */
  protected final int minLength;

  /**
   * Maximum sequence length to report.
   */
  protected final int maxLength;

  /**
   * Constructor.
   *
   * @param minSuppRaw Minimum support
   * @param minLength Minimum length
   * @param maxLength Maximum length
   */
  public AbstractFrequentSubsequenceAlgorithm(double minSuppRaw, int minLength, int maxLength) {
    this.minSuppRaw = minSuppRaw;
    this.minLength = minLength;
    this.maxLength = maxLength > 0 ? maxLength : Integer.MAX_VALUE;
  }

  /**
   * Convert minimum support to absolute support for the given transaction count.
   *
   * @param nTransactions Number of transactions
   * @return Absolute minimum support
   */
  protected int getMinimumSupport(int nTransactions) {
    return (int) ((minSuppRaw < 1.) ? Math.ceil(minSuppRaw * nTransactions) : minSuppRaw);
  }

  /**
   * Effective minimum sequence length.
   *
   * @return Effective minimum length
   */
  protected int getMinimumLength() {
    return Math.max(1, minLength);
  }

  @Override
  public FrequentSubsequencesResult autorun(Database database) {
    return (FrequentSubsequencesResult) Algorithm.super.autorun(database);
  }

  /**
   * Parameterization class.
   */
  public abstract static class Par implements Parameterizer {
    /**
     * Parameter to specify the minimum support, in absolute or relative terms.
     */
    public static final OptionID MINSUPP_ID = new OptionID("sequencemining.minsupp", //
        "Threshold for minimum support as minimally required number of transactions (if > 1) " //
            + "or the minimum frequency (if <= 1).");

    /**
     * Parameter to specify the minimum sequence length.
     */
    public static final OptionID MINLENGTH_ID = new OptionID("sequencemining.minlength", //
        "Minimum length of frequent sequences to report.");

    /**
     * Parameter to specify the maximum sequence length.
     */
    public static final OptionID MAXLENGTH_ID = new OptionID("sequencemining.maxlength", //
        "Maximum length of frequent sequences to report.");

    /**
     * Parameter for minimum support.
     */
    protected double minSupp = 1;

    /**
     * Parameter for minimum sequence length.
     */
    protected int minLength = 0;

    /**
     * Parameter for maximum sequence length.
     */
    protected int maxLength = Integer.MAX_VALUE;

    @Override
    public void configure(Parameterization config) {
      new DoubleParameter(MINSUPP_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> minSupp = x);
      new IntParameter(MINLENGTH_ID) //
          .setOptional(true) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> minLength = x);
      new IntParameter(MAXLENGTH_ID) //
          .setOptional(true) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> maxLength = x);
    }
  }
}
