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

import static de.lmu.ifi.dbs.elki.math.MathUtil.HALFPI;
import static de.lmu.ifi.dbs.elki.math.MathUtil.TWOPI;
import static de.lmu.ifi.dbs.elki.math.MathUtil.deg2rad;
import static de.lmu.ifi.dbs.elki.math.MathUtil.rad2deg;
import static net.jafama.FastMath.acos;
import static net.jafama.FastMath.asin;
import static net.jafama.FastMath.atan;
import static net.jafama.FastMath.atan2;
import static net.jafama.FastMath.cos;
import static net.jafama.FastMath.sin;
import static net.jafama.FastMath.sinAndCos;
import static net.jafama.FastMath.sqrt;
import static net.jafama.FastMath.tan;

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import net.jafama.DoubleWrapper;

/**
 * Class with utility functions for distance computations on the sphere.
 * <p>
 * Note: the formulas are usually implemented for the unit sphere.
 * <p>
 * The majority of formulas are adapted from:
 * <p>
 * E. Williams<br>
 * Aviation Formulary<br>
 * Online: http://www.edwilliams.org/avform.htm
 * 
 * @author Erich Schubert
 * @author Niels Dörre
 * @since 0.5.5
 */
@Reference(authors = "E. Williams", //
    title = "Aviation Formulary", booktitle = "", //
    url = "http://www.edwilliams.org/avform.htm", //
    bibkey = "web/Williams11")
public final class SphereUtil {
  /**
   * Maximum number of iterations.
   */
  private static final int MAX_ITER = 20;

  /**
   * Maximum desired precision.
   */
  private static final double PRECISION = 1e-12;

  /**
   * Constant to divide by 6 via multiplication.
   */
  private static final double ONE_SIXTH = 1. / 6;

  /**
   * Dummy constructor. Do not instantiate.
   */
  private SphereUtil() {
    // Use static methods. Do not intantiate
  }

  /**
   * Compute the approximate great-circle distance of two points using the
   * Haversine formula
   * <p>
   * Complexity: 6 trigonometric functions.
   * <p>
   * Reference:
   * <p>
   * R. W. Sinnott,<br>
   * Virtues of the Haversine<br>
   * Sky and Telescope 68(2)
   * 
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance on unit sphere
   */
  public static double cosineFormulaDeg(double lat1, double lon1, double lat2, double lon2) {
    return cosineFormulaRad(deg2rad(lat1), deg2rad(lon1), deg2rad(lat2), deg2rad(lon2));
  }

  /**
   * Compute the approximate great-circle distance of two points using the
   * Spherical law of cosines.
   * <p>
   * Complexity: 6 trigonometric functions. Note that acos
   * is rather expensive apparently - roughly atan + sqrt.
   * <p>
   * Reference:
   * <p>
   * R. W. Sinnott,<br>
   * Virtues of the Haversine<br>
   * Sky and Telescope 68(2)
   * 
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance on unit sphere
   */
  public static double cosineFormulaRad(double lat1, double lon1, double lat2, double lon2) {
    if(lat1 == lat2 && lon1 == lon2) {
      return 0.;
    }
    final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
    final double slat1 = sinAndCos(lat1, tmp), clat1 = tmp.value;
    final double slat2 = sinAndCos(lat2, tmp), clat2 = tmp.value;
    final double a = slat1 * slat2 + clat1 * clat2 * cos(lon2 - lon1);
    return a < .9999_9999_9999_999 ? acos(a) : 0;
  }

  /**
   * Compute the approximate great-circle distance of two points using the
   * Haversine formula
   * <p>
   * Complexity: 5 trigonometric functions, 1 sqrt.
   * <p>
   * Reference:
   * <p>
   * R. W. Sinnott,<br>
   * Virtues of the Haversine<br>
   * Sky and Telescope 68(2)
   * 
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance on unit sphere
   */
  public static double haversineFormulaDeg(double lat1, double lon1, double lat2, double lon2) {
    return haversineFormulaRad(deg2rad(lat1), deg2rad(lon1), deg2rad(lat2), deg2rad(lon2));
  }

  /**
   * Compute the approximate great-circle distance of two points using the
   * Haversine formula
   * <p>
   * Complexity: 5 trigonometric functions, 1-2 sqrt.
   * <p>
   * Reference:
   * <p>
   * R. W. Sinnott,<br>
   * Virtues of the Haversine<br>
   * Sky and Telescope 68(2)
   *
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance on unit sphere
   */
  @Reference(authors = "R. W. Sinnott", //
      title = "Virtues of the Haversine", //
      booktitle = "Sky and Telescope 68(2)", //
      bibkey = "journals/skytelesc/Sinnott84")
  public static double haversineFormulaRad(double lat1, double lon1, double lat2, double lon2) {
    if(lat1 == lat2 && lon1 == lon2) {
      return 0.;
    }
    // Haversine formula, higher precision at < 1 meters but maybe issues at
    // antipodal points with asin, atan2 is supposedly better (but slower).
    final double slat = sin((lat1 - lat2) * .5), slon = sin((lon1 - lon2) * .5);
    double a = slat * slat + slon * slon * cos(lat1) * cos(lat2);
    return a < .9 ? 2 * asin(sqrt(a)) : a < 1 ? 2 * atan2(sqrt(a), sqrt(1 - a)) : Math.PI;
  }

  /**
   * Use cosine or haversine dynamically.
   *
   * Complexity: 4-5 trigonometric functions, 1 sqrt.
   *
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance on unit sphere
   */
  public static double cosineOrHaversineDeg(double lat1, double lon1, double lat2, double lon2) {
    return cosineOrHaversineRad(deg2rad(lat1), deg2rad(lon1), deg2rad(lat2), deg2rad(lon2));
  }

  /**
   * Use cosine or haversine dynamically.
   * <p>
   * Complexity: 4-5 trigonometric functions, 1 sqrt.
   *
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance on unit sphere
   */
  public static double cosineOrHaversineRad(double lat1, double lon1, double lat2, double lon2) {
    if(lat1 == lat2 && lon1 == lon2) {
      return 0.;
    }
    final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
    final double slat1 = sinAndCos(lat1, tmp), clat1 = tmp.value;
    final double slat2 = sinAndCos(lat2, tmp), clat2 = tmp.value;
    final double dlat = lat1 - lat2, dlon = lon1 - lon2;
    // Use cosine, cheaper:
    if(dlat > 0.01 || dlat < -0.01 || dlon > 0.01 || dlat < -0.01) {
      final double a = slat1 * slat2 + clat1 * clat2 * cos(dlon);
      return a < .9999_9999_9999_999 ? acos(a) : 0;
    }
    // Haversine formula, higher precision at < 1 meters
    final double slat = sin(dlat * .5), slon = sin(dlon * .5);
    return 2 * asin(sqrt(slat * slat + slon * slon * clat1 * clat2));
  }

  /**
   * Compute the approximate great-circle distance of two points.
   * 
   * Uses Vincenty's Formula for the spherical case, which does not require
   * iterations.
   * <p>
   * Complexity: 7 trigonometric functions, 1 sqrt.
   * <p>
   * Reference:
   * <p>
   * T. Vincenty<br>
   * Direct and inverse solutions of geodesics on the ellipsoid with application
   * of nested equations<br>
   * Survey Review 23:176, 1975
   * 
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance in radians / on unit sphere.
   */
  public static double sphericalVincentyFormulaDeg(double lat1, double lon1, double lat2, double lon2) {
    return sphericalVincentyFormulaRad(deg2rad(lat1), deg2rad(lon1), deg2rad(lat2), deg2rad(lon2));
  }

  /**
   * Compute the approximate great-circle distance of two points.
   * 
   * Uses Vincenty's Formula for the spherical case, which does not require
   * iterations.
   * <p>
   * Complexity: 7 trigonometric functions, 1 sqrt.
   * <p>
   * Reference:
   * <p>
   * T. Vincenty<br>
   * Direct and inverse solutions of geodesics on the ellipsoid with application
   * of nested equations<br>
   * Survey review 23 176, 1975
   * 
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance on unit sphere
   */
  @Reference(authors = "T. Vincenty", //
      title = "Direct and inverse solutions of geodesics on the ellipsoid with application of nested equations", //
      booktitle = "Survey Review 23:176", //
      url = "https://doi.org/10.1179/sre.1975.23.176.88", //
      bibkey = "doi:10.1179/sre.1975.23.176.88")
  public static double sphericalVincentyFormulaRad(double lat1, double lon1, double lat2, double lon2) {
    // Half delta longitude.
    final double dlnh = (lon1 > lon2) ? (lon1 - lon2) : (lon2 - lon1);

    // Spherical special case of Vincenty's formula - no iterations needed
    final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
    final double slat1 = sinAndCos(lat1, tmp), clat1 = tmp.value;
    final double slat2 = sinAndCos(lat2, tmp), clat2 = tmp.value;
    final double slond = sinAndCos(dlnh, tmp), clond = tmp.value;
    final double a = clat2 * slond;
    final double b = (clat1 * slat2) - (slat1 * clat2 * clond);
    return atan2(sqrt(a * a + b * b), slat1 * slat2 + clat1 * clat2 * clond);
  }

  /**
   * Compute the approximate great-circle distance of two points.
   * <p>
   * Reference:
   * <p>
   * T. Vincenty<br>
   * Direct and inverse solutions of geodesics on the ellipsoid with application
   * of nested equations<br>
   * Survey review 23 176, 1975
   * 
   * @param f Ellipsoid flattening
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance for a minor axis of 1.
   */
  public static double ellipsoidVincentyFormulaDeg(double f, double lat1, double lon1, double lat2, double lon2) {
    return ellipsoidVincentyFormulaRad(f, deg2rad(lat1), deg2rad(lon1), deg2rad(lat2), deg2rad(lon2));
  }

  /**
   * Compute the approximate great-circle distance of two points.
   * <p>
   * Reference:
   * <p>
   * T. Vincenty<br>
   * Direct and inverse solutions of geodesics on the ellipsoid with application
   * of nested equations<br>
   * Survey review 23 176, 1975
   * 
   * @param f Ellipsoid flattening
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance for a minor axis of 1.
   */
  @Reference(authors = "T. Vincenty", //
      title = "Direct and inverse solutions of geodesics on the ellipsoid with application of nested equations", //
      booktitle = "Survey Review 23:176", //
      url = "https://doi.org/10.1179/sre.1975.23.176.88", //
      bibkey = "doi:10.1179/sre.1975.23.176.88")
  public static double ellipsoidVincentyFormulaRad(double f, double lat1, double lon1, double lat2, double lon2) {
    final double dlon = (lon2 >= lon1) ? (lon2 - lon1) : (lon1 - lon2);
    final double onemf = 1 - f; // = 1 - (a-b)/a = b/a

    // Second eccentricity squared
    final double a_b = 1. / onemf; // = a/b
    final double ecc2 = (a_b + 1) * (a_b - 1); // (a^2-b^2)/(b^2)

    // Reduced latitudes:
    final double u1 = atan(onemf * tan(lat1));
    final double u2 = atan(onemf * tan(lat2));
    // Trigonometric values
    final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
    final double su1 = sinAndCos(u1, tmp), cu1 = tmp.value;
    final double su2 = sinAndCos(u2, tmp), cu2 = tmp.value;

    // Eqn (13) - initial value
    double lambda = dlon;

    for(int i = 0;; i++) {
      final double slon = sinAndCos(lambda, tmp), clon = tmp.value;

      // Eqn (14) - \sin \sigma
      final double term1 = cu2 * slon, term2 = cu1 * su2 - su1 * cu2 * clon;
      final double ssig = sqrt(term1 * term1 + term2 * term2);
      // Eqn (15) - \cos \sigma
      final double csig = su1 * su2 + cu1 * cu2 * clon;
      // Two identical points?
      if(!(ssig > 0)) {
        return 0.;
      }
      // Eqn (16) - \sigma from \tan \sigma
      final double sigma = atan2(ssig, csig);
      // Eqn (17) - \sin \alpha, and this way \cos^2 \alpha
      final double salp = cu1 * cu2 * slon / ssig;
      final double c2alp = (1. + salp) * (1. - salp);
      // Eqn (18) - \cos 2 \sigma_m
      final double ctwosigm = (Math.abs(c2alp) > 0) ? csig - 2.0 * su1 * su2 / c2alp : 0.;
      final double c2twosigm = ctwosigm * ctwosigm;

      // Eqn (10) - C
      final double cc = f * .0625 * c2alp * (4.0 + f * (4.0 - 3.0 * c2alp));
      // Eqn (11) - new \lambda
      final double prevlambda = lambda;
      lambda = dlon + (1.0 - cc) * f * salp * //
          (sigma + cc * ssig * (ctwosigm + cc * csig * (-1.0 + 2.0 * c2twosigm)));
      // Check for convergence:
      if(Math.abs(prevlambda - lambda) < PRECISION || i >= MAX_ITER) {
        // TODO: what is the proper result to return on MAX_ITER (antipodal
        // points)?
        // Definition of u^2, rewritten to use second eccentricity.
        final double usq = c2alp * ecc2;
        // Eqn (3) - A
        final double aa = 1.0 + usq / 16384.0 * (4096.0 + usq * (-768.0 + usq * (320.0 - 175.0 * usq)));
        // Eqn (4) - B
        final double bb = usq / 1024.0 * (256.0 + usq * (-128.0 + usq * (74.0 - 47.0 * usq)));
        // Eqn (6) - \Delta \sigma
        final double dsig = bb * ssig * (ctwosigm + .25 * bb * (csig * (-1.0 + 2.0 * c2twosigm) //
            - ONE_SIXTH * bb * ctwosigm * (-3.0 + 4.0 * ssig * ssig) * (-3.0 + 4.0 * c2twosigm)));
        // Eqn (19) - s
        return aa * (sigma - dsig);
      }
    }
  }

  /**
   * Compute the cross-track distance.
   * <p>
   * XTD = asin(sin(dist_1Q)*sin(crs_1Q-crs_12))
   * 
   * @param lat1 Latitude of starting point.
   * @param lon1 Longitude of starting point.
   * @param lat2 Latitude of destination point.
   * @param lon2 Longitude of destination point.
   * @param latQ Latitude of query point.
   * @param lonQ Longitude of query point.
   * @return Cross-track distance in km. May be negative - this gives the side.
   */
  public static double crossTrackDistanceDeg(double lat1, double lon1, double lat2, double lon2, double latQ, double lonQ) {
    return crossTrackDistanceRad(deg2rad(lat1), deg2rad(lon1), deg2rad(lat2), deg2rad(lon2), deg2rad(latQ), deg2rad(lonQ));
  }

  /**
   * Compute the cross-track distance.
   * 
   * @param lat1 Latitude of starting point.
   * @param lon1 Longitude of starting point.
   * @param lat2 Latitude of destination point.
   * @param lon2 Longitude of destination point.
   * @param latQ Latitude of query point.
   * @param lonQ Longitude of query point.
   * @param dist1Q Distance from starting point to query point on unit sphere
   * @return Cross-track distance on unit sphere. May be negative - this gives
   *         the side.
   */
  public static double crossTrackDistanceRad(double lat1, double lon1, double lat2, double lon2, double latQ, double lonQ, double dist1Q) {
    final double dlon12 = lon2 - lon1;
    final double dlon1Q = lonQ - lon1;

    // Compute trigonometric functions only once.
    final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
    final double slat1 = sinAndCos(lat1, tmp), clat1 = tmp.value;
    final double slatQ = sinAndCos(latQ, tmp), clatQ = tmp.value;
    final double slat2 = sinAndCos(lat2, tmp), clat2 = tmp.value;

    // / Compute the course
    // y = sin(dlon) * cos(lat2)
    final double sdlon12 = sinAndCos(dlon12, tmp), cdlon12 = tmp.value;
    final double sdlon1Q = sinAndCos(dlon1Q, tmp), cdlon1Q = tmp.value;
    final double yE = sdlon12 * clat2;
    final double yQ = sdlon1Q * clatQ;

    // x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dlon)
    final double xE = clat1 * slat2 - slat1 * clat2 * cdlon12;
    final double xQ = clat1 * slatQ - slat1 * clatQ * cdlon1Q;

    final double crs12 = atan2(yE, xE);
    final double crs1Q = atan2(yQ, xQ);

    // / Calculate cross-track distance
    return asin(sin(dist1Q) * sin(crs1Q - crs12));
  }

  /**
   * Compute the cross-track distance.
   * 
   * @param lat1 Latitude of starting point.
   * @param lon1 Longitude of starting point.
   * @param lat2 Latitude of destination point.
   * @param lon2 Longitude of destination point.
   * @param latQ Latitude of query point.
   * @param lonQ Longitude of query point.
   * @param dist1Q Distance from starting point to query point in radians (i.e.
   *        on unit sphere).
   * @return Cross-track distance on unit sphere. May be negative - this gives
   *         the side.
   */
  public static double crossTrackDistanceDeg(double lat1, double lon1, double lat2, double lon2, double latQ, double lonQ, double dist1Q) {
    return crossTrackDistanceRad(deg2rad(lat1), deg2rad(lon1), deg2rad(lat2), deg2rad(lon2), deg2rad(latQ), deg2rad(lonQ), dist1Q);
  }

  /**
   * Compute the cross-track distance.
   * <p>
   * XTD = asin(sin(dist_SQ)*sin(crs_SQ-crs_SE))
   * 
   * @param lat1 Latitude of starting point.
   * @param lon1 Longitude of starting point.
   * @param lat2 Latitude of destination point.
   * @param lon2 Longitude of destination point.
   * @param latQ Latitude of query point.
   * @param lonQ Longitude of query point.
   * @return Cross-track distance in km. May be negative - this gives the side.
   */
  public static double crossTrackDistanceRad(double lat1, double lon1, double lat2, double lon2, double latQ, double lonQ) {
    final double dlon12 = lon2 - lon1;
    final double dlon1Q = lonQ - lon1;
    final double dlat1Q = latQ - lat1;

    // Compute trigonometric functions only once.
    final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
    final double slat1 = sinAndCos(lat1, tmp), clat1 = tmp.value;
    final double slatQ = sinAndCos(latQ, tmp), clatQ = tmp.value;
    final double slat2 = sinAndCos(lat2, tmp), clat2 = tmp.value;

    // Compute the course
    // y = sin(dlon) * cos(lat2)
    final double sdlon12 = sinAndCos(dlon12, tmp), cdlon12 = tmp.value;
    final double sdlon1Q = sinAndCos(dlon1Q, tmp), cdlon1Q = tmp.value;
    final double yE = sdlon12 * clat2;
    final double yQ = sdlon1Q * clatQ;

    // x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dlon)
    final double xE = clat1 * slat2 - slat1 * clat2 * cdlon12;
    final double xQ = clat1 * slatQ - slat1 * clatQ * cdlon1Q;

    // Calculate cross-track distance
    // Haversine formula, higher precision at < 1 meters but maybe issues at
    // antipodal points - we do not yet multiply with the radius!
    final double slat = sin(dlat1Q * .5);
    final double slon = sin(dlon1Q * .5);
    final double a = slat * slat + slon * slon * clat1 * clatQ;
    if(a > 0.9999_9999_9999_999 || a < -0.9999_9999_9999_999 || a == 0.) {
      return 0.;
    }
    final double crs12 = atan2(yE, xE);
    final double crs1Q = atan2(yQ, xQ);
    return asin(sqrt(a) * sqrt(1 - a) * 2 * sin(crs1Q - crs12));
    // final double angDist1Q = a < 1 ? 2 * asin(sqrt(a)) : 0;
    // return asin(sin(angDist1Q) * sin(crs1Q - crs12));
  }

  /**
   * The along track distance is the distance from S to Q along the track from
   * S to E.
   * <p>
   * ATD=acos(cos(dist_1Q)/cos(XTD))
   * 
   * @param lat1 Latitude of starting point.
   * @param lon1 Longitude of starting point.
   * @param lat2 Latitude of destination point.
   * @param lon2 Longitude of destination point.
   * @param latQ Latitude of query point.
   * @param lonQ Longitude of query point.
   * @return Along-track distance in radians. May be negative - this gives the
   *         side.
   */
  public static double alongTrackDistanceDeg(double lat1, double lon1, double lat2, double lon2, double latQ, double lonQ) {
    return alongTrackDistanceRad(deg2rad(lat1), deg2rad(lon1), deg2rad(lat2), deg2rad(lon2), deg2rad(latQ), deg2rad(lonQ));
  }

  /**
   * The along track distance is the distance from S to Q along the track from
   * S to E.
   * <p>
   * ATD=acos(cos(dist_1Q)/cos(XTD))
   * <p>
   * TODO: optimize.
   * 
   * @param lat1 Latitude of starting point in radians.
   * @param lon1 Longitude of starting point in radians.
   * @param lat2 Latitude of destination point in radians.
   * @param lon2 Longitude of destination point in radians.
   * @param latQ Latitude of query point in radians.
   * @param lonQ Longitude of query point in radians.
   * @return Along-track distance in radians. May be negative - this gives the
   *         side.
   */
  public static double alongTrackDistanceRad(double lat1, double lon1, double lat2, double lon2, double latQ, double lonQ) {
    // TODO: inline and share some of the trigonometric computations!
    double dist1Q = haversineFormulaRad(lat1, lon1, latQ, lonQ);
    double ctd = crossTrackDistanceRad(lat1, lon1, lat2, lon2, latQ, lonQ, dist1Q);
    return alongTrackDistanceRad(lat1, lon1, lat2, lon2, latQ, lonQ, dist1Q, ctd);
  }

  /**
   * The along track distance is the distance from S to Q along the track from
   * S to E.
   * <p>
   * ATD=acos(cos(dist_SQ)/cos(XTD))
   * 
   * @param lat1 Latitude of starting point.
   * @param lon1 Longitude of starting point.
   * @param lat2 Latitude of destination point.
   * @param lon2 Longitude of destination point.
   * @param latQ Latitude of query point.
   * @param lonQ Longitude of query point.
   * @param dist1Q Distance S to Q in radians.
   * @param ctd Cross-track-distance in radians.
   * @return Along-track distance in radians. May be negative - this gives the
   *         side.
   */
  public static double alongTrackDistanceDeg(double lat1, double lon1, double lat2, double lon2, double latQ, double lonQ, double dist1Q, double ctd) {
    return alongTrackDistanceRad(deg2rad(lat1), deg2rad(lon1), deg2rad(lat2), deg2rad(lon2), deg2rad(latQ), deg2rad(lonQ), dist1Q, ctd);
  }

  /**
   * The along track distance, is the distance from S to Q along the track S to
   * E.
   * 
   * ATD=acos(cos(dist_SQ)/cos(XTD))
   * 
   * TODO: optimize: can we do a faster sign computation?
   * 
   * @param lat1 Latitude of starting point in radians.
   * @param lon1 Longitude of starting point in radians.
   * @param lat2 Latitude of destination point in radians.
   * @param lon2 Longitude of destination point in radians.
   * @param latQ Latitude of query point in radians.
   * @param lonQ Longitude of query point in radians.
   * @param dist1Q Distance S to Q in radians.
   * @param ctd Cross-track-distance in radians.
   * @return Along-track distance in radians. May be negative - this gives the
   *         side.
   */
  public static double alongTrackDistanceRad(double lat1, double lon1, double lat2, double lon2, double latQ, double lonQ, double dist1Q, double ctd) {
    // FIXME: optimize the sign computation!
    int sign = Math.abs(bearingRad(lat1, lon1, lat2, lon2) - bearingRad(lat1, lon1, latQ, lonQ)) < HALFPI ? +1 : -1;
    return sign * acos(cos(dist1Q) / cos(ctd));
    // TODO: for short distances, use this instead?
    // asin(sqrt( (sin(dist_1Q))^2 - (sin(XTD))^2 )/cos(XTD))
  }

  /**
   * Point to rectangle minimum distance.
   * <p>
   * Complexity:
   * <ul>
   * <li>Trivial cases (on longitude slice): no trigonometric functions.</li>
   * <li>Cross-track case: 10+2 trig</li>
   * <li>Corner case: 10+3 trig, 1 sqrt</li>
   * </ul>
   * <p>
   * Reference:
   * <p>
   * Erich Schubert, Arthur Zimek, Hans-Peter Kriegel<br>
   * Geodetic Distance Queries on R-Trees for Indexing Geographic Data<br>
   * Int. Symp. Advances in Spatial and Temporal Databases (SSTD'2013)
   * 
   * @param plat Latitude of query point.
   * @param plng Longitude of query point.
   * @param rminlat Min latitude of rectangle.
   * @param rminlng Min longitude of rectangle.
   * @param rmaxlat Max latitude of rectangle.
   * @param rmaxlng Max longitude of rectangle.
   * @return Distance in radians.
   */
  @Reference(authors = "Erich Schubert, Arthur Zimek, Hans-Peter Kriegel", //
      title = "Geodetic Distance Queries on R-Trees for Indexing Geographic Data", //
      booktitle = "Int. Symp. Advances in Spatial and Temporal Databases (SSTD'2013)", //
      url = "https://doi.org/10.1007/978-3-642-40235-7_9", //
      bibkey = "DBLP:conf/ssd/SchubertZK13")
  public static double latlngMinDistDeg(double plat, double plng, double rminlat, double rminlng, double rmaxlat, double rmaxlng) {
    return latlngMinDistRad(deg2rad(plat), deg2rad(plng), deg2rad(rminlat), deg2rad(rminlng), deg2rad(rmaxlat), deg2rad(rmaxlng));
  }

  /**
   * Point to rectangle minimum distance.
   * <p>
   * Complexity:
   * <ul>
   * <li>Trivial cases (on longitude slice): no trigonometric functions.</li>
   * <li>Corner case: 3/4 trig + 4-5 trig, 1 sqrt</li>
   * <li>Cross-track case: 4+2 trig</li>
   * </ul>
   * <p>
   * <b>Important:</b> Rectangles must be in -pi:+pi, and must have min &lt;
   * max, so they cannot cross the date line.
   * <p>
   * Reference:
   * <p>
   * Erich Schubert, Arthur Zimek, Hans-Peter Kriegel<br>
   * Geodetic Distance Queries on R-Trees for Indexing Geographic Data<br>
   * Int. Symp. Advances in Spatial and Temporal Databases (SSTD'2013)
   * 
   * @param plat Latitude of query point.
   * @param plng Longitude of query point.
   * @param rminlat Min latitude of rectangle.
   * @param rminlng Min longitude of rectangle.
   * @param rmaxlat Max latitude of rectangle.
   * @param rmaxlng Max longitude of rectangle.
   * @return Distance on unit sphere.
   */
  @Reference(authors = "Erich Schubert, Arthur Zimek, Hans-Peter Kriegel", //
      title = "Geodetic Distance Queries on R-Trees for Indexing Geographic Data", //
      booktitle = "Int. Symp. Advances in Spatial and Temporal Databases (SSTD'2013)", //
      url = "https://doi.org/10.1007/978-3-642-40235-7_9", //
      bibkey = "DBLP:conf/ssd/SchubertZK13")
  public static double latlngMinDistRad(double plat, double plng, double rminlat, double rminlng, double rmaxlat, double rmaxlng) {
    // FIXME: add support for rectangles crossing the +-180 deg boundary!

    // Degenerate rectangles:
    if((rminlat >= rmaxlat) && (rminlng >= rmaxlng)) {
      return cosineOrHaversineRad(rminlat, rminlng, plat, plng);
    }

    // The simplest case is when the query point is in the same "slice":
    if(rminlng <= plng && plng <= rmaxlng) {
      return (rminlat <= plat && plat <= rmaxlat) ? 0 // Inside
          : (plat < rminlat) ? rminlat - plat : plat - rmaxlat; // S, N
    }

    // Determine whether going east or west is shorter.
    double lngE = rminlng - plng, lngW = plng - rmaxlng;
    // Ensure delta to be in 0 to 2pi.
    lngE = lngE >= 0 ? lngE : lngE + TWOPI;
    lngW = lngW >= 0 ? lngW : lngW + TWOPI;

    // Case distinction east or west:
    final double lngD = (lngE <= lngW) ? lngE : lngW;
    final double rlng = (lngE <= lngW) ? rminlng : rmaxlng;

    final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
    final double slngD = sinAndCos(lngD, tmp), clngD = tmp.value;
    final double tlatQ = tan(plat);
    if(lngD >= HALFPI) { // XTD disappears at 90°
      return cosineOrHaversineRad(plat, plng, //
          tlatQ >= tan((rmaxlat + rminlat) * .5) * clngD // N/S
              ? rmaxlat : rminlat, rlng);
    }
    if(tlatQ >= tan(rmaxlat) * clngD) { // North corner
      return cosineOrHaversineRad(plat, plng, rmaxlat, rlng);
    }
    if(tlatQ <= tan(rminlat) * clngD) { // South corner
      return cosineOrHaversineRad(plat, plng, rminlat, rlng);
    }
    // Cross-track-distance to longitude line.
    return asin(cos(plat) * slngD);
  }

  /**
   * Point to rectangle minimum distance.
   * <p>
   * Previous version, only around for reference.
   * <p>
   * Complexity:
   * <ul>
   * <li>Trivial cases (on longitude slice): no trigonometric functions.</li>
   * <li>Cross-track case: 10+2 trig</li>
   * <li>Corner case: 10+3 trig, 1 sqrt</li>
   * </ul>
   * <p>
   * Reference:
   * <p>
   * Erich Schubert, Arthur Zimek, Hans-Peter Kriegel<br>
   * Geodetic Distance Queries on R-Trees for Indexing Geographic Data<br>
   * Int. Symp. Advances in Spatial and Temporal Databases (SSTD'2013)
   * 
   * @param plat Latitude of query point.
   * @param plng Longitude of query point.
   * @param rminlat Min latitude of rectangle.
   * @param rminlng Min longitude of rectangle.
   * @param rmaxlat Max latitude of rectangle.
   * @param rmaxlng Max longitude of rectangle.
   * @return Distance in radians
   */
  @Reference(authors = "Erich Schubert, Arthur Zimek, Hans-Peter Kriegel", //
      title = "Geodetic Distance Queries on R-Trees for Indexing Geographic Data", //
      booktitle = "Int. Symp. Advances in Spatial and Temporal Databases (SSTD'2013)", //
      url = "https://doi.org/10.1007/978-3-642-40235-7_9", //
      bibkey = "DBLP:conf/ssd/SchubertZK13")
  public static double latlngMinDistRadFull(double plat, double plng, double rminlat, double rminlng, double rmaxlat, double rmaxlng) {
    // FIXME: add support for rectangles crossing the +-180 deg boundary!

    // Degenerate rectangles:
    if((rminlat >= rmaxlat) && (rminlng >= rmaxlng)) {
      return haversineFormulaRad(rminlat, rminlng, plat, plng);
    }

    // The simplest case is when the query point is in the same "slice":
    if(rminlng <= plng && plng <= rmaxlng) {
      return (rminlat <= plat && plat <= rmaxlat) ? 0 // Inside
          : (plat < rminlat) ? rminlat - plat : plat - rmaxlat; // S, N
    }

    // Determine whether going east or west is shorter.
    double lngE = rminlng - plng;
    lngE = lngE >= 0 ? lngE : lngE + TWOPI;
    double lngW = plng - rmaxlng; // we keep this negative!
    lngW = lngW >= 0 ? lngW : lngW + TWOPI;

    // Compute sine and cosine values we will certainly need below:
    final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
    final double slatQ = sinAndCos(plat, tmp), clatQ = tmp.value;
    final double slatN = sinAndCos(rmaxlat, tmp), clatN = tmp.value;
    final double slatS = sinAndCos(rminlat, tmp), clatS = tmp.value;

    // Head east, to min edge:
    if(lngE <= lngW) {
      final double slngD = sinAndCos(lngE, tmp), clngD = tmp.value;

      // Bearing to south
      // atan2(slngD * clatS, clatQ * slatS - slatQ * clatS * clngD);
      // Bearing from south
      final double bs = atan2(slngD * clatQ, clatS * slatQ - slatS * clatQ * clngD);
      // Bearing to north
      // atan2(slngD * clatN, clatQ * slatN - slatQ * clatN * clngD);
      // Bearing from north
      final double bn = atan2(slngD * clatQ, clatN * slatQ - slatN * clatQ * clngD);
      if(bs < HALFPI && bn > HALFPI) {
        // Radians from south pole = abs(ATD)
        final double radFromS = -HALFPI - plat;

        // Cross-track-distance to longitude line.
        return asin(sin(radFromS) * -slngD);
      }
      if(bs - HALFPI < HALFPI - bn) {
        // Haversine to north corner.
        final double slatN2 = sin((plat - rmaxlat) * .5);
        final double slon = sin(lngE * .5);
        final double aN = slatN2 * slatN2 + slon * slon * clatQ * clatN;
        return 2 * atan2(sqrt(aN), sqrt(1 - aN));
      }
      else {
        // Haversine to south corner.
        final double slatS2 = sin((plat - rminlat) * .5);
        final double slon = sin(lngE * .5);
        final double aS = slatS2 * slatS2 + slon * slon * clatQ * clatS;
        return 2 * atan2(sqrt(aS), sqrt(1 - aS));
      }
    }
    else { // Head west, to max edge
      final double slngD = -sinAndCos(lngW, tmp), clngD = tmp.value;

      // Bearing to south
      // atan2(slngD * clatS, clatQ * slatS - slatQ * clatS * clngD);
      // Bearing from south
      final double bs = atan2(slngD * clatQ, clatS * slatQ - slatS * clatQ * clngD);
      // Bearing to north
      // atan2(slngD * clatN, clatQ * slatN - slatQ * clatN * clngD);
      // Bearing from north
      final double bn = atan2(slngD * clatQ, clatN * slatQ - slatN * clatQ * clngD);
      if(bs > -HALFPI && bn < -HALFPI) {
        // Radians from south = abs(ATD) = distance from pole
        final double radFromS = -HALFPI - plat;
        // Cross-track-distance to longitude line.
        return asin(sin(radFromS) * slngD);
      }
      if(-HALFPI - bs < bn + HALFPI) {
        // Haversine to north corner.
        final double slatN2 = sin((plat - rmaxlat) * .5);
        final double slon = sin(lngW * .5);
        final double aN = slatN2 * slatN2 + slon * slon * clatQ * clatN;
        return 2 * atan2(sqrt(aN), sqrt(1 - aN));
      }
      else {
        // Haversine to south corner.
        final double slatS2 = sin((plat - rminlat) * .5);
        final double slon = sin(lngW * .5);
        final double aS = slatS2 * slatS2 + slon * slon * clatQ * clatS;
        return 2 * atan2(sqrt(aS), sqrt(1 - aS));
      }
    }
  }

  /**
   * Compute the bearing from start to end.
   * 
   * @param latS Start latitude, in degree
   * @param lngS Start longitude, in degree
   * @param latE End latitude, in degree
   * @param lngE End longitude, in degree
   * @return Bearing in degree
   */
  public static double bearingDegDeg(double latS, double lngS, double latE, double lngE) {
    return rad2deg(bearingRad(deg2rad(latS), deg2rad(lngS), deg2rad(latE), deg2rad(lngE)));
  }

  /**
   * Compute the bearing from start to end.
   * 
   * @param latS Start latitude, in radians
   * @param lngS Start longitude, in radians
   * @param latE End latitude, in radians
   * @param lngE End longitude, in radians
   * @return Bearing in degree
   */
  public static double bearingRad(double latS, double lngS, double latE, double lngE) {
    final DoubleWrapper tmp = new DoubleWrapper(); // To return cosine
    final double slatS = sinAndCos(latS, tmp), clatS = tmp.value;
    final double slatE = sinAndCos(latE, tmp), clatE = tmp.value;
    return atan2(-sin(lngS - lngE) * clatE, clatS * slatE - slatS * clatE * cos(lngS - lngE));
  }
}
