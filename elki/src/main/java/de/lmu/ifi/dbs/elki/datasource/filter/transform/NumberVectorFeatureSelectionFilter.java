package de.lmu.ifi.dbs.elki.datasource.filter.transform;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractVectorStreamConversionFilter;
import de.lmu.ifi.dbs.elki.utilities.BitsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;

/**
 * <p>
 * Parser to project the ParsingResult obtained by a suitable base parser onto a
 * selected subset of attributes.
 * </p>
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.uses NumberVector
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
   * <p>
   * Sets the bits set to true in the given BitSet as selected attributes in
   * {@link Parameterizer#SELECTED_ATTRIBUTES_ID}.
   * </p>
   * 
   * @param selectedAttributes the new selected attributes
   */
  public void setSelectedAttributes(long[] selectedAttributes) {
    this.selectedAttributes = selectedAttributes;
  }

  /**
   * Provides a BitSet with the bits set to true corresponding to the selected
   * attributes in {@link Parameterizer#SELECTED_ATTRIBUTES_ID}.
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Selected attributes parameter.
     * <p>
     * Key: <code>-projectionfilter.selectedattributes</code>
     * </p>
     */
    public static final OptionID SELECTED_ATTRIBUTES_ID = new OptionID("projectionfilter.selectedattributes", "a comma separated array of integer values d_i, where 0 <= d_i < the dimensionality of the feature space specifying the dimensions to be considered for projection. If this parameter is not set, no dimensions will be considered, i.e. the projection is a zero-dimensional feature space");

    /**
     * Selected attributes.
     */
    protected long[] selectedAttributes;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntListParameter selectedAttributesP = new IntListParameter(SELECTED_ATTRIBUTES_ID) //
      .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT_LIST);
      if(config.grab(selectedAttributesP)) {
        selectedAttributes = selectedAttributesP.getValueAsBitSet();
      }
    }

    @Override
    protected NumberVectorFeatureSelectionFilter<DoubleVector> makeInstance() {
      return new NumberVectorFeatureSelectionFilter<>(selectedAttributes);
    }
  }
}
