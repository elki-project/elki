/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ParameterFlagGlobalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;

/**
 * The LimitEigenPairFilter marks all eigenpairs having an (absolute) eigenvalue
 * below the specified threshold (relative or absolute) as weak eigenpairs, the
 * others are marked as strong eigenpairs.
 * 
 * @author Elke Achtert
 * @since 0.2
 */
@Title("Limit-based Eigenpair Filter")
@Description("Filters all eigenvalues, which are lower than a given value.")
public class LimitEigenPairFilter implements EigenPairFilter {
  /**
   * The default value for delta.
   */
  public static final double DEFAULT_DELTA = 0.01;

  /**
   * Threshold for strong eigenpairs, can be absolute or relative.
   */
  private double delta;

  /**
   * Indicates whether delta is an absolute or a relative value.
   */
  private boolean absolute;

  /**
   * Constructor.
   * 
   * @param delta
   * @param absolute
   */
  public LimitEigenPairFilter(double delta, boolean absolute) {
    super();
    this.delta = delta;
    this.absolute = absolute;
  }

  @Override
  public int filter(double[] eigenValues) {
    // determine limit
    double limit = absolute ? delta : eigenValues[0] * delta;

    // determine strong and weak eigenpairs
    for(int i = 0; i < eigenValues.length; i++) {
      double eigenValue = Math.abs(eigenValues[i]);
      if(eigenValue < limit) {
        return i;
      }
    }
    // By default, keep all.
    return eigenValues.length;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * "absolute" Flag
     */
    public static final OptionID EIGENPAIR_FILTER_ABSOLUTE = new OptionID("pca.filter.absolute", "Flag to mark delta as an absolute value.");

    /**
     * Parameter delta
     */
    public static final OptionID EIGENPAIR_FILTER_DELTA = new OptionID("pca.filter.delta", "The threshold for strong Eigenvalues. If not otherwise specified, delta " + "is a relative value w.r.t. the (absolute) highest Eigenvalues and has to be " + "a double between 0 and 1. To mark delta as an absolute value, use " + "the option -" + EIGENPAIR_FILTER_ABSOLUTE.getName() + ".");

    /**
     * Threshold for strong eigenpairs, can be absolute or relative.
     */
    private double delta;

    /**
     * Indicates whether delta is an absolute or a relative value.
     */
    private boolean absolute;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      Flag absoluteF = new Flag(EIGENPAIR_FILTER_ABSOLUTE);
      if(config.grab(absoluteF)) {
        absolute = absoluteF.isTrue();
      }

      DoubleParameter deltaP = new DoubleParameter(EIGENPAIR_FILTER_DELTA, DEFAULT_DELTA) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(deltaP)) {
        delta = deltaP.doubleValue();
        // TODO: make this a global constraint?
        if(absolute && deltaP.tookDefaultValue()) {
          config.reportError(new WrongParameterValueException("Illegal parameter setting: " + "Flag " + absoluteF.getName() + " is set, " + "but no value for " + deltaP.getName() + " is specified."));
        }
      }

      // Conditional Constraint:
      // delta must be >= 0 and <= 1 if it's a relative value
      // Since relative or absolute is dependent on the absolute flag this is a
      // global constraint!
      List<ParameterConstraint<? super Double>> cons = new ArrayList<>();
      // TODO: Keep the constraint here - applies to non-conditional case as
      // well, and is set above.
      cons.add(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      cons.add(CommonConstraints.LESS_EQUAL_ONE_DOUBLE);

      GlobalParameterConstraint gpc = new ParameterFlagGlobalConstraint<>(deltaP, cons, absoluteF, false);
      config.checkConstraint(gpc);
    }

    @Override
    protected LimitEigenPairFilter makeInstance() {
      return new LimitEigenPairFilter(delta, absolute);
    }
  }
}
