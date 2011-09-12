package de.lmu.ifi.dbs.elki.distance.distancefunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Abstract base class for the most common family of distance functions: defined
 * on number vectors and returning double values.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.uses NumberVector
 * @apiviz.has DoubleDistance
 */
public abstract class AbstractVectorDoubleDistanceFunction extends AbstractPrimitiveDistanceFunction<NumberVector<?, ?>, DoubleDistance> implements PrimitiveDoubleDistanceFunction<NumberVector<?, ?>> {
  /**
   * Constructor.
   */
  public AbstractVectorDoubleDistanceFunction() {
    super();
  }

  @Override
  public SimpleTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public final DoubleDistance distance(NumberVector<?, ?> o1, NumberVector<?, ?> o2) {
    return new DoubleDistance(doubleDistance(o1, o2));
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }
}