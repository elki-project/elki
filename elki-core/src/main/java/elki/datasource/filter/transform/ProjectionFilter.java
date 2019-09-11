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
package elki.datasource.filter.transform;

import elki.data.projection.Projection;
import elki.data.type.SimpleTypeInformation;
import elki.data.type.TypeInformation;
import elki.datasource.filter.AbstractStreamConversionFilter;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Apply a projection to the data.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @composed - - - Projection
 * 
 * @param <I> Input type
 * @param <O> Output type
 */
public class ProjectionFilter<I, O> extends AbstractStreamConversionFilter<I, O> {
  /**
   * Projection to apply.
   */
  Projection<I, O> projection;

  /**
   * Constructor.
   * 
   * @param projection Projection
   */
  public ProjectionFilter(Projection<I, O> projection) {
    super();
    this.projection = projection;
  }

  @Override
  protected O filterSingleObject(I obj) {
    return projection.project(obj);
  }

  @Override
  protected TypeInformation getInputTypeRestriction() {
    return projection.getInputDataTypeInformation();
  }

  @Override
  protected SimpleTypeInformation<? super O> convertedType(SimpleTypeInformation<I> in) {
    projection.initialize(in);
    return projection.getOutputDataTypeInformation();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @hidden
   * 
   * @param <I> Input type
   * @param <O> Output type
   */
  public static class Par<I, O> implements Parameterizer {
    /**
     * Parameter to specify the projection to use
     */
    public static final OptionID PROJ_ID = new OptionID("projection", "Projection to use.");

    /**
     * Projection to apply.
     */
    Projection<I, O> projection;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Projection<I, O>>(PROJ_ID, Projection.class) //
          .grab(config, x -> projection = x);
    }

    @Override
    public ProjectionFilter<I, O> make() {
      return new ProjectionFilter<>(projection);
    }
  }
}
