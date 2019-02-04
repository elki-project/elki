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
package de.lmu.ifi.dbs.elki.data.projection;

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;

/**
 * Projection class for number vectors.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @assoc - - - NumberVector
 * 
 * @param <V> Vector type
 */
public class NumericalFeatureSelection<V extends NumberVector> implements Projection<V, V> {
  /**
   * Minimum dimensionality required for projection.
   */
  private int mindim;

  /**
   * Object factory.
   */
  private NumberVector.Factory<V> factory;

  /**
   * Subspace.
   */
  private int[] dims;

  /**
   * Constructor.
   * 
   * @param dims Dimensions
   */
  public NumericalFeatureSelection(int[] dims) {
    super();
    this.dims = dims;

    int mind = 0;
    for(int i : dims) {
      mind = (i > mind) ? i : mind;
    }
    this.mindim = mind + 1;
  }

  /**
   * Constructor.
   * 
   * @param bits Bitset
   */
  public NumericalFeatureSelection(BitSet bits) {
    super();
    final int card = bits.cardinality();
    this.dims = new int[card];
    int mind = 0;
    for(int i = bits.nextSetBit(0), j = 0; i >= 0; i = bits.nextSetBit(i + 1), j++) {
      dims[j] = i;
      mind = (i > mind) ? i : mind;
    }
    this.mindim = mind + 1;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void initialize(SimpleTypeInformation<? extends V> in) {
    final VectorFieldTypeInformation<V> vin = (VectorFieldTypeInformation<V>) in;
    factory = (NumberVector.Factory<V>) vin.getFactory();
    if(vin.getDimensionality() < mindim) {
      throw new AbortException("Data does not have enough dimensions for this projection!");
    }
  }

  @Override
  public V project(V data) {
    double[] dbl = new double[dims.length];
    for(int i = 0; i < dims.length; i++) {
      dbl[i] = data.doubleValue(dims[i]);
    }
    return factory.newNumberVector(dbl);
  }

  @Override
  public SimpleTypeInformation<V> getOutputDataTypeInformation() {
    return new VectorFieldTypeInformation<>(factory, dims.length);
  }

  @Override
  public TypeInformation getInputDataTypeInformation() {
    return VectorTypeInformation.typeRequest(NumberVector.class, mindim, Integer.MAX_VALUE);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Dimensions to select.
     */
    int[] dims;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntListParameter selectedAttributesP = new IntListParameter(FeatureSelection.Parameterizer.SELECTED_ATTRIBUTES_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT_LIST);
      if(config.grab(selectedAttributesP)) {
        dims = selectedAttributesP.getValue();
      }
    }

    @Override
    protected NumericalFeatureSelection<V> makeInstance() {
      return new NumericalFeatureSelection<>(dims);
    }
  }
}
