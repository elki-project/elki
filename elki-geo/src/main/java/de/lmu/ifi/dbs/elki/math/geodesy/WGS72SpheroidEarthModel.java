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
 * The WGS72 spheroid earth model, without height model.
 * <p>
 * Radius: 6378135.0 m
 * <p>
 * Flattening: 1 / 298.26
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias({ "WGS72", "WGS-72" })
public class WGS72SpheroidEarthModel extends AbstractEarthModel {
  /**
   * Static instance.
   */
  public static final WGS72SpheroidEarthModel STATIC = new WGS72SpheroidEarthModel();

  /**
   * Radius of the WGS72 Ellipsoid in m (a).
   */
  public static final double WGS72_RADIUS = 6378135.0; // m

  /**
   * Inverse flattening 1/f of the WGS72 Ellipsoid.
   */
  public static final double WGS72_INV_FLATTENING = 298.26;

  /**
   * Flattening f of the WGS72 Ellipsoid.
   */
  public static final double WGS72_FLATTENING = 1 / WGS72_INV_FLATTENING;

  /**
   * Constructor.
   */
  protected WGS72SpheroidEarthModel() {
    super(WGS72_RADIUS, WGS72_RADIUS * (1 - WGS72_FLATTENING), WGS72_FLATTENING, WGS72_INV_FLATTENING);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected WGS72SpheroidEarthModel makeInstance() {
      return STATIC;
    }
  }
}
