package de.lmu.ifi.dbs.elki.datasource.filter.transform;

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

import java.util.BitSet;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractVectorStreamConversionFilter;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ListGreaterEqualConstraint;
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
public class NumberVectorFeatureSelectionFilter<V extends NumberVector<?>> extends AbstractVectorStreamConversionFilter<V, V> {
  /**
   * Keeps the selection of the subspace to project onto.
   */
  private BitSet selectedAttributes;

  /**
   * Constructor.
   * 
   * @param selectedAttributes Selected attributes
   */
  public NumberVectorFeatureSelectionFilter(BitSet selectedAttributes) {
    super();
    this.selectedAttributes = selectedAttributes;
  }

  @Override
  protected V filterSingleObject(V obj) {
    return Util.project(obj, getSelectedAttributes(), factory);
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  protected SimpleTypeInformation<? super V> convertedType(SimpleTypeInformation<V> in) {
    initializeOutputType(in);
    return new VectorFieldTypeInformation<V>(factory, getDimensionality());
  }

  /**
   * <p>
   * Sets the bits set to true in the given BitSet as selected attributes in
   * {@link #SELECTED_ATTRIBUTES_ID}.
   * </p>
   * 
   * The index in the BitSet is expected to be shifted to the left by one, i.e.,
   * index 0 in the BitSet relates to the first attribute.
   * 
   * @param selectedAttributes the new selected attributes
   */
  public void setSelectedAttributes(BitSet selectedAttributes) {
    this.selectedAttributes.or(selectedAttributes);
  }

  /**
   * <p>
   * Provides a BitSet with the bits set to true corresponding to the selected
   * attributes in {@link #SELECTED_ATTRIBUTES_ID}.
   * </p>
   * 
   * The index in the BitSet is shifted to the left by one, i.e., index 0 in the
   * BitSet relates to the first attribute.
   * 
   * @return the selected attributes
   */
  public BitSet getSelectedAttributes() {
    return selectedAttributes;
  }

  /**
   * Get the resulting dimensionality.
   * 
   * @return dimensionality
   */
  public int getDimensionality() {
    return selectedAttributes.cardinality();
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
     * <p>
     * Selected attributes parameter.
     * </p>
     * <p>
     * Key: <code>-projectionfilter.selectedattributes</code>
     * </p>
     */
    public static final OptionID SELECTED_ATTRIBUTES_ID = OptionID.getOrCreateOptionID("projectionfilter.selectedattributes", "a comma separated array of integer values d_i, where 1 <= d_i <= the " + "dimensionality of the feature space " + "specifying the dimensions to be considered " + "for projection. If this parameter is not set, " + "no dimensions will be considered, i.e. the projection is a zero-dimensional feature space");

    /**
     * Selected attributes.
     */
    protected BitSet selectedAttributes = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntListParameter selectedAttributesP = new IntListParameter(SELECTED_ATTRIBUTES_ID, new ListGreaterEqualConstraint<Integer>(1));
      if (config.grab(selectedAttributesP)) {
        selectedAttributes = new BitSet();
        List<Integer> dimensionList = selectedAttributesP.getValue();
        for (int d : dimensionList) {
          selectedAttributes.set(d - 1);
        }
      }
    }

    @Override
    protected NumberVectorFeatureSelectionFilter<DoubleVector> makeInstance() {
      return new NumberVectorFeatureSelectionFilter<DoubleVector>(selectedAttributes);
    }
  }
}
