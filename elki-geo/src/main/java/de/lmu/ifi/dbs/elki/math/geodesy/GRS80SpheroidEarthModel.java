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
 * The GRS 80 spheroid earth model, without height model (so not a geoid, just a
 * spheroid!)
 * <p>
 * Radius: 6378137.0 m
 * <p>
 * Flattening: 1 / 298.257222101
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias({ "GRS-80", "GRS80" })
public class GRS80SpheroidEarthModel extends AbstractEarthModel {
  /**
   * Static instance.
   */
  public static final GRS80SpheroidEarthModel STATIC = new GRS80SpheroidEarthModel();

  /**
   * Radius of the GRS80 Ellipsoid in m (a).
   */
  public static final double GRS80_RADIUS = 6378137.0; // m

  /**
   * Inverse flattening 1/f of the GRS80 Ellipsoid.
   */
  public static final double GRS80_INV_FLATTENING = 298.257222101;

  /**
   * Flattening f of the GRS80 Ellipsoid.
   */
  public static final double GRS80_FLATTENING = 1 / GRS80_INV_FLATTENING;

  /**
   * Constructor.
   */
  protected GRS80SpheroidEarthModel() {
    super(GRS80_RADIUS, GRS80_RADIUS * (1 - GRS80_FLATTENING), GRS80_FLATTENING, GRS80_INV_FLATTENING);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected GRS80SpheroidEarthModel makeInstance() {
      return STATIC;
    }
  }
}
