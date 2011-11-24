package de.lmu.ifi.dbs.elki.datasource.filter;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * <p>Parser to project the ParsingResult obtained by a suitable base parser
 * onto a randomly selected subset of attributes.</p>
 *
 * @author Arthur Zimek
 */
public class SparseFloatVectorRandomProjectionFilter extends AbstractRandomFeatureSelectionFilter<SparseFloatVector> {
  /**
   * Constructor.
   *
   * @param dim
   */
  public SparseFloatVectorRandomProjectionFilter(int dim) {
    super(dim);
  }

  @Override
  protected SparseFloatVector filterSingleObject(SparseFloatVector obj) {
    return Util.project(obj, selectedAttributes);
  }

  @Override
  protected SimpleTypeInformation<? super SparseFloatVector> getInputTypeRestriction() {
    return TypeUtil.SPARSE_FLOAT_FIELD;
  }

  @Override
  protected SimpleTypeInformation<? super SparseFloatVector> convertedType(SimpleTypeInformation<SparseFloatVector> in) {
    initializeRandomAttributes(in);
    return new VectorFieldTypeInformation<SparseFloatVector>(SparseFloatVector.class, k, new SparseFloatVector(SparseFloatVector.EMPTYMAP, k));
  }
  
  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractRandomFeatureSelectionFilter.Parameterizer<SparseFloatVector> {
    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
    }

    @Override
    protected SparseFloatVectorRandomProjectionFilter makeInstance() {
      return new SparseFloatVectorRandomProjectionFilter(k);
    }
  }
}