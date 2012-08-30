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

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.Util;

/**
 * <p>Parser to project the ParsingResult obtained by a suitable base parser
 * onto a selected subset of attributes.</p>
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.uses DoubleVector
 */
public class DoubleVectorProjectionFilter extends AbstractFeatureSelectionFilter<DoubleVector> {
  /**
   * Constructor.
   *
   * @param selectedAttributes
   */
  public DoubleVectorProjectionFilter(BitSet selectedAttributes) {
    super(selectedAttributes);
  }
  
  @Override
  protected DoubleVector filterSingleObject(DoubleVector obj) {
    return Util.project(obj, getSelectedAttributes());
  }

  @Override
  protected SimpleTypeInformation<? super DoubleVector> getInputTypeRestriction() {
    return TypeUtil.DOUBLE_VECTOR_FIELD;
  }

  @Override
  protected SimpleTypeInformation<? super DoubleVector> convertedType(SimpleTypeInformation<DoubleVector> in) {
    return new VectorFieldTypeInformation<DoubleVector>(DoubleVector.class, DoubleVector.STATIC, getDimensionality(), DoubleVector.STATIC);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractFeatureSelectionFilter.Parameterizer<DoubleVector> {
    @Override
    protected DoubleVectorProjectionFilter makeInstance() {
      return new DoubleVectorProjectionFilter(selectedAttributes);
    }
  }
}