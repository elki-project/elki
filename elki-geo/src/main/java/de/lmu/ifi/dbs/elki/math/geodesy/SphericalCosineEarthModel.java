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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import net.jafama.DoubleWrapper;
import net.jafama.FastMath;

/**
 * A simple spherical earth model using radius 6371009 m.
 * <p>
 * For distance computations, this variant uses the Cosine formula, which is
 * faster but less accurate than the Haversince or Vincenty's formula.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public class SphericalCosineEarthModel extends AbstractEarthModel {
  /**
   * Spherical earth model, static instance.
   */
  public static final SphericalCosineEarthModel STATIC = new SphericalCosineEarthModel();

  /**
   * Earth radius approximation in m.
   */
  public static final double EARTH_RADIUS = 6371009; // m

  /**
   * Constructor.
   */
  protected SphericalCosineEarthModel() {
    super(EARTH_RADIUS, EARTH_RADIUS, 0., Double.POSITIVE_INFINITY);
  }

  @Override
  public double[] latLngRadToECEF(double lat, double lng) {
    // Then to sine and cosines:
    final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
    final double slat = FastMath.sinAndCos(lat, tmp), clat = tmp.value;
    final double slng = FastMath.sinAndCos(lng, tmp), clng = tmp.value;

    return new double[] { EARTH_RADIUS * clat * clng, EARTH_RADIUS * clat * slng, EARTH_RADIUS * slat };
  }

  @Override
  public double[] latLngRadToECEF(double lat, double lng, double h) {
    // Then to sine and cosines:
    final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
    final double slat = FastMath.sinAndCos(lat, tmp), clat = tmp.value;
    final double slng = FastMath.sinAndCos(lng, tmp), clng = tmp.value;

    return new double[] { (EARTH_RADIUS + h) * clat * clng, (EARTH_RADIUS + h) * clat * slng, (EARTH_RADIUS + h) * slat };
  }

  @Override
  public double ecefToLatRad(double x, double y, double z) {
    final double p = FastMath.sqrt(x * x + y * y);
    return FastMath.atan2(z, p);
  }

  @Override
  public double distanceRad(double lat1, double lng1, double lat2, double lng2) {
    return EARTH_RADIUS * SphereUtil.cosineFormulaRad(lat1, lng1, lat2, lng2);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected SphericalCosineEarthModel makeInstance() {
      return STATIC;
    }
  }
}
