package de.lmu.ifi.dbs.elki.data.uncertain;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;

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

/**
 * This abstract class is derived from {@link UOModel} to
 * model Uncertain-Data-Objects with continuous distributions,
 * i.e. Uncertain-Data-Objects, where the possible values
 * aren't explicitly given but are chosen from a continuous
 * space.
 * 
 * Most likely one will use a {@link experimentalcode.students.koosa.probdensfunc.template.ProbabilityDensityFunction}
 * of some kind but I decided to implement this abstract layer
 * to give everyone the possibility to derive a custom solution
 * to this, if he likes to.
 * 
 * @author Alexander Koos
 *
 */
public abstract class AbstractContinuousUncertainObject extends UOModel<SpatialComparable> {
  
  /**
   * Since Continuous-Uncertain-Data-Objects don't
   * have a specific number of values, that could
   * be retrieved, but infinite possibilities,
   * the getWeight method defaults to {@link Integer#MAX_VALUE}.
   * 
   * @return int
   */
  @Override
  public int getWeight() {
    return Integer.MAX_VALUE;
  }
}
