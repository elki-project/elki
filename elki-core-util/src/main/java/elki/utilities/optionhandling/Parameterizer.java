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
package elki.utilities.optionhandling;

import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Generic interface for a parameterizable factory.
 * <p>
 * To instantiate a class, use {@link Parameterization#tryInstantiate(Class)}!
 * 
 * @author Erich Schubert
 * @since 0.4.0
 */
public interface Parameterizer {
  /**
   * Configure the class.
   * <p>
   * Note: the status is collected by the parameterization object, so that
   * multiple errors may arise and be reported in one run.
   * 
   * @param config Parameterization
   */
  default void configure(Parameterization config) {
    // Nothing to do by default;
  }

  /**
   * Make an instance after successful configuration.
   * <p>
   * Note: your class should return the exact type, only this very broad
   * interface should use {@code Object} as return type.
   *
   * @return a new instance
   */
  Object make();
}
