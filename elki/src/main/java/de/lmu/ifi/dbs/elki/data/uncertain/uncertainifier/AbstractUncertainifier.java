package de.lmu.ifi.dbs.elki.data.uncertain.uncertainifier;
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

import de.lmu.ifi.dbs.elki.data.uncertain.UncertainObject;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Vector factory for uncertain objects.
 *
 * @author Erich Schubert
 *
 * @param <UO> Object type
 */
public abstract class AbstractUncertainifier<UO extends UncertainObject> implements Uncertainifier<UO> {
  /**
   * Constructor.
   */
  public AbstractUncertainifier() {
    super();
  }

  /**
   * Generate a bounding box for sampling.
   *
   * @param dim Dimensionality
   * @param minDev Minimum deviation
   * @param maxDev Maximum deviation
   * @param symmetric Generate a symmetric distribution
   * @param drand Random generator
   * @return Offsets and ranges.
   */
  protected static double[] generateRandomRange(int dim, double minDev, double maxDev, boolean symmetric, Random drand) {
    double[] offrange = new double[dim << 1];
    if(symmetric) {
      for(int i = 0, j = 0; i < dim; ++i) {
        double off = -1 * (drand.nextDouble() * (maxDev - minDev) + minDev);
        offrange[j++] = off;
        offrange[j++] = -2 * off;
      }
    }
    else {
      for(int i = 0, j = 0; i < dim; ++i) {
        double off = -1 * (drand.nextDouble() * (maxDev - minDev) + minDev);
        double range = (drand.nextDouble() * (maxDev - minDev) + minDev) - /* negative: */ off;
        offrange[j++] = off;
        offrange[j++] = range;
      }
    }
    return offrange;
  }

  @Override
  public abstract <A> UO newFeatureVector(A array, NumberArrayAdapter<?, A> adapter);

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public abstract static class Parameterizer extends AbstractParameterizer {
    public static final OptionID DEV_MIN_ID = new OptionID("uo.uncertainty.min", "Minimum width of uncertain region.");

    public static final OptionID DEV_MAX_ID = new OptionID("uo.uncertainty.max", "Maximum width of uncertain region.");

    public static final OptionID SEED_ID = new OptionID("uo.seed", "Seed for uncertainification.");

    public static final OptionID SYMMETRIC_ID = new OptionID("uo.symmetric", "Only generate symmetric distributions based on the seed data.");

    @Override
    abstract protected AbstractUncertainifier<?> makeInstance();
  }
}