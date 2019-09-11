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
package elki.datasource.filter.typeconversions;

import java.util.ArrayList;
import java.util.List;

import elki.data.NumberVector;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.data.type.VectorFieldTypeInformation;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.datasource.filter.FilterUtil;
import elki.datasource.filter.ObjectFilter;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntListParameter;

/**
 * Split an existing column into two types.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @assoc - - - NumberVector
 * 
 * @param <V> Vector type
 */
public class SplitNumberVectorFilter<V extends NumberVector> implements ObjectFilter {
  /**
   * Selected dimensions.
   */
  final int[] dims;

  /**
   * Constructor.
   * 
   * @param dims Dimensions to use.
   */
  public SplitNumberVectorFilter(int[] dims) {
    super();
    this.dims = dims;
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    if(objects.dataLength() == 0) {
      return objects;
    }
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();

    for(int r = 0; r < objects.metaLength(); r++) {
      @SuppressWarnings("unchecked")
      SimpleTypeInformation<Object> type = (SimpleTypeInformation<Object>) objects.meta(r);
      @SuppressWarnings("unchecked")
      final List<Object> column = (List<Object>) objects.getColumn(r);
      if(!getInputTypeRestriction().isAssignableFromType(type)) {
        bundle.appendColumn(type, column);
        continue;
      }
      // Should be a vector type after above test.
      @SuppressWarnings("unchecked")
      final VectorFieldTypeInformation<V> vtype = VectorFieldTypeInformation.class.cast(type);
      NumberVector.Factory<V> factory = FilterUtil.guessFactory(vtype);

      // Get the replacement type informations
      VectorFieldTypeInformation<V> type1 = new VectorFieldTypeInformation<>(factory, dims.length);
      VectorFieldTypeInformation<V> type2 = new VectorFieldTypeInformation<>(factory, vtype.getDimensionality() - dims.length);
      final List<V> col1 = new ArrayList<>(column.size());
      final List<V> col2 = new ArrayList<>(column.size());
      bundle.appendColumn(type1, col1);
      bundle.appendColumn(type2, col2);

      // Build other dimensions array.
      int[] odims = new int[vtype.getDimensionality() - dims.length];
      {
        int i = 0;
        for(int d = 0; d < vtype.getDimensionality(); d++) {
          boolean found = false;
          for(int j = 0; j < dims.length; j++) {
            if(dims[j] == d) {
              found = true;
              break;
            }
          }
          if(!found) {
            if(i >= odims.length) {
              throw new AbortException("Dimensionalities not proper!");
            }
            odims[i] = d;
            i++;
          }
        }
      }
      // Splitting scan.
      for(int i = 0; i < objects.dataLength(); i++) {
        @SuppressWarnings("unchecked")
        final V obj = (V) column.get(i);
        double[] part1 = new double[dims.length];
        double[] part2 = new double[obj.getDimensionality() - dims.length];
        for(int d = 0; d < dims.length; d++) {
          part1[d] = obj.doubleValue(dims[d]);
        }
        for(int d = 0; d < odims.length; d++) {
          part2[d] = obj.doubleValue(odims[d]);
        }
        col1.add(factory.newNumberVector(part1));
        col2.add(factory.newNumberVector(part2));
      }
    }
    return bundle;
  }

  /**
   * The input type we use.
   * 
   * @return type information
   */
  private TypeInformation getInputTypeRestriction() {
    // Find maximum dimension requested
    int m = dims[0];
    for(int i = 1; i < dims.length; i++) {
      m = Math.max(dims[i], m);
    }
    return VectorFieldTypeInformation.typeRequest(NumberVector.class, m, Integer.MAX_VALUE);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<V extends NumberVector> implements Parameterizer {
    /**
     * The parameter listing the split dimensions.
     */
    public static final OptionID SELECTED_ATTRIBUTES_ID = new OptionID("split.dims", "Dimensions to split into the first relation.");

    /**
     * Dimensions to use.
     */
    protected int[] dims;

    @Override
    public void configure(Parameterization config) {
      new IntListParameter(SELECTED_ATTRIBUTES_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT_LIST) //
          .grab(config, x -> dims = x.clone());
    }

    @Override
    public SplitNumberVectorFilter<V> make() {
      return new SplitNumberVectorFilter<>(dims);
    }
  }
}
