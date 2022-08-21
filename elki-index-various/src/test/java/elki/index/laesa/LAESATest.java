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
package elki.index.laesa;

import org.junit.Test;

import elki.distance.minkowski.EuclideanDistance;
import elki.index.AbstractIndexStructureTest;
import elki.utilities.ELKIBuilder;

/**
 * Unit test for the {@link LAESA}.
 *
 * @author Robert Gehde
 * @since 0.8.0
 */
public class LAESATest extends AbstractIndexStructureTest {
  @Test
  public void testLAESA() {
    LAESA.Factory<?> factory = new ELKIBuilder<>(LAESA.Factory.class) //
        .with(LAESA.Factory.Par.DISTANCE_FUNCTION_ID, EuclideanDistance.class) //
        .with(LAESA.Factory.Par.M_ID, 100) //
        .build();
    assertExactEuclidean(factory, LAESA.LAESAKNNSearcher.class, LAESA.LAESARangeSearcher.class);
    assertSinglePoint(factory, LAESA.LAESAKNNByDBIDSearcher.class, LAESA.LAESARangeByDBIDSearcher.class);
  }
}
