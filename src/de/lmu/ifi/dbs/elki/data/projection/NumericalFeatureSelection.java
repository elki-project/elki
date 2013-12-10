package de.lmu.ifi.dbs.elki.data.projection;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import java.util.BitSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.filter.transform.NumberVectorFeatureSelectionFilter;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;

/**
 * Projection class for number vectors.
 * 
 * FIXME: Use int[] instead of a BitSet, to allow reordering?
 * 
 * @author Erich Schubert
 * 
 * @param <V> Vector type
 */
public class NumericalFeatureSelection<V extends NumberVector<?>> implements Projection<V, V> {
  /**
   * Minimum dimensionality required for projection.
   */
  private int mindim;

  /**
   * Object factory.
   */
  private NumberVector.Factory<V, ?> factory;

  /**
   * Output dimensionality.
   */
  private int dimensionality;

  /**
   * Subspace.
   */
  private BitSet bits;

  /**
   * Constructor.
   * 
   * @param bits Dimensions
   */
  public NumericalFeatureSelection(BitSet bits) {
    super();
    this.bits = bits;
    this.dimensionality = bits.cardinality();

    int mind = 0;
    for(int i = bits.nextSetBit(0); i >= 0; i = bits.nextSetBit(i + 1)) {
      mind = Math.max(mind, i + 1);
    }
    this.mindim = mind;
  }

  @Override
  public void initialize(SimpleTypeInformation<V> in) {
    final VectorFieldTypeInformation<V> vin = (VectorFieldTypeInformation<V>) in;
    factory = (NumberVector.Factory<V, ?>) vin.getFactory();
    if(vin.getDimensionality() < mindim) {
      throw new AbortException("Data does not have enough dimensions for this projection!");
    }
  }

  @Override
  public V project(V data) {
    double[] dbl = new double[dimensionality];
    for(int i = bits.nextSetBit(0), j = 0; i >= 0; i = bits.nextSetBit(i + 1), j++) {
      dbl[j] = data.doubleValue(i);
    }
    return factory.newNumberVector(dbl);
  }

  @Override
  public SimpleTypeInformation<V> getOutputDataTypeInformation() {
    return new VectorFieldTypeInformation<>(factory, dimensionality);
  }

  @Override
  public TypeInformation getInputDataTypeInformation() {
    return new VectorTypeInformation<>(NumberVector.class, mindim, Integer.MAX_VALUE);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
    /**
     * Dimensions to select.
     */
    BitSet dims = new BitSet();

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      IntListParameter selectedAttributesP = new IntListParameter(NumberVectorFeatureSelectionFilter.Parameterizer.SELECTED_ATTRIBUTES_ID);
      selectedAttributesP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT_LIST);
      if(config.grab(selectedAttributesP)) {
        dims.clear();
        for(int in : selectedAttributesP.getValue()) {
          dims.set(in);
        }
      }
    }

    @Override
    protected NumericalFeatureSelection<V> makeInstance() {
      return new NumericalFeatureSelection<>(dims);
    }
  }
}
