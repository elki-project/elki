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
 * The WGS84 spheroid earth model, without height model (so not a geoid, just a
 * spheroid!)
 * <p>
 * Note that EGM96 uses the same spheroid, but what really makes the difference
 * is its geoid expansion.
 * <p>
 * Radius: 6378137.0 m
 * <p>
 * Flattening: 1 / 298.257223563
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @opt nodefillcolor LemonChiffon
 */
@Alias({ "WGS-84", "WGS84" })
public class WGS84SpheroidEarthModel extends AbstractEarthModel {
  /**
   * Static instance.
   */
  public static final WGS84SpheroidEarthModel STATIC = new WGS84SpheroidEarthModel();

  /**
   * Radius of the WGS84 Ellipsoid in m (a).
   */
  public static final double WGS84_RADIUS = 6378137.0; // m

  /**
   * Inverse flattening 1/f of the WGS84 Ellipsoid.
   */
  public static final double WGS84_INV_FLATTENING = 298.257223563;

  /**
   * Flattening f of the WGS84 Ellipsoid.
   */
  public static final double WGS84_FLATTENING = 1 / WGS84_INV_FLATTENING;

  /**
   * Constructor.
   */
  protected WGS84SpheroidEarthModel() {
    super(WGS84_RADIUS, WGS84_RADIUS * (1 - WGS84_FLATTENING), WGS84_FLATTENING, WGS84_INV_FLATTENING);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected WGS84SpheroidEarthModel makeInstance() {
      return STATIC;
    }
  }
}
