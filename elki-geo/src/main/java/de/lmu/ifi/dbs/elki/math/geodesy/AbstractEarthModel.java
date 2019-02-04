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

import de.lmu.ifi.dbs.elki.math.MathUtil;
import net.jafama.DoubleWrapper;
import net.jafama.FastMath;

/**
 * Abstract base class for earth models with shared glue code.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
public abstract class AbstractEarthModel implements EarthModel {
  /**
   * Maximum number of iterations.
   */
  private static final int MAX_ITER = 20;

  /**
   * Maximum desired precision.
   */
  private static final double PRECISION = 1e-10;

  /**
   * Model parameters: major and minor radius.
   */
  final double a, b;

  /**
   * Model parameters: flattening, inverse flattening.
   */
  final double f, invf;

  /**
   * Derived model parameters: e and e squared.
   */
  final double e, esq;

  /**
   * Constructor.
   * 
   * @param a Major axis radius
   * @param b Minor axis radius
   * @param f Flattening
   * @param invf Inverse flattening
   */
  public AbstractEarthModel(double a, double b, double f, double invf) {
    super();
    this.a = a;
    this.b = b;
    this.f = f;
    this.invf = invf;
    this.esq = f * (2 - f);
    this.e = FastMath.sqrt(esq);
  }

  @Override
  public double getEquatorialRadius() {
    return a;
  }

  @Override
  public double getPolarDistance() {
    return b;
  }

  @Override
  public double[] latLngDegToECEF(double lat, double lng) {
    return latLngRadToECEF(MathUtil.deg2rad(lat), MathUtil.deg2rad(lng));
  }

  @Override
  public double[] latLngDegToECEF(double lat, double lng, double h) {
    return latLngRadToECEF(MathUtil.deg2rad(lat), MathUtil.deg2rad(lng), h);
  }

  @Override
  public double[] latLngRadToECEF(double lat, double lng) {
    // Sine and cosines:
    final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
    final double slat = FastMath.sinAndCos(lat, tmp), clat = tmp.value;
    final double slng = FastMath.sinAndCos(lng, tmp), clng = tmp.value;

    final double v = a / FastMath.sqrt(1 - esq * slat * slat);
    return new double[] { v * clat * clng, v * clat * slng, (1 - esq) * v * slat };
  }

  @Override
  public double[] latLngRadToECEF(double lat, double lng, double h) {
    // Sine and cosines:
    final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
    final double slat = FastMath.sinAndCos(lat, tmp), clat = tmp.value;
    final double slng = FastMath.sinAndCos(lng, tmp), clng = tmp.value;

    final double v = a / FastMath.sqrt(1 - esq * slat * slat);
    return new double[] { (v + h) * clat * clng, (v + h) * clat * slng, ((1 - esq) * v + h) * slat };
  }

  @Override
  public double ecefToLatDeg(double x, double y, double z) {
    return MathUtil.rad2deg(ecefToLatRad(x, y, z));
  }

  @Override
  public double ecefToLatRad(double x, double y, double z) {
    final double p = FastMath.sqrt(x * x + y * y);
    double plat = FastMath.atan2(z, p * (1 - esq));

    // Iteratively improving the lat value
    // TODO: instead of a fixed number of iterations, check for convergence?
    for (int i = 0;; i++) {
      final double slat = FastMath.sin(plat);
      final double v = a / FastMath.sqrt(1 - esq * slat * slat);
      final double lat = FastMath.atan2(z + esq * v * slat, p);
      if (Math.abs(lat - plat) < PRECISION || i > MAX_ITER) {
        return lat;
      }
      plat = lat;
    }
  }

  @Override
  public double ecefToLngDeg(double x, double y) {
    return MathUtil.rad2deg(ecefToLngRad(x, y));
  }

  @Override
  public double ecefToLngRad(double x, double y) {
    return FastMath.atan2(y, x);
  }

  @Override
  public double[] ecefToLatLngDegHeight(double x, double y, double z) {
    double[] ret = ecefToLatLngRadHeight(x, y, z);
    ret[0] = MathUtil.rad2deg(ret[0]);
    ret[1] = MathUtil.rad2deg(ret[1]);
    return ret;
  }

  @Override
  public double[] ecefToLatLngRadHeight(double x, double y, double z) {
    double lng = FastMath.atan2(y, x);
    final double p = FastMath.sqrt(x * x + y * y);
    double plat = FastMath.atan2(z, p * (1 - esq));
    double h = 0;

    // Iteratively improving the lat value
    // TODO: instead of a fixed number of iterations, check for convergence?
    for (int i = 0;; i++) {
      final double slat = FastMath.sin(plat);
      final double v = a / FastMath.sqrt(1 - esq * slat * slat);
      double lat = FastMath.atan2(z + esq * v * slat, p);
      if (Math.abs(lat - plat) < PRECISION || i > MAX_ITER) {
        h = p / FastMath.cos(lat) - v;
        return new double[] { lat, lng, h };
      }
      plat = lat;
    }
  }

  @Override
  public double distanceDeg(double lat1, double lng1, double lat2, double lng2) {
    return distanceRad(MathUtil.deg2rad(lat1), MathUtil.deg2rad(lng1), //
        MathUtil.deg2rad(lat2), MathUtil.deg2rad(lng2));
  }

  @Override
  public double distanceRad(double lat1, double lng1, double lat2, double lng2) {
    // Vincenty uses minor axis radius!
    return b * SphereUtil.ellipsoidVincentyFormulaRad(f, lat1, lng1, lat2, lng2);
  }

  @Override
  public double minDistDeg(double plat, double plng, double rminlat, double rminlng, double rmaxlat, double rmaxlng) {
    return minDistRad(MathUtil.deg2rad(plat), MathUtil.deg2rad(plng), //
        MathUtil.deg2rad(rminlat), MathUtil.deg2rad(rminlng), //
        MathUtil.deg2rad(rmaxlat), MathUtil.deg2rad(rmaxlng));
  }

  @Override
  public double minDistRad(double plat, double plng, double rminlat, double rminlng, double rmaxlat, double rmaxlng) {
    return b * SphereUtil.latlngMinDistRad(plat, plng, rminlat, rminlng, rmaxlat, rmaxlng);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName()+" [a=" + a + ", b=" + b + ", f=" + f + ", invf=" + invf + ", e=" + e + ", esq=" + esq + "]";
  }
}
