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
package de.lmu.ifi.dbs.elki.math.geodesy;

import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * The Clarke 1880 spheroid earth model.
 * <p>
 * Radius: 6378249.145 m
 * <p>
 * Flattening: 1 / 293.465
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias({ "Clarke1880" })
public class Clarke1880SpheroidEarthModel extends AbstractEarthModel {
  /**
   * Static instance.
   */
  public static final Clarke1880SpheroidEarthModel STATIC = new Clarke1880SpheroidEarthModel();

  /**
   * Radius of the CLARKE1880 Ellipsoid in m (a).
   */
  public static final double CLARKE1880_RADIUS = 6378249.145; // m

  /**
   * Inverse flattening 1/f of the CLARKE1880 Ellipsoid.
   */
  public static final double CLARKE1880_INV_FLATTENING = 293.465;

  /**
   * Flattening f of the CLARKE1880 Ellipsoid.
   */
  public static final double CLARKE1880_FLATTENING = 1 / CLARKE1880_INV_FLATTENING;

  /**
   * Constructor.
   */
  protected Clarke1880SpheroidEarthModel() {
    super(CLARKE1880_RADIUS, CLARKE1880_RADIUS * (1 - CLARKE1880_FLATTENING), CLARKE1880_FLATTENING, CLARKE1880_INV_FLATTENING);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected Clarke1880SpheroidEarthModel makeInstance() {
      return STATIC;
    }
  }
}
