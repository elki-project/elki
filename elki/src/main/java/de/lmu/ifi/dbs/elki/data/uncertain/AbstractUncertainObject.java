package de.lmu.ifi.dbs.elki.data.uncertain;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import java.util.Random;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

/**
 * Abstract base implementation for {@link UncertainObject}s, providing shared
 * functionality such as bounding box access and random generation.
 *
 * @author Alexander Koos
 * @author Erich Schubert
 */
public abstract class AbstractUncertainObject implements UncertainObject {
  /**
   * Default retry limit for sampling, to guard against bad parameters.
   */
  public final static int DEFAULT_TRY_LIMIT = 1000;

  /**
   * Bounding box of the object.
   */
  protected SpatialComparable bounds;

  @Override
  public abstract DoubleVector drawSample(Random rand);

  @Override
  public int getDimensionality() {
    return bounds.getDimensionality();
  }

  @Override
  public double getMin(final int dimension) {
    return bounds.getMin(dimension);
  }

  @Override
  public double getMax(final int dimension) {
    return bounds.getMax(dimension);
  }

  @Override
  public Double getValue(int dimension) {
    // Center of bounding box.
    // Note: currently not used, part of the FeatureVector<Double> API.
    // But we are currently not implementing NumberVector!
    return (bounds.getMax(dimension) + bounds.getMin(dimension)) * .5;
  }

  @Override
  public abstract DoubleVector getCenterOfMass();
}
