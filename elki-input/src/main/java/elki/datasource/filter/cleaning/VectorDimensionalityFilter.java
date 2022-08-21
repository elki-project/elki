/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.datasource.filter.cleaning;

import elki.data.NumberVector;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeUtil;
import elki.data.type.VectorFieldTypeInformation;
import elki.data.type.VectorTypeInformation;
import elki.datasource.bundle.BundleMeta;
import elki.datasource.filter.AbstractStreamFilter;
import elki.datasource.filter.FilterUtil;
import elki.logging.Logging;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;

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
    return meta;
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
      if(column < 0 || i == column) {
        // Test whether this type matches
        if(TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH.isAssignableFromType(type)) {
          if(type instanceof VectorFieldTypeInformation) {
            @SuppressWarnings("unchecked")
            final VectorFieldTypeInformation<V> castType = (VectorFieldTypeInformation<V>) type;
            if(dim == -1) {
              dim = castType.mindim();
            }
            if(castType.mindim() == dim && castType.maxdim() == dim) {
              meta.add(castType); // Can use this without changing
              column = i;
              continue;
            }
          }
          @SuppressWarnings("unchecked")
          final VectorTypeInformation<V> castType = (VectorTypeInformation<V>) type;
          if(dim == -1) {
            dim = castType.mindim();
          }
          meta.add(new VectorFieldTypeInformation<>(FilterUtil.guessFactory(castType), dim, dim, castType.getSerializer()));
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
  public static class Par<V extends NumberVector> implements Parameterizer {
    /**
     * Parameter for specifying the dimensionality.
     */
    public static final OptionID DIM_P = new OptionID("filter.dim", "Dimensionality of vectors to retain.");

    /**
     * Desired dimensionality.
     */
    int dim = -1;

    @Override
    public void configure(Parameterization config) {
      new IntParameter(DIM_P)//
          .setOptional(true)//
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> dim = x);
    }

    @Override
    public VectorDimensionalityFilter<V> make() {
      return new VectorDimensionalityFilter<>(dim);
    }
  }
}
