package de.lmu.ifi.dbs.elki.math.geodesy;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Class with utility functions for distance computations on the sphere.
 * 
 * Note: the formulas are usually implemented for the unit sphere.
 * 
 * The majority of formulas are adapted from:
 * <p>
 * Ed Williams<br />
 * Aviation Formulary<br />
 * Online: http://williams.best.vwh.net/avform.htm
 * </p>
 * 
 * TODO: add ellipsoid version of Vinentry formula.
 * 
 * @author Erich Schubert
 * @author Niels Dörre
 * @since 0.5.5
 */
@Reference(authors = "Ed Williams", title = "Aviation Formulary", booktitle = "", url = "http://williams.best.vwh.net/avform.htm")
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
   * 
   * Complexity: 6 (2 of which emulated) trigonometric functions.
   * 
   * Reference:
   * <p>
   * R. W. Sinnott,<br/>
   * Virtues of the Haversine<br />
   * Sky and telescope, 68-2, 1984
   * </p>
   * 
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance on unit sphere
   */
  public static double cosineFormulaDeg(double lat1, double lon1, double lat2, double lon2) {
    return cosineFormulaRad(MathUtil.deg2rad(lat1), MathUtil.deg2rad(lon1),//
        MathUtil.deg2rad(lat2), MathUtil.deg2rad(lon2));
  }

  /**
   * Compute the approximate great-circle distance of two points using the
   * Spherical law of cosines.
   * 
   * Complexity: 6 (2 of which emulated) trigonometric functions. Note that acos
   * is rather expensive apparently - roughly atan + sqrt.
   * 
   * Reference:
   * <p>
   * R. W. Sinnott,<br/>
   * Virtues of the Haversine<br />
   * Sky and telescope, 68-2, 1984
   * </p>
   * 
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance on unit sphere
   */
  public static double cosineFormulaRad(double lat1, double lon1, double lat2, double lon2) {
    final double slat1 = Math.sin(lat1), clat1 = MathUtil.sinToCos(lat1, slat1);
    final double slat2 = Math.sin(lat2), clat2 = MathUtil.sinToCos(lat2, slat2);
    return Math.acos(Math.min(1.0, slat1 * slat2 + clat1 * clat2 * Math.cos(Math.abs(lon2 - lon1))));
  }

  /**
   * Compute the approximate great-circle distance of two points using the
   * Haversine formula
   * 
   * Complexity: 5 trigonometric functions, 2 sqrt.
   * 
   * Reference:
   * <p>
   * R. W. Sinnott,<br/>
   * Virtues of the Haversine<br />
   * Sky and telescope, 68-2, 1984
   * </p>
   * 
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance on unit sphere
   */
  @Reference(authors = "Sinnott, R. W.", title = "Virtues of the Haversine", booktitle = "Sky and telescope, 68-2, 1984")
  public static double haversineFormulaDeg(double lat1, double lon1, double lat2, double lon2) {
    return haversineFormulaRad(MathUtil.deg2rad(lat1), MathUtil.deg2rad(lon1),//
        MathUtil.deg2rad(lat2), MathUtil.deg2rad(lon2));
  }

  /**
   * Compute the approximate great-circle distance of two points using the
   * Haversine formula
   * 
   * Complexity: 5 trigonometric functions, 2 sqrt.
   * 
   * Reference:
   * <p>
   * R. W. Sinnott,<br/>
   * Virtues of the Haversine<br />
   * Sky and telescope, 68-2, 1984
   * </p>
   * 
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance on unit sphere
   */
  @Reference(authors = "Sinnott, R. W.", title = "Virtues of the Haversine", booktitle = "Sky and telescope, 68-2, 1984")
  public static double haversineFormulaRad(double lat1, double lon1, double lat2, double lon2) {
    // Haversine formula, higher precision at < 1 meters but maybe issues at
    // antipodal points.
    final double slat = Math.sin((lat1 - lat2) * .5);
    final double slon = Math.sin((lon1 - lon2) * .5);
    final double a = slat * slat + slon * slon * Math.cos(lat1) * Math.cos(lat2);
    return 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  /**
   * Compute the approximate great-circle distance of two points.
   * 
   * Uses Vincenty's Formula for the spherical case, which does not require
   * iterations.
   * 
   * Complexity: 7 trigonometric functions, 1 sqrt.
   * 
   * Reference:
   * <p>
   * T. Vincenty<br />
   * Direct and inverse solutions of geodesics on the ellipsoid with application
   * of nested equations<br />
   * Survey review 23 176, 1975
   * </p>
   * 
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance in radians / on unit sphere.
   */
  @Reference(authors = "T. Vincenty", title = "Direct and inverse solutions of geodesics on the ellipsoid with application of nested equations", booktitle = "Survey review 23 176, 1975", url = "http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf")
  public static double sphericalVincentyFormulaDeg(double lat1, double lon1, double lat2, double lon2) {
    return sphericalVincentyFormulaRad(MathUtil.deg2rad(lat1), MathUtil.deg2rad(lon1),//
        MathUtil.deg2rad(lat2), MathUtil.deg2rad(lon2));
  }

  /**
   * Compute the approximate great-circle distance of two points.
   * 
   * Uses Vincenty's Formula for the spherical case, which does not require
   * iterations.
   * 
   * Complexity: 7 trigonometric functions, 1 sqrt.
   * 
   * Reference:
   * <p>
   * T. Vincenty<br />
   * Direct and inverse solutions of geodesics on the ellipsoid with application
   * of nested equations<br />
   * Survey review 23 176, 1975
   * </p>
   * 
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance on unit sphere
   */
  @Reference(authors = "T. Vincenty", title = "Direct and inverse solutions of geodesics on the ellipsoid with application of nested equations", booktitle = "Survey review 23 176, 1975", url = "http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf")
  public static double sphericalVincentyFormulaRad(double lat1, double lon1, double lat2, double lon2) {
    // Half delta longitude.
    final double dlnh = Math.abs(lon1 - lon2);

    // Spherical special case of Vincenty's formula - no iterations needed
    final double slat1 = Math.sin(lat1), clat1 = MathUtil.sinToCos(lat1, slat1);
    final double slat2 = Math.sin(lat2), clat2 = MathUtil.sinToCos(lat2, slat2);
    final double slond = Math.sin(dlnh), clond = MathUtil.sinToCos(dlnh, slond);
    final double a = clat2 * slond;
    final double b = (clat1 * slat2) - (slat1 * clat2 * clond);
    return Math.atan2(Math.sqrt(a * a + b * b), slat1 * slat2 + clat1 * clat2 * clond);
  }

  /**
   * Compute the approximate great-circle distance of two points.
   * 
   * Reference:
   * <p>
   * T. Vincenty<br />
   * Direct and inverse solutions of geodesics on the ellipsoid with application
   * of nested equations<br />
   * Survey review 23 176, 1975
   * </p>
   * 
   * @param f Ellipsoid flattening
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance for a minor axis of 1.
   */
  @Reference(authors = "T. Vincenty", title = "Direct and inverse solutions of geodesics on the ellipsoid with application of nested equations", booktitle = "Survey review 23 176, 1975", url = "http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf")
  public static double ellipsoidVincentyFormulaDeg(double f, double lat1, double lon1, double lat2, double lon2) {
    return ellipsoidVincentyFormulaRad(f, MathUtil.deg2rad(lat1), MathUtil.deg2rad(lon1), //
        MathUtil.deg2rad(lat2), MathUtil.deg2rad(lon2));
  }

  /**
   * Compute the approximate great-circle distance of two points.
   * 
   * Reference:
   * <p>
   * T. Vincenty<br />
   * Direct and inverse solutions of geodesics on the ellipsoid with application
   * of nested equations<br />
   * Survey review 23 176, 1975
   * </p>
   * 
   * @param f Ellipsoid flattening
   * @param lat1 Latitude of first point in degree
   * @param lon1 Longitude of first point in degree
   * @param lat2 Latitude of second point in degree
   * @param lon2 Longitude of second point in degree
   * @return Distance for a minor axis of 1.
   */
  @Reference(authors = "T. Vincenty", title = "Direct and inverse solutions of geodesics on the ellipsoid with application of nested equations", booktitle = "Survey review 23 176, 1975", url = "http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf")
  public static double ellipsoidVincentyFormulaRad(double f, double lat1, double lon1, double lat2, double lon2) {
    final double dlon = Math.abs(lon2 - lon1);
    final double onemf = 1 - f; // = 1 - (a-b)/a = b/a

    // Second eccentricity squared
    final double a_b = 1. / onemf; // = a/b
    final double ecc2 = (a_b + 1) * (a_b - 1); // (a^2-b^2)/(b^2)

    // Reduced latitudes:
    final double u1 = Math.atan(onemf * Math.tan(lat1));
    final double u2 = Math.atan(onemf * Math.tan(lat2));
    // Trigonometric values
    final double su1 = Math.sin(u1), cu1 = MathUtil.sinToCos(u1, su1);
    final double su2 = Math.sin(u2), cu2 = MathUtil.sinToCos(u2, su2);

    // Eqn (13) - initial value
    double lambda = dlon;

    for (int i = 0;; i++) {
      final double slon = Math.sin(lambda), clon = MathUtil.sinToCos(lambda, slon);

      // Eqn (14) - \sin \sigma
      final double term1 = cu2 * slon, term2 = cu1 * su2 - su1 * cu2 * clon;
      final double ssig = Math.sqrt(term1 * term1 + term2 * term2);
      // Eqn (15) - \cos \sigma
      final double csig = su1 * su2 + cu1 * cu2 * clon;
      // Eqn (16) - \sigma from \tan \sigma
      final double sigma = Math.atan2(ssig, csig);

      // Two identical points?
      if (!(ssig > 0)) {
        return 0.;
      }
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
      if (Math.abs(prevlambda - lambda) < PRECISION || i >= MAX_ITER) {
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
   * 
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
    return crossTrackDistanceRad(MathUtil.deg2rad(lat1), MathUtil.deg2rad(lon1),//
        MathUtil.deg2rad(lat2), MathUtil.deg2rad(lon2),//
        MathUtil.deg2rad(latQ), MathUtil.deg2rad(lonQ));
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
    final double slat1 = Math.sin(lat1), clat1 = MathUtil.sinToCos(lat1, slat1);
    final double slatQ = Math.sin(latQ), clatQ = MathUtil.sinToCos(latQ, slatQ);
    final double slat2 = Math.sin(lat2), clat2 = MathUtil.sinToCos(lat2, slat2);

    // / Compute the course
    // y = sin(dlon) * cos(lat2)
    final double sdlon12 = Math.sin(dlon12), cdlon12 = MathUtil.sinToCos(dlon12, sdlon12);
    final double sdlon1Q = Math.sin(dlon1Q), cdlon1Q = MathUtil.sinToCos(dlon1Q, sdlon1Q);

    final double yE = sdlon12 * clat2;
    final double yQ = sdlon1Q * clatQ;

    // x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dlon)
    final double xE = clat1 * slat2 - slat1 * clat2 * cdlon12;
    final double xQ = clat1 * slatQ - slat1 * clatQ * cdlon1Q;

    final double crs12 = Math.atan2(yE, xE);
    final double crs1Q = Math.atan2(yQ, xQ);

    // / Calculate cross-track distance
    return Math.asin(Math.sin(dist1Q) * Math.sin(crs1Q - crs12));
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
    return crossTrackDistanceRad(MathUtil.deg2rad(lat1), MathUtil.deg2rad(lon1),//
        MathUtil.deg2rad(lat2), MathUtil.deg2rad(lon2),//
        MathUtil.deg2rad(latQ), MathUtil.deg2rad(lonQ),//
        dist1Q);
  }

  /**
   * Compute the cross-track distance.
   * 
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
    final double clat1 = Math.cos(lat1), slat1 = MathUtil.cosToSin(lat1, clat1);
    final double clatQ = Math.cos(latQ), slatQ = MathUtil.cosToSin(latQ, clatQ);
    final double clat2 = Math.cos(lat2), slat2 = MathUtil.cosToSin(lat2, clat2);

    // Haversine formula, higher precision at < 1 meters but maybe issues at
    // antipodal points - we do not yet multiply with the radius!
    final double slat = Math.sin(dlat1Q * .5);
    final double slon = Math.sin(dlon1Q * .5);
    final double a = slat * slat + slon * slon * clat1 * clatQ;
    final double angDist1Q = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    // Compute the course
    // y = sin(dlon) * cos(lat2)
    final double sdlon12 = Math.sin(dlon12), cdlon12 = MathUtil.sinToCos(dlon12, sdlon12);
    final double sdlon1Q = Math.sin(dlon1Q), cdlon1Q = MathUtil.sinToCos(dlon1Q, sdlon1Q);
    final double yE = sdlon12 * clat2;
    final double yQ = sdlon1Q * clatQ;

    // x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dlon)
    final double xE = clat1 * slat2 - slat1 * clat2 * cdlon12;
    final double xQ = clat1 * slatQ - slat1 * clatQ * cdlon1Q;

    final double crs12 = Math.atan2(yE, xE);
    final double crs1Q = Math.atan2(yQ, xQ);

    // Calculate cross-track distance
    return Math.asin(Math.sin(angDist1Q) * Math.sin(crs1Q - crs12));
  }

  /**
   * The along track distance, is the distance from S to Q along the track S to
   * E.
   * 
   * ATD=acos(cos(dist_1Q)/cos(XTD))
   * 
   * TODO: optimize.
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
    // TODO: inline and share some of the trigonometric computations!
    double dist1Q = haversineFormulaDeg(lat1, lon1, latQ, lonQ);
    double ctd = crossTrackDistanceDeg(lat1, lon1, lat2, lon2, latQ, lonQ, dist1Q);
    return alongTrackDistanceDeg(lat1, lon1, lat2, lon2, latQ, lonQ, dist1Q, ctd);
  }

  /**
   * The along track distance, is the distance from S to Q along the track S to
   * E.
   * 
   * ATD=acos(cos(dist_1Q)/cos(XTD))
   * 
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
   * The along track distance, is the distance from S to Q along the track S to
   * E.
   * 
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
    return alongTrackDistanceRad(MathUtil.deg2rad(lat1), MathUtil.deg2rad(lon1), MathUtil.deg2rad(lat2), MathUtil.deg2rad(lon2), MathUtil.deg2rad(latQ), MathUtil.deg2rad(lonQ), dist1Q, ctd);
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
    int sign = Math.abs(bearingRad(lat1, lon1, lat2, lon2) - bearingRad(lat1, lon1, latQ, lonQ)) < MathUtil.HALFPI ? +1 : -1;
    return sign * Math.acos(Math.cos(dist1Q) / Math.cos(ctd));
    // TODO: for short distances, use this instead?
    // asin(sqrt( (sin(dist_1Q))^2 - (sin(XTD))^2 )/cos(XTD))
  }

  /**
   * Point to rectangle minimum distance.
   * 
   * Complexity:
   * <ul>
   * <li>Trivial cases (on longitude slice): no trigonometric functions.</li>
   * <li>Cross-track case: 10+2 trig</li>
   * <li>Corner case: 10+3 trig, 2 sqrt</li>
   * </ul>
   * 
   * Reference:
   * <p>
   * Erich Schubert, Arthur Zimek and Hans-Peter Kriegel<br />
   * Geodetic Distance Queries on R-Trees for Indexing Geographic Data<br />
   * Advances in Spatial and Temporal Databases - 13th International Symposium,
   * SSTD 2013, Munich, Germany
   * </p>
   * 
   * @param plat Latitude of query point.
   * @param plng Longitude of query point.
   * @param rminlat Min latitude of rectangle.
   * @param rminlng Min longitude of rectangle.
   * @param rmaxlat Max latitude of rectangle.
   * @param rmaxlng Max longitude of rectangle.
   * @return Distance in radians.
   */
  @Reference(authors = "Erich Schubert, Arthur Zimek and Hans-Peter Kriegel", title = "Geodetic Distance Queries on R-Trees for Indexing Geographic Data", booktitle = "Advances in Spatial and Temporal Databases - 13th International Symposium, SSTD 2013, Munich, Germany")
  public static double latlngMinDistDeg(double plat, double plng, double rminlat, double rminlng, double rmaxlat, double rmaxlng) {
    return latlngMinDistRad(MathUtil.deg2rad(plat), MathUtil.deg2rad(plng),//
        MathUtil.deg2rad(rminlat), MathUtil.deg2rad(rminlng), //
        MathUtil.deg2rad(rmaxlat), MathUtil.deg2rad(rmaxlng));
  }

  /**
   * Point to rectangle minimum distance.
   * 
   * Complexity:
   * <ul>
   * <li>Trivial cases (on longitude slice): no trigonometric functions.</li>
   * <li>Corner case: 3/4 trig + (haversine:) 5 trig, 2 sqrt</li>
   * <li>Cross-track case: 4+3 trig</li>
   * </ul>
   * 
   * Reference:
   * <p>
   * Erich Schubert, Arthur Zimek and Hans-Peter Kriegel<br />
   * Geodetic Distance Queries on R-Trees for Indexing Geographic Data<br />
   * Advances in Spatial and Temporal Databases - 13th International Symposium,
   * SSTD 2013, Munich, Germany
   * </p>
   * 
   * @param plat Latitude of query point.
   * @param plng Longitude of query point.
   * @param rminlat Min latitude of rectangle.
   * @param rminlng Min longitude of rectangle.
   * @param rmaxlat Max latitude of rectangle.
   * @param rmaxlng Max longitude of rectangle.
   * @return Distance on unit sphere.
   */
  @Reference(authors = "Erich Schubert, Arthur Zimek and Hans-Peter Kriegel", title = "Geodetic Distance Queries on R-Trees for Indexing Geographic Data", booktitle = "Advances in Spatial and Temporal Databases - 13th International Symposium, SSTD 2013, Munich, Germany")
  public static double latlngMinDistRad(double plat, double plng, double rminlat, double rminlng, double rmaxlat, double rmaxlng) {
    // FIXME: handle rectangles crossing the +-180 deg boundary correctly!

    // Degenerate rectangles:
    if ((rminlat >= rmaxlat) && (rminlng >= rmaxlng)) {
      return haversineFormulaRad(rminlat, rminlng, plat, plng);
    }

    // The simplest case is when the query point is in the same "slice":
    if (rminlng <= plng && plng <= rmaxlng) {
      // Inside rectangle:
      if (rminlat <= plat && plat <= rmaxlat) {
        return 0;
      }
      // South:
      if (plat < rminlat) {
        return rminlat - plat;
      } else {
        // plat > rmaxlat
        return plat - rmaxlat;
      }
    }

    // Determine whether going east or west is shorter.
    double lngE = rminlng - plng;
    if (lngE < 0) {
      lngE += MathUtil.TWOPI;
    }
    double lngW = rmaxlng - plng; // we keep this negative!
    if (lngW > 0) {
      lngW -= MathUtil.TWOPI;
    }

    // East, to min edge:
    if (lngE <= -lngW) {
      final double clngD = Math.cos(lngE);
      final double tlatQ = Math.tan(plat);
      if (lngE > MathUtil.HALFPI) {
        final double tlatm = Math.tan((rmaxlat + rminlat) * .5);
        if (tlatQ >= tlatm * clngD) {
          return haversineFormulaRad(plat, plng, rmaxlat, rminlng);
        } else {
          return haversineFormulaRad(plat, plng, rminlat, rminlng);
        }
      }
      final double tlatN = Math.tan(rmaxlat);
      if (tlatQ >= tlatN * clngD) { // North corner
        return haversineFormulaRad(plat, plng, rmaxlat, rminlng);
      }
      final double tlatS = Math.tan(rminlat);
      if (tlatQ <= tlatS * clngD) { // South corner
        return haversineFormulaRad(plat, plng, rminlat, rminlng);
      }
      // Cross-track-distance to longitude line.
      final double slngD = MathUtil.cosToSin(lngE, clngD);
      return Math.asin(Math.cos(plat) * slngD);
    } else { // West, to max edge:
      final double clngD = Math.cos(lngW);
      final double tlatQ = Math.tan(plat);
      if (-lngW > MathUtil.HALFPI) {
        final double tlatm = Math.tan((rmaxlat + rminlat) * .5);
        if (tlatQ >= tlatm * clngD) {
          return haversineFormulaRad(plat, plng, rmaxlat, rmaxlng);
        } else {
          return haversineFormulaRad(plat, plng, rminlat, rmaxlng);
        }
      }
      final double tlatN = Math.tan(rmaxlat);
      if (tlatQ >= tlatN * clngD) { // North corner
        return haversineFormulaRad(plat, plng, rmaxlat, rmaxlng);
      }
      final double tlatS = Math.tan(rminlat);
      if (tlatQ <= tlatS * clngD) { // South corner
        return haversineFormulaRad(plat, plng, rminlat, rmaxlng);
      }
      // Cross-track-distance to longitude line.
      final double slngD = MathUtil.cosToSin(lngW, clngD);
      return Math.asin(-Math.cos(plat) * slngD);
    }
  }

  /**
   * Point to rectangle minimum distance.
   * 
   * Previous version, only around for reference.
   * 
   * Complexity:
   * <ul>
   * <li>Trivial cases (on longitude slice): no trigonometric functions.</li>
   * <li>Cross-track case: 10+2 trig</li>
   * <li>Corner case: 10+3 trig, 2 sqrt</li>
   * </ul>
   * 
   * Reference:
   * <p>
   * Erich Schubert, Arthur Zimek and Hans-Peter Kriegel<br />
   * Geodetic Distance Queries on R-Trees for Indexing Geographic Data<br />
   * Advances in Spatial and Temporal Databases - 13th International Symposium,
   * SSTD 2013, Munich, Germany
   * </p>
   * 
   * @param plat Latitude of query point.
   * @param plng Longitude of query point.
   * @param rminlat Min latitude of rectangle.
   * @param rminlng Min longitude of rectangle.
   * @param rmaxlat Max latitude of rectangle.
   * @param rmaxlng Max longitude of rectangle.
   * @return Distance in radians
   */
  @Reference(authors = "Erich Schubert, Arthur Zimek and Hans-Peter Kriegel", title = "Geodetic Distance Queries on R-Trees for Indexing Geographic Data", booktitle = "Advances in Spatial and Temporal Databases - 13th International Symposium, SSTD 2013, Munich, Germany")
  public static double latlngMinDistRadFull(double plat, double plng, double rminlat, double rminlng, double rmaxlat, double rmaxlng) {
    // FIXME: handle rectangles crossing the +-180 deg boundary correctly!

    // Degenerate rectangles:
    if ((rminlat >= rmaxlat) && (rminlng >= rmaxlng)) {
      return haversineFormulaRad(rminlat, rminlng, plat, plng);
    }

    // The simplest case is when the query point is in the same "slice":
    if (rminlng <= plng && plng <= rmaxlng) {
      // Inside rectangle:
      if (rminlat <= plat && plat <= rmaxlat) {
        return 0;
      }
      // South:
      if (plat < rminlat) {
        return rminlat - plat;
      } else {
        // plat > rmaxlat
        return plat - rmaxlat;
      }
    }

    // Determine whether going east or west is shorter.
    double lngE = rminlng - plng;
    if (lngE < 0) {
      lngE += MathUtil.TWOPI;
    }
    double lngW = rmaxlng - plng; // we keep this negative!
    if (lngW > 0) {
      lngW -= MathUtil.TWOPI;
    }

    // Compute sine and cosine values we will certainly need below:
    final double slatQ = Math.sin(plat), clatQ = MathUtil.sinToCos(plat, slatQ);
    final double slatN = Math.sin(rmaxlat), clatN = MathUtil.sinToCos(rmaxlat, slatN);
    final double slatS = Math.sin(rminlat), clatS = MathUtil.sinToCos(rminlat, slatS);

    // East, to min edge:
    if (lngE <= -lngW) {
      final double slngD = Math.sin(lngE);
      final double clngD = MathUtil.sinToCos(lngE, slngD);

      // Bearing to south
      // Math.atan2(slngD * clatS, clatQ * slatS - slatQ * clatS * clngD);
      // Bearing from south
      final double bs = Math.atan2(slngD * clatQ, clatS * slatQ - slatS * clatQ * clngD);
      // Bearing to north
      // Math.atan2(slngD * clatN, clatQ * slatN - slatQ * clatN * clngD);
      // Bearing from north
      final double bn = Math.atan2(slngD * clatQ, clatN * slatQ - slatN * clatQ * clngD);
      if (bs < MathUtil.HALFPI) {
        if (bn > MathUtil.HALFPI) {
          // Radians from south pole = abs(ATD)
          final double radFromS = -MathUtil.HALFPI - plat;

          // Cross-track-distance to longitude line.
          return Math.asin(Math.sin(radFromS) * -slngD);
        }
      }
      if (bs - MathUtil.HALFPI < MathUtil.HALFPI - bn) {
        // Haversine to north corner.
        final double slatN2 = Math.sin((plat - rmaxlat) * .5);
        final double slon = Math.sin(lngE * .5);
        final double aN = slatN2 * slatN2 + slon * slon * clatQ * clatN;
        final double distN = 2 * Math.atan2(Math.sqrt(aN), Math.sqrt(1 - aN));
        return distN;
      } else {
        // Haversine to south corner.
        final double slatS2 = Math.sin((plat - rminlat) * .5);
        final double slon = Math.sin(lngE * .5);
        final double aS = slatS2 * slatS2 + slon * slon * clatQ * clatS;
        final double distS = 2 * Math.atan2(Math.sqrt(aS), Math.sqrt(1 - aS));
        return distS;
      }
    } else { // West, to max edge
      final double slngD = Math.sin(lngW);
      final double clngD = MathUtil.sinToCos(lngW, slngD);

      // Bearing to south
      // Math.atan2(slngD * clatS, clatQ * slatS - slatQ * clatS * clngD);
      // Bearing from south
      final double bs = Math.atan2(slngD * clatQ, clatS * slatQ - slatS * clatQ * clngD);
      // Bearing to north
      // Math.atan2(slngD * clatN, clatQ * slatN - slatQ * clatN * clngD);
      // Bearing from north
      final double bn = Math.atan2(slngD * clatQ, clatN * slatQ - slatN * clatQ * clngD);
      if (bs > -MathUtil.HALFPI) {
        if (bn < -MathUtil.HALFPI) {
          // Radians from south = abs(ATD) = distance from pole
          final double radFromS = -MathUtil.HALFPI - plat;
          // Cross-track-distance to longitude line.
          return Math.asin(Math.sin(radFromS) * slngD);
        }
      }
      if (-MathUtil.HALFPI - bs < bn + MathUtil.HALFPI) {
        // Haversine to north corner.
        final double slatN2 = Math.sin((plat - rmaxlat) * .5);
        final double slon = Math.sin(lngW * .5);
        final double aN = slatN2 * slatN2 + slon * slon * clatQ * clatN;
        final double distN = 2 * Math.atan2(Math.sqrt(aN), Math.sqrt(1 - aN));
        return distN;
      } else {
        // Haversine to south corner.
        final double slatS2 = Math.sin((plat - rminlat) * .5);
        final double slon = Math.sin(lngW * .5);
        final double aS = slatS2 * slatS2 + slon * slon * clatQ * clatS;
        final double distS = 2 * Math.atan2(Math.sqrt(aS), Math.sqrt(1 - aS));
        return distS;
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
    return MathUtil.rad2deg(bearingRad(MathUtil.deg2rad(latS), MathUtil.deg2rad(lngS), MathUtil.deg2rad(latE), MathUtil.deg2rad(lngE)));
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
    final double slatS = Math.sin(latS), clatS = MathUtil.sinToCos(latS, slatS);
    final double slatE = Math.sin(latE), clatE = MathUtil.sinToCos(latE, slatE);
    return Math.atan2(-Math.sin(lngS - lngE) * clatE, clatS * slatE - slatS * clatE * Math.cos(lngS - lngE));
  }
}
