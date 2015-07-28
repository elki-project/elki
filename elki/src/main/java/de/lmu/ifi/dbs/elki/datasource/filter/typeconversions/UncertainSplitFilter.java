package de.lmu.ifi.dbs.elki.datasource.filter.typeconversions;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.data.uncertain.UniformDiscreteUO;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractConversionFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Filter to transform a single vector into a set of samples to interpret as
 * uncertain observation.
 *
 * @author Erich Schubert
 */
public class UncertainSplitFilter extends AbstractConversionFilter<NumberVector, UncertainObject<UniformDiscreteUO>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(UncertainSplitFilter.class);

  /**
   * Data dimensionality.
   */
  private int dims;

  /**
   * Constructor.
   *
   * @param dims Number of dimensions
   */
  public UncertainSplitFilter(int dims) {
    this.dims = dims;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  protected UncertainObject<UniformDiscreteUO> filterSingleObject(NumberVector vec) {
    if(vec.getDimensionality() % dims != 0) {
      throw new AbortException("Vector length " + vec.getDimensionality() + " not divisible by the number of dimensions " + dims);
    }
    final List<DoubleVector> sampleList = new ArrayList<DoubleVector>();
    final double[] val = new double[dims];
    for(int i = 0, j = 0; i < vec.getDimensionality(); i++) {
      val[j++] = vec.doubleValue(i);
      if(j == dims) {
        sampleList.add(new DoubleVector(val));
        j = 0;
      }
    }
    return new UncertainObject<UniformDiscreteUO>(new UniformDiscreteUO(sampleList, RandomFactory.DEFAULT), vec.getColumnVector());
  }

  @Override
  protected SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  protected SimpleTypeInformation<UncertainObject<UniformDiscreteUO>> convertedType(SimpleTypeInformation<NumberVector> in) {
    final int dim = ((VectorFieldTypeInformation<NumberVector>) in).getDimensionality();
    if(dim % dims != 0) {
      throw new AbortException("Vector length " + dim + " not divisible by the number of dimensions " + dims);
    }
    return VectorFieldTypeInformation.typeRequest(UncertainObject.class, dims, dims);
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
     * Parameter for specifying the number of dimensions of the sample.
     */
    public static final OptionID DIM_ID = new OptionID("uncertain.dimensionality", "Dimensionality of the data set (used for splitting).");

    /**
     * Field to hold the dimensional constraint.
     */
    protected int dims;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter dimsP = new IntParameter(DIM_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(dimsP)) {
        dims = dimsP.intValue();
      }
    }

    @Override
    protected UncertainSplitFilter makeInstance() {
      return new UncertainSplitFilter(dims);
    }
  }
}
