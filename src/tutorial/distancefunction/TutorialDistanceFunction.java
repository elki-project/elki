package tutorial.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractVectorDoubleDistanceFunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Tutorial example for ELKI.
 * 
 * See <a href="http://elki.dbs.ifi.lmu.de/wiki/Tutorial/DistanceFunctions">Distance function tutorial</a>
 * 
 * @author Erich Schubert
 */
public class TutorialDistanceFunction extends AbstractVectorDoubleDistanceFunction {
  @Override
  public double doubleDistance(NumberVector<?> o1, NumberVector<?> o2) {
    double dx = (o1.doubleValue(0) - o2.doubleValue(0));
    double dy = (o1.doubleValue(1) - o2.doubleValue(1));
    return dx * dx + Math.abs(dy);
  }

  @Override
  public SimpleTypeInformation<? super NumberVector<?>> getInputTypeRestriction() {
    return new VectorFieldTypeInformation<NumberVector<?>>(NumberVector.class, 2);
  }
}