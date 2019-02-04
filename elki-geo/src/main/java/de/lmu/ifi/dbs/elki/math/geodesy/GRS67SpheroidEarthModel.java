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
 * The GRS 67 spheroid earth model.
 * <p>
 * Radius: 6378160.0 m
 * <p>
 * Flattening: 1 / 298.25
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias({ "GRS67", "GRS-67" })
public class GRS67SpheroidEarthModel extends AbstractEarthModel {
  /**
   * Static instance.
   */
  public static final GRS67SpheroidEarthModel STATIC = new GRS67SpheroidEarthModel();

  /**
   * Radius of the GRS67 Ellipsoid in m (a).
   */
  public static final double GRS67_RADIUS = 6378160.0; // m

  /**
   * Inverse flattening 1/f of the GRS67 Ellipsoid.
   */
  public static final double GRS67_INV_FLATTENING = 298.25;

  /**
   * Flattening f of the GRS67 Ellipsoid.
   */
  public static final double GRS67_FLATTENING = 1 / GRS67_INV_FLATTENING;

  /**
   * Constructor.
   */
  protected GRS67SpheroidEarthModel() {
    super(GRS67_RADIUS, GRS67_RADIUS * (1 - GRS67_FLATTENING), GRS67_FLATTENING, GRS67_INV_FLATTENING);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected GRS67SpheroidEarthModel makeInstance() {
      return STATIC;
    }
  }
}
