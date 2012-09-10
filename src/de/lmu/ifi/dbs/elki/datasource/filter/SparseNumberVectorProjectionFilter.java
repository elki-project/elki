package de.lmu.ifi.dbs.elki.datasource.filter;
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

import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.Util;

/**
 * <p>
 * Parser to project the ParsingResult obtained by a suitable base parser onto a
 * selected subset of attributes.
 * </p>
 * 
 * @author Arthur Zimek
 * 
 * @param <V> Vector type
 */
public class SparseNumberVectorProjectionFilter<V extends SparseNumberVector<?>> extends AbstractFeatureSelectionFilter<V> {
  /**
   * Object factory.
   */
  SparseNumberVector.Factory<V, ?> factory;

  /**
   * Constructor.
   * 
   * @param selectedAttributes Selected attributes
   */
  public SparseNumberVectorProjectionFilter(BitSet selectedAttributes) {
    super(selectedAttributes);
  }

  @Override
  protected V filterSingleObject(V obj) {
    return Util.project(obj, getSelectedAttributes(), factory);
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.SPARSE_VECTOR_FIELD;
  }

  @Override
  protected SimpleTypeInformation<? super V> convertedType(SimpleTypeInformation<V> in) {
    factory = (SparseNumberVector.Factory<V, ?>) FilterUtil.guessFactory(in);
    return new VectorFieldTypeInformation<V>(factory, getDimensionality());
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends SparseNumberVector<?>> extends AbstractFeatureSelectionFilter.Parameterizer<V> {
    @Override
    protected SparseNumberVectorProjectionFilter<V> makeInstance() {
      return new SparseNumberVectorProjectionFilter<V>(selectedAttributes);
    }
  }
}