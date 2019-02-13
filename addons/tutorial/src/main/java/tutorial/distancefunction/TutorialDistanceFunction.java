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
package tutorial.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;

/**
 * Tutorial distance function example for ELKI.
 * <p>
 * See <a
 * href="https://elki-project.github.io/tutorial/distance_functions">Distance
 * function tutorial</a>
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class TutorialDistanceFunction extends AbstractNumberVectorDistanceFunction {
  @Override
  public double distance(NumberVector o1, NumberVector o2) {
    double dx = o1.doubleValue(0) - o2.doubleValue(0);
    double dy = o1.doubleValue(1) - o2.doubleValue(1);
    return dx * dx + Math.abs(dy);
  }

  @Override
  public SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD_2D;
  }
}
