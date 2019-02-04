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
package de.lmu.ifi.dbs.elki.datasource.filter;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;

/**
 * Abstract base class for streaming filters that produce vectors.
 * 
 * @author Erich Schubert
 * @since 0.5.5
 * 
 * @param <I> Input type
 * @param <O> Output vector type
 */
public abstract class AbstractVectorStreamConversionFilter<I, O extends NumberVector> extends AbstractStreamConversionFilter<I, O> {
  /**
   * Number vector factory.
   */
  protected NumberVector.Factory<O> factory;

  /**
   * Initialize factory from a data type.
   * 
   * @param type Output data type information.
   */
  protected void initializeOutputType(SimpleTypeInformation<O> type) {
    factory = FilterUtil.guessFactory(type);
  }
}
