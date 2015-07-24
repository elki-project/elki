package de.lmu.ifi.dbs.elki.data.uncertain;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

import java.util.ArrayList;
import java.util.List;

/**
 * This abstract class is derived from {@link UOModel} to model
 * Uncertain-Data-Objects with discrete distributions, i.e.
 * Uncertain-Data-Objects where the possible values are explicitly given.
 *
 * The parameter is a List of some kind, which holds the possible values of the
 * particular Uncertain-Data-Object.
 *
 * The way those values are displayed shall be of concern for the particular
 * author, but still we assume some kind of vector to be the most convenient
 * choice.
 *
 * @author Alexander Koos
 *
 * @param <T>
 */
public abstract class AbstractDiscreteUncertainObject<T extends List<?>> extends UOModel {
  protected T samplePoints;

  public abstract double getSampleProbability(final int position);

  /**
   * Returns the weight, i.e. the number of different possible values, of the
   * particular Uncertain-Data-Object.
   *
   * @return int
   */
  @Override
  public int getWeight() {
    return this.samplePoints.size();
  }

  public T getObservationsReference() {
    return this.samplePoints;
  }

  @SuppressWarnings({ "unchecked", "serial" })
  public T getObservationsCopy() {
    return (T) (new ArrayList<Object>() {
      {
        this.addAll(AbstractDiscreteUncertainObject.this.samplePoints);
      }
    });
  }
}
