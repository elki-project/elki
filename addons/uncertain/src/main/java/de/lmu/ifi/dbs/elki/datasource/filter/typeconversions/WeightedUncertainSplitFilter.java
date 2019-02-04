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
package de.lmu.ifi.dbs.elki.datasource.filter.typeconversions;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.uncertain.WeightedDiscreteUncertainObject;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractConversionFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.LessConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Filter to transform a single vector into a set of samples and weights to
 * interpret as uncertain observation.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class WeightedUncertainSplitFilter extends AbstractConversionFilter<NumberVector, WeightedDiscreteUncertainObject> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(WeightedUncertainSplitFilter.class);

  /**
   * Data dimensionality.
   */
  protected int dims;

  /**
   * Modulo value for splitting.
   */
  protected int mod;

  /**
   * Column in which the probability is stored.
   */
  protected int probcol;

  /**
   * Constructor.
   *
   * @param dims Number of dimensions
   * @param probcol Column in which probabilities are stored.
   */
  public WeightedUncertainSplitFilter(int dims, int probcol) {
    this.dims = dims;
    if(probcol < -dims || probcol >= dims) {
      throw new AbortException("Invalid column given for prob: " + probcol);
    }
    else if(probcol < 0) {
      probcol += dims;
    }
    this.probcol = probcol;
    this.mod = dims + 1;
  }

  @Override
  protected WeightedDiscreteUncertainObject filterSingleObject(NumberVector vec) {
    final int dim = vec.getDimensionality();
    if(dim % mod != 0) {
      throw new AbortException("Vector length " + dim + " not divisible by the number of dimensions + 1 (for probability): " + mod);
    }
    final int num = dim / mod;
    final DoubleVector[] samples = new DoubleVector[num];
    final double[] weights = new double[dims];
    final double[] buf = new double[dims];
    for(int i = 0, j = 0, k = 0, l = 0; i < mod; i++) {
      if(l++ == probcol) {
        weights[k] = vec.doubleValue(i);
      }
      else {
        buf[j++] = vec.doubleValue(i);
      }
      if(l == mod) {
        samples[k] = DoubleVector.copy(buf);
        j = 0;
        l = 0;
        k++;
      }
    }
    return new WeightedDiscreteUncertainObject(samples, weights);
  }

  @Override
  protected SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  protected SimpleTypeInformation<WeightedDiscreteUncertainObject> convertedType(SimpleTypeInformation<NumberVector> in) {
    final int dim = ((VectorFieldTypeInformation<NumberVector>) in).getDimensionality();
    if(dim % mod != 0) {
      throw new AbortException("Vector length " + dim + " not divisible by the number of dimensions + 1 (for probability): " + mod);
    }
    return new VectorFieldTypeInformation<WeightedDiscreteUncertainObject>(WeightedDiscreteUncertainObject.FACTORY, dim);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Parameter for specifying the number of dimensions of the sample.
     */
    public static final OptionID DIM_ID = new OptionID("uncertain.dimensionality", "Dimensionality of the data set (used for splitting).");

    /**
     * Parameter to specify where the probability is stored.
     */
    public static final OptionID PROBCOL_ID = new OptionID("uncertain.probability.column", "Column in which the probability is stored, starting at 0. -1 is the last column.");

    /**
     * Field to hold the dimensional constraint.
     */
    protected int dims = 0;

    /**
     * Column in which the probability is stored.
     */
    protected int probcol = Integer.MIN_VALUE;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter dimsP = new IntParameter(DIM_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      IntParameter probP = new IntParameter(PROBCOL_ID);

      if(config.grab(dimsP)) {
        dims = dimsP.intValue();
        // Add additional constraints:
        probP.addConstraint(new LessConstraint(dimsP.intValue())) //
            .addConstraint(new GreaterEqualConstraint(-dimsP.intValue()));
      }
      if(config.grab(probP)) {
        probcol = probP.intValue();
      }
    }

    @Override
    protected WeightedUncertainSplitFilter makeInstance() {
      return new WeightedUncertainSplitFilter(dims, probcol);
    }
  }
}
