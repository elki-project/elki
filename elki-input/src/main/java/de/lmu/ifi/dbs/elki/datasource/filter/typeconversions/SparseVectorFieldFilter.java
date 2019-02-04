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
package de.lmu.ifi.dbs.elki.datasource.filter.typeconversions;

import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractConversionFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.FilterUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Alias;

/**
 * Class that turns sparse float vectors into a proper vector field, by setting
 * the maximum dimensionality for each vector.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 * 
 * @param <V> Vector type
 */
@Alias({ "de.lmu.ifi.dbs.elki.datasource.filter.SparseVectorFieldFilter" })
public class SparseVectorFieldFilter<V extends SparseNumberVector> extends AbstractConversionFilter<V, V> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(SparseVectorFieldFilter.class);

  /**
   * Maximum dimension.
   */
  int maxdim = -1;

  /**
   * Constructor.
   */
  public SparseVectorFieldFilter() {
    super();
  }

  @Override
  protected boolean prepareStart(SimpleTypeInformation<V> in) {
    return true;
  }

  @Override
  protected void prepareProcessInstance(V obj) {
    maxdim = Math.max(maxdim, obj.getDimensionality());
  }

  @Override
  protected V filterSingleObject(V obj) {
    assert (maxdim > 0);
    obj.setDimensionality(maxdim);
    return obj;
  }

  @Override
  protected SimpleTypeInformation<? super V> getInputTypeRestriction() {
    return TypeUtil.SPARSE_VECTOR_VARIABLE_LENGTH;
  }

  @Override
  protected SimpleTypeInformation<? super V> convertedType(SimpleTypeInformation<V> in) {
    SparseNumberVector.Factory<V> factory = (SparseNumberVector.Factory<V>) FilterUtil.guessFactory(in);
    return new VectorFieldTypeInformation<>(factory, maxdim);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }
}
