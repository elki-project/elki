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

import de.lmu.ifi.dbs.elki.data.SparseFloatVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;

/**
 * Class that turns sparse float vectors into a proper vector field, by setting
 * the maximum dimensionality for each vector.
 * 
 * @author Erich Schubert
 */
public class SparseVectorFieldFilter extends AbstractConversionFilter<SparseFloatVector, SparseFloatVector> {
  /**
   * Maximum dimension
   */
  int maxdim = -1;

  /**
   * Constructor.
   */
  public SparseVectorFieldFilter() {
    super();
  }

  @Override
  protected boolean prepareStart(SimpleTypeInformation<SparseFloatVector> in) {
    return true;
  }

  @Override
  protected void prepareProcessInstance(SparseFloatVector obj) {
    maxdim = Math.max(maxdim, obj.getDimensionality());
  }

  @Override
  protected SparseFloatVector filterSingleObject(SparseFloatVector obj) {
    assert(maxdim > 0);
    obj.setDimensionality(maxdim);
    return obj;
  }

  @Override
  protected SimpleTypeInformation<? super SparseFloatVector> getInputTypeRestriction() {
    return TypeUtil.SPARSE_FLOAT_FIELD;
  }

  @Override
  protected SimpleTypeInformation<? super SparseFloatVector> convertedType(SimpleTypeInformation<SparseFloatVector> in) {
    return new VectorFieldTypeInformation<SparseFloatVector>(SparseFloatVector.class, maxdim, SparseFloatVector.STATIC);
  }
}