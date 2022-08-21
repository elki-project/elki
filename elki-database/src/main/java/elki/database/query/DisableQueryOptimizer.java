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
package elki.database.query;

import elki.utilities.Alias;
import elki.utilities.Priority;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Dummy implementation to <i>disable</i> automatic optimization.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
@Priority(Priority.SUPPLEMENTARY)
@Alias({ "no", "none", "false", "disable", "disabled" })
public class DisableQueryOptimizer implements QueryOptimizer {
  /**
   * Public static instance.
   */
  public static final DisableQueryOptimizer STATIC = new DisableQueryOptimizer();

  /**
   * Constructor, use {@link #STATIC} instead.
   */
  public DisableQueryOptimizer() {
    super();
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public DisableQueryOptimizer make() {
      return STATIC;
    }
  }
}
