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
package de.lmu.ifi.dbs.elki.datasource.filter.cleaning;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractStreamFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.FilterUtil;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Filter to remove all vectors that do not have the desired dimensionality.
 *
 * @author Erich Schubert
 * @since 0.7.0
 *
 * @param <V> Vector type
 */
public class VectorDimensionalityFilter<V extends NumberVector> extends AbstractStreamFilter {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(VectorDimensionalityFilter.class);

  /**
   * The filtered meta.
   */
  BundleMeta meta;

  /**
   * The column to filter.
   */
  int column = -1;

  /**
   * Desired dimensionality.
   */
  int dim = -1;

  /**
   * Constructor.
   *
   * @param dim Dimensionality to enforce (use -1 to use the dimensionality of
   *        the first vector in the data set)
   */
  public VectorDimensionalityFilter(int dim) {
    super();
    this.dim = dim;
  }

  @Override
  public BundleMeta getMeta() {
    if(meta == null) {
      updateMeta();
    }
    return source.getMeta();
  }

  @Override
  public Object data(int rnum) {
    return source.data(rnum);
  }

  @Override
  public Event nextEvent() {
    while(true) {
      Event ev = source.nextEvent();
      switch(ev){
      case END_OF_STREAM:
        return ev;
      case META_CHANGED:
        meta = null;
        return ev;
      case NEXT_OBJECT:
        if(meta == null) {
          updateMeta();
        }
        if(column >= 0 && dim >= 0) {
          @SuppressWarnings("unchecked")
          V vec = (V) source.data(column);
          if(vec == null) {
            if(LOG.isVeryVerbose()) {
              LOG.veryverbose("Skipping null vector.");
            }
            continue;
          }
          if(vec.getDimensionality() != dim) {
            if(LOG.isVeryVerbose()) {
              StringBuilder buf = new StringBuilder(1000) //
                  .append("Skipping vector of wrong dimensionality ") //
                  .append(vec.getDimensionality()).append(':');
              for(int i = 0; i < meta.size(); i++) {
                buf.append(' ').append(source.data(i));
              }
              LOG.veryverbose(buf.toString());
            }
            continue;
          }
        }
        return ev;
      }
    }
  }

  /**
   * Update metadata.
   */
  private void updateMeta() {
    meta = new BundleMeta();
    BundleMeta origmeta = source.getMeta();
    for(int i = 0; i < origmeta.size(); i++) {
      SimpleTypeInformation<?> type = origmeta.get(i);
      if(column < 0) {
        // Test whether this type matches
        if(TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH.isAssignableFromType(type)) {
          if(type instanceof VectorFieldTypeInformation) {
            @SuppressWarnings("unchecked")
            final VectorFieldTypeInformation<V> castType = (VectorFieldTypeInformation<V>) type;
            if(dim != -1 && castType.mindim() > dim) {
              throw new AbortException("Would filter all vectors: minimum dimensionality " + castType.mindim() + " > desired dimensionality " + dim);
            }
            if(dim != -1 && castType.maxdim() < dim) {
              throw new AbortException("Would filter all vectors: maximum dimensionality " + castType.maxdim() + " < desired dimensionality " + dim);
            }
            if(dim == -1) {
              dim = castType.mindim();
            }
            if(castType.mindim() == castType.maxdim()) {
              meta.add(castType);
              column = i;
              continue;
            }
          }
          @SuppressWarnings("unchecked")
          final VectorTypeInformation<V> castType = (VectorTypeInformation<V>) type;
          if(dim != -1) {
            meta.add(new VectorFieldTypeInformation<>(FilterUtil.guessFactory(castType), dim, dim, castType.getSerializer()));
          }
          else {
            LOG.warning("No dimensionality yet for column " + i);
            meta.add(castType);
          }
          column = i;
          continue;
        }
      }
      meta.add(type);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @hidden
   *
   * @param <V> Vector type
   */
  public static class Parameterizer<V extends NumberVector> extends AbstractParameterizer {
    /**
     * Parameter for specifying the dimensionality.
     */
    public static final OptionID DIM_P = new OptionID("filter.dim", "Dimensionality of vectors to retain.");

    /**
     * Desired dimensionality.
     */
    int dim = -1;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter dimP = new IntParameter(DIM_P)//
          .setOptional(true)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      dim = config.grab(dimP) ? dimP.intValue() : -1;
    }

    @Override
    protected VectorDimensionalityFilter<V> makeInstance() {
      return new VectorDimensionalityFilter<>(dim);
    }
  }
}
