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
package elki.datasource.filter.transform;

import elki.data.DoubleVector;
import elki.data.NumberVector;
import elki.data.VectorUtil;
import elki.data.projection.FeatureSelection;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.datasource.filter.AbstractVectorStreamConversionFilter;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntListParameter;

/**
 * Parser to project the ParsingResult obtained by a suitable base parser onto a
 * selected subset of attributes.
 * 
 * @author Arthur Zimek
 * @since 0.5.5
 * 
 * @assoc - - - NumberVector
 * 
 * @param <V> Vector type
 */
public class NumberVectorFeatureSelectionFilter<V extends NumberVector> extends AbstractVectorStreamConversionFilter<V, V> {
  /**
   * Keeps the selection of the subspace to project onto.
   */
  private long[] selectedAttributes;

  /**
   * Constructor.
   * 
   * @param selectedAttributes Selected attributes
   */
  public NumberVectorFeatureSelectionFilter(long[] selectedAttributes) {
    super();
    this.selectedAttributes = selectedAttributes;
  }

  @Override
  protected V filterSingleObject(V obj) {
    return VectorUtil.project(obj, getSelectedAttributes(), factory);
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  protected SimpleTypeInformation<? super V> convertedType(SimpleTypeInformation<V> in) {
    initializeOutputType(in);
    return new VectorFieldTypeInformation<>(factory, getDimensionality());
  }

  /**
   * Sets the bits set to true in the given BitSet as selected attributes in
   * {@link Par#SELECTED_ATTRIBUTES_ID}.
   * 
   * @param selectedAttributes the new selected attributes
   */
  public void setSelectedAttributes(long[] selectedAttributes) {
    this.selectedAttributes = selectedAttributes;
  }

  /**
   * Provides a BitSet with the bits set to true corresponding to the selected
   * attributes in {@link Par#SELECTED_ATTRIBUTES_ID}.
   * 
   * @return the selected attributes
   */
  public long[] getSelectedAttributes() {
    return selectedAttributes;
  }

  /**
   * Get the resulting dimensionality.
   * 
   * @return dimensionality
   */
  public int getDimensionality() {
    return BitsUtil.cardinality(selectedAttributes);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Selected attributes parameter.
     */
    public static final OptionID SELECTED_ATTRIBUTES_ID = FeatureSelection.Par.SELECTED_ATTRIBUTES_ID;

    /**
     * Selected attributes.
     */
    protected long[] selectedAttributes;

    @Override
    public void configure(Parameterization config) {
      new IntListParameter(SELECTED_ATTRIBUTES_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT_LIST) //
          .grab(config, x -> selectedAttributes = BitsUtil.of(x));
    }

    @Override
    public NumberVectorFeatureSelectionFilter<DoubleVector> make() {
      return new NumberVectorFeatureSelectionFilter<>(selectedAttributes);
    }
  }
}
