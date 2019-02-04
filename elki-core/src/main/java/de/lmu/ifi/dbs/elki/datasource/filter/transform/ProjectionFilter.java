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
package de.lmu.ifi.dbs.elki.datasource.filter.transform;

import de.lmu.ifi.dbs.elki.data.projection.Projection;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.datasource.filter.AbstractStreamConversionFilter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

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
  public static class Parameterizer<I, O> extends AbstractParameterizer {
    /**
     * Parameter to specify the projection to use
     */
    public static final OptionID PROJ_ID = new OptionID("projection", "Projection to use.");

    /**
     * Projection to apply.
     */
    Projection<I, O> projection;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<Projection<I, O>> projP = new ObjectParameter<>(PROJ_ID, Projection.class);
      if(config.grab(projP)) {
        projection = projP.instantiateClass(config);
      }
    }

    @Override
    protected ProjectionFilter<I, O> makeInstance() {
      return new ProjectionFilter<>(projection);
    }
  }
}
