package de.lmu.ifi.dbs.elki.math.linearalgebra.pca;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.EigenPair;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SortedEigenPairs;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessEqualConstraint;
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
 */
@Title("Limit-based Eigenpair Filter")
@Description("Filters all eigenpairs, which are lower than a given value.")
public class LimitEigenPairFilter implements EigenPairFilter {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(LimitEigenPairFilter.class);

  /**
   * "absolute" Flag
   */
  public static final OptionID EIGENPAIR_FILTER_ABSOLUTE = OptionID.getOrCreateOptionID("pca.filter.absolute", "Flag to mark delta as an absolute value.");

  /**
   * Parameter delta
   */
  public static final OptionID EIGENPAIR_FILTER_DELTA = OptionID.getOrCreateOptionID("pca.filter.delta", "The threshold for strong Eigenvalues. If not otherwise specified, delta " + "is a relative value w.r.t. the (absolute) highest Eigenvalues and has to be " + "a double between 0 and 1. To mark delta as an absolute value, use " + "the option -" + EIGENPAIR_FILTER_ABSOLUTE.getName() + ".");

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
  public FilteredEigenPairs filter(SortedEigenPairs eigenPairs) {
    StringBuffer msg = new StringBuffer();
    if(logger.isDebugging()) {
      msg.append("delta = ").append(delta);
    }

    // determine limit
    double limit;
    if(absolute) {
      limit = delta;
    }
    else {
      double max = Double.NEGATIVE_INFINITY;
      for(int i = 0; i < eigenPairs.size(); i++) {
        EigenPair eigenPair = eigenPairs.getEigenPair(i);
        double eigenValue = Math.abs(eigenPair.getEigenvalue());
        if(max < eigenValue) {
          max = eigenValue;
        }
      }
      limit = max * delta;
    }
    if(logger.isDebugging()) {
      msg.append("\nlimit = ").append(limit);
    }

    // init strong and weak eigenpairs
    List<EigenPair> strongEigenPairs = new ArrayList<EigenPair>();
    List<EigenPair> weakEigenPairs = new ArrayList<EigenPair>();

    // determine strong and weak eigenpairs
    for(int i = 0; i < eigenPairs.size(); i++) {
      EigenPair eigenPair = eigenPairs.getEigenPair(i);
      double eigenValue = Math.abs(eigenPair.getEigenvalue());
      if(eigenValue >= limit) {
        strongEigenPairs.add(eigenPair);
      }
      else {
        weakEigenPairs.add(eigenPair);
      }
    }
    if(logger.isDebugging()) {
      msg.append("\nstrong EigenPairs = ").append(strongEigenPairs);
      msg.append("\nweak EigenPairs = ").append(weakEigenPairs);
      logger.debugFine(msg.toString());
    }

    return new FilteredEigenPairs(weakEigenPairs, strongEigenPairs);
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
        absolute = absoluteF.getValue();
      }

      DoubleParameter deltaP = new DoubleParameter(EIGENPAIR_FILTER_DELTA, new GreaterEqualConstraint(0), DEFAULT_DELTA);
      if(config.grab(deltaP)) {
        delta = deltaP.getValue();
        // TODO: make this a global constraint?
        if(absolute && deltaP.tookDefaultValue()) {
          config.reportError(new WrongParameterValueException("Illegal parameter setting: " + "Flag " + absoluteF.getName() + " is set, " + "but no value for " + deltaP.getName() + " is specified."));
        }
      }

      // Conditional Constraint:
      // delta must be >= 0 and <= 1 if it's a relative value
      // Since relative or absolute is dependent on the absolute flag this is a
      // global constraint!
      List<ParameterConstraint<Number>> cons = new Vector<ParameterConstraint<Number>>();
      // TODO: Keep the constraint here - applies to non-conditional case as
      // well,
      // and is set above.
      ParameterConstraint<Number> aboveNull = new GreaterEqualConstraint(0);
      cons.add(aboveNull);
      ParameterConstraint<Number> underOne = new LessEqualConstraint(1);
      cons.add(underOne);

      GlobalParameterConstraint gpc = new ParameterFlagGlobalConstraint<Number, Double>(deltaP, cons, absoluteF, false);
      config.checkConstraint(gpc);
    }

    @Override
    protected LimitEigenPairFilter makeInstance() {
      return new LimitEigenPairFilter(delta, absolute);
    }
  }
}