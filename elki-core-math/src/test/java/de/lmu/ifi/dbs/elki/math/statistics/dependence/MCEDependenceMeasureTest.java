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
package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import org.junit.Test;

/**
 * Ensure basic integrity.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class MCEDependenceMeasureTest {
  @Test
  public void testBasic() {
    DependenceMeasure cor = MCEDependenceMeasure.STATIC;
    AbstractDependenceMeasureTest.checkPerfectLinear(cor, 1000, 1.0, 1.0, 1e-15);
    AbstractDependenceMeasureTest.checkUniform(cor, 1000, 1.0, 1e-15, 0.002, 0.005);
  }
}
