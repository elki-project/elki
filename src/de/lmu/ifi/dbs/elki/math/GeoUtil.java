package de.lmu.ifi.dbs.elki.math;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Class with utility functions for geographic computations.
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
 */
@Reference(authors = "Ed Williams", title = "Aviation Formulary", booktitle = "", url = "http://williams.best.vwh.net/avform.htm")
public final class GeoUtil {
  /**
   * Earth radius approximation in km.
   */
  public static final double EARTH_RADIUS = 6371.009; // km.

  /**
   * Radius of the WGS84 Ellipsoid in km.
   */
  public static final double WGS84_RADIUS = 6378.137; // km

  /**
   * Flattening of the WGS84 Ellipsoid.
   */
  public static final double WGS84_FLATTENING = 0.00335281066474748;

  /**
   * Eccentricity squared of the WGS84 Ellipsoid
   */
  public static final double WGS84_ECCENTRICITY_SQUARED = 2 * WGS84_FLATTENING - (WGS84_FLATTENING * WGS84_FLATTENING);

  /**
   * Dummy constructor. Do not instantiate.
   */
  private GeoUtil() {
    // Use static methods. Do not intantiate
  }

  /**
   * Compute the approximate on-earth-surface distance of two points using the
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
   * @return Distance in km (approximately)
   */
  @Reference(authors = "Sinnott, R. W.", title = "Virtues of the Haversine", booktitle = "Sky and telescope, 68-2, 1984")
  public static double haversineFormulaDeg(double lat1, double lon1, double lat2, double lon2) {
    // Convert to radians:
    lat1 = MathUtil.deg2rad(lat1);
    lat2 = MathUtil.deg2rad(lat2);
    lon1 = MathUtil.deg2rad(lon1);
    lon2 = MathUtil.deg2rad(lon2);
    return haversineFormulaRad(lat1, lon1, lat2, lon2);
  }

  /**
   * Compute the approximate on-earth-surface distance of two points using the
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
   * @return Distance in km (approximately)
   */
  @Reference(authors = "Sinnott, R. W.", title = "Virtues of the Haversine", booktitle = "Sky and telescope, 68-2, 1984")
  public static double haversineFormulaRad(double lat1, double lon1, double lat2, double lon2) {
    // Haversine formula, higher precision at < 1 meters but maybe issues at
    // antipodal points.
    final double slat = Math.sin((lat1 - lat2) * .5);
    final double slon = Math.sin((lon1 - lon2) * .5);
    final double a = slat * slat + slon * slon * Math.cos(lat1) * Math.cos(lat2);
    final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return EARTH_RADIUS * c;
  }

  /**
   * Compute the approximate on-earth-surface distance of two points.
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
   * @return Distance in km (approximately)
   */
  @Reference(authors = "T. Vincenty", title = "Direct and inverse solutions of geodesics on the ellipsoid with application of nested equations", booktitle = "Survey review 23 176, 1975", url = "http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf")
  public static double sphericalVincentyFormulaDeg(double lat1, double lon1, double lat2, double lon2) {
    // Work in radians
    lat1 = MathUtil.deg2rad(lat1);
    lat2 = MathUtil.deg2rad(lat2);
    lon1 = MathUtil.deg2rad(lon1);
    lon2 = MathUtil.deg2rad(lon2);
    return sphericalVincentyFormulaRad(lat1, lon1, lat2, lon2);
  }

  /**
   * Compute the approximate on-earth-surface distance of two points.
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
   * @return Distance in km (approximately)
   */
  @Reference(authors = "T. Vincenty", title = "Direct and inverse solutions of geodesics on the ellipsoid with application of nested equations", booktitle = "Survey review 23 176, 1975", url = "http://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf")
  public static double sphericalVincentyFormulaRad(double lat1, double lon1, double lat2, double lon2) {
    // Delta
    final double dlon = lon1 - lon2;

    // Spherical special case of Vincenty's formula - no iterations needed
    final double slat1 = Math.sin(lat1);
    final double slat2 = Math.sin(lat2);
    final double slond = Math.sin(dlon * .5);
    final double clat1 = MathUtil.sinToCos(lat1, slat1);
    final double clat2 = MathUtil.sinToCos(lat2, slat2);
    final double clond = MathUtil.sinToCos(dlon * .5, slond);
    final double a = clat2 * slond;
    final double b = (clat1 * slat2) - (slat1 * clat2 * clond);
    final double d = Math.atan2(Math.sqrt(a * a + b * b), slat1 * slat2 + clat1 * clat2 * clond);
    return EARTH_RADIUS * d;
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
   * @param dist1Q Distance from starting point to query point in km.
   * @return Cross-track distance in km. May be negative - this gives the side.
   */
  public static double crossTrackDistanceDeg(double lat1, double lon1, double lat2, double lon2, double latQ, double lonQ, double dist1Q) {
    // Convert to radians.
    lat1 = MathUtil.deg2rad(lat1);
    latQ = MathUtil.deg2rad(latQ);
    lat2 = MathUtil.deg2rad(lat2);
    lon1 = MathUtil.deg2rad(lon1);
    lonQ = MathUtil.deg2rad(lonQ);
    lon2 = MathUtil.deg2rad(lon2);
    return crossTrackDistanceRad(lat1, lon1, lat2, lon2, latQ, lonQ, dist1Q);
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
   * @param dist1Q Distance from starting point to query point in km.
   * @return Cross-track distance in km. May be negative - this gives the side.
   */
  public static double crossTrackDistanceRad(double lat1, double lon1, double lat2, double lon2, double latQ, double lonQ, double dist1Q) {
    final double dlon12 = lon2 - lon1;
    final double dlon1Q = lonQ - lon1;

    // Compute trigonometric functions only once.
    final double slat1 = Math.sin(lat1);
    final double slatQ = Math.sin(latQ);
    final double slat2 = Math.sin(lat2);
    final double clat1 = MathUtil.sinToCos(lat1, slat1);
    final double clatQ = MathUtil.sinToCos(latQ, slatQ);
    final double clat2 = MathUtil.sinToCos(lat2, slat2);

    // Compute the course
    final double crs12, crs1Q;
    {
      // y = sin(dlon) * cos(lat2)
      final double sdlon12 = Math.sin(dlon12);
      final double cdlon12 = MathUtil.sinToCos(dlon12, sdlon12);
      final double sdlon1Q = Math.sin(dlon1Q);
      final double cdlon1Q = MathUtil.sinToCos(dlon1Q, sdlon1Q);

      double yE = sdlon12 * clat2;
      double yQ = sdlon1Q * clatQ;

      // x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dlon)
      double xE = clat1 * slat2 - slat1 * clat2 * cdlon12;
      double xQ = clat1 * slatQ - slat1 * clatQ * cdlon1Q;

      crs12 = Math.atan2(yE, xE);
      crs1Q = Math.atan2(yQ, xQ);
    }

    // Calculate cross-track distance
    return EARTH_RADIUS * Math.asin(Math.sin(dist1Q / EARTH_RADIUS) * Math.sin(crs1Q - crs12));
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
  public static double crossTrackDistance(double lat1, double lon1, double lat2, double lon2, double latQ, double lonQ) {
    // Convert to radians.
    lat1 = MathUtil.deg2rad(lat1);
    latQ = MathUtil.deg2rad(latQ);
    lat2 = MathUtil.deg2rad(lat2);
    lon1 = MathUtil.deg2rad(lon1);
    lonQ = MathUtil.deg2rad(lonQ);
    lon2 = MathUtil.deg2rad(lon2);
    return crossTrackDistanceRad(lat1, lon1, lat2, lon2, latQ, lonQ);
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

    // Compute trigonometric functions only once.
    final double clat1 = Math.cos(lat1);
    final double clatQ = Math.cos(latQ);
    final double clat2 = Math.cos(lat2);
    final double slat1 = MathUtil.cosToSin(lat1, clat1);
    final double slatQ = MathUtil.cosToSin(latQ, clatQ);
    final double slat2 = MathUtil.cosToSin(lat2, clat2);

    // Haversine formula, higher precision at < 1 meters but maybe issues at
    // antipodal points - we do not yet multiply with the radius!
    double angDist1Q;
    {
      final double slat = Math.sin((latQ - lat1) * .5);
      final double slon = Math.sin(dlon1Q * .5);
      final double a = slat * slat + slon * slon * clat1 * clatQ;
      angDist1Q = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // Compute the course
    final double crs12, crs1Q;
    {
      // y = sin(dlon) * cos(lat2)
      final double sdlon12 = Math.sin(dlon12);
      final double sdlon1Q = Math.sin(dlon1Q);
      final double cdlon12 = MathUtil.sinToCos(dlon12, sdlon12);
      final double cdlon1Q = MathUtil.sinToCos(dlon1Q, sdlon1Q);
      double yE = sdlon12 * clat2;
      double yQ = sdlon1Q * clatQ;

      // x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dlon)
      double xE = clat1 * slat2 - slat1 * clat2 * cdlon12;
      double xQ = clat1 * slatQ - slat1 * clatQ * cdlon1Q;

      crs12 = Math.atan2(yE, xE);
      crs1Q = Math.atan2(yQ, xQ);
    }

    // Calculate cross-track distance
    return EARTH_RADIUS * Math.asin(Math.sin(angDist1Q) * Math.sin(crs1Q - crs12));
  }

  /**
   * The along track distance, is the distance from S to Q along the track S to
   * E.
   * 
   * ATD=acos(cos(dist_1Q)/cos(XTD))
   * 
   * FIXME: can we get a proper sign into this?
   * 
   * @param lat1 Latitude of starting point.
   * @param lon1 Longitude of starting point.
   * @param lat2 Latitude of destination point.
   * @param lon2 Longitude of destination point.
   * @param latQ Latitude of query point.
   * @param lonQ Longitude of query point.
   * @return Along-track distance in km. May be negative - this gives the side.
   */
  public static double alongTrackDistance(double lat1, double lon1, double lat2, double lon2, double latQ, double lonQ) {
    double dist1Q = haversineFormulaDeg(lat1, lon1, latQ, lonQ);
    double ctd = crossTrackDistanceDeg(lat1, lon1, lat2, lon2, latQ, lonQ, dist1Q);
    return alongTrackDistance(lat1, lon1, lat2, lon2, latQ, lonQ, dist1Q, ctd);
  }

  /**
   * The along track distance, is the distance from S to Q along the track S to
   * E.
   * 
   * ATD=acos(cos(dist_SQ)/cos(XTD))
   * 
   * FIXME: can we get a proper sign into this?
   * 
   * @param lat1 Latitude of starting point.
   * @param lon1 Longitude of starting point.
   * @param lat2 Latitude of destination point.
   * @param lon2 Longitude of destination point.
   * @param latQ Latitude of query point.
   * @param lonQ Longitude of query point.
   * @param dist1Q Distance S to Q
   * @param ctd Cross-track-distance
   * @return Along-track distance in km. May be negative - this gives the side.
   */
  public static double alongTrackDistance(double lat1, double lon1, double lat2, double lon2, double latQ, double lonQ, double dist1Q, double ctd) {
    // TODO: optimize the sign computation!
    int sign = Math.abs(bearing(lat1, lon1, lat2, lon2) - bearing(lat1, lon1, latQ, lonQ)) < MathUtil.HALFPI ? +1 : -1;
    return sign * EARTH_RADIUS * Math.acos(Math.cos(dist1Q / EARTH_RADIUS) / Math.cos(ctd / EARTH_RADIUS));
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
   * @param plat Latitude of query point.
   * @param plng Longitude of query point.
   * @param rminlat Min latitude of rectangle.
   * @param rminlng Min longitude of rectangle.
   * @param rmaxlat Max latitude of rectangle.
   * @param rmaxlng Max longitude of rectangle.
   * @return Distance
   */
  public static double latlngMinDistDeg(double plat, double plng, double rminlat, double rminlng, double rmaxlat, double rmaxlng) {
    // Convert to radians.
    plat = MathUtil.deg2rad(plat);
    plng = MathUtil.deg2rad(plng);
    rminlat = MathUtil.deg2rad(rminlat);
    rminlng = MathUtil.deg2rad(rminlng);
    rmaxlat = MathUtil.deg2rad(rmaxlat);
    rmaxlng = MathUtil.deg2rad(rmaxlng);

    return latlngMinDistRad(plat, plng, rminlat, rminlng, rmaxlat, rmaxlng);
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
   * @param plat Latitude of query point.
   * @param plng Longitude of query point.
   * @param rminlat Min latitude of rectangle.
   * @param rminlng Min longitude of rectangle.
   * @param rmaxlat Max latitude of rectangle.
   * @param rmaxlng Max longitude of rectangle.
   * @return Distance
   */
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
        return EARTH_RADIUS * (rminlat - plat);
      } else {
        // plat > rmaxlat
        return EARTH_RADIUS * (plat - rmaxlat);
      }
    }

    // Determine whether going east or west is shorter.
    double lngE = rminlng - plng;
    lngE += (lngE < 0) ? MathUtil.TWOPI : 0;
    double lngW = rmaxlng - plng;
    lngW -= (lngW > 0) ? MathUtil.TWOPI : 0;

    // Compute sine and cosine values we will certainly need below:
    final double slatQ = Math.sin(plat);
    final double clatQ = MathUtil.sinToCos(plat, slatQ);
    final double slatN = Math.sin(rmaxlat);
    final double clatN = MathUtil.sinToCos(rmaxlat, slatN);
    final double slatS = Math.sin(rminlat);
    final double clatS = MathUtil.sinToCos(rminlat, slatS);

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
          return EARTH_RADIUS * Math.asin(Math.sin(radFromS) * -slngD);
        }
      }
      if (bs - MathUtil.HALFPI < MathUtil.HALFPI - bn) {
        // Haversine to north corner.
        final double slatN2 = Math.sin((plat - rmaxlat) * .5);
        final double slon = Math.sin(lngE * .5);
        final double aN = slatN2 * slatN2 + slon * slon * clatQ * clatN;
        final double distN = 2 * Math.atan2(Math.sqrt(aN), Math.sqrt(1 - aN));
        return EARTH_RADIUS * distN;
      } else {
        // Haversine to south corner.
        final double slatS2 = Math.sin((plat - rminlat) * .5);
        final double slon = Math.sin(lngE * .5);
        final double aS = slatS2 * slatS2 + slon * slon * clatQ * clatS;
        final double distS = 2 * Math.atan2(Math.sqrt(aS), Math.sqrt(1 - aS));
        return EARTH_RADIUS * distS;
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
          return EARTH_RADIUS * Math.asin(Math.sin(radFromS) * slngD);
        }
      }
      if (-MathUtil.HALFPI - bs < bn + MathUtil.HALFPI) {
        // Haversine to north corner.
        final double slatN2 = Math.sin((plat - rmaxlat) * .5);
        final double slon = Math.sin(lngW * .5);
        final double aN = slatN2 * slatN2 + slon * slon * clatQ * clatN;
        final double distN = 2 * Math.atan2(Math.sqrt(aN), Math.sqrt(1 - aN));
        return EARTH_RADIUS * distN;
      } else {
        // Haversine to south corner.
        final double slatS2 = Math.sin((plat - rminlat) * .5);
        final double slon = Math.sin(lngW * .5);
        final double aS = slatS2 * slatS2 + slon * slon * clatQ * clatS;
        final double distS = 2 * Math.atan2(Math.sqrt(aS), Math.sqrt(1 - aS));
        return EARTH_RADIUS * distS;
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
   * @return Bearing in radians
   */
  public static double bearing(double latS, double lngS, double latE, double lngE) {
    latS = MathUtil.deg2rad(latS);
    latE = MathUtil.deg2rad(latE);
    lngS = MathUtil.deg2rad(lngS);
    lngE = MathUtil.deg2rad(lngE);
    final double slatS = Math.sin(latS);
    final double clatS = MathUtil.sinToCos(latS, slatS);
    final double slatE = Math.sin(latE);
    final double clatE = MathUtil.sinToCos(latE, slatE);
    return Math.atan2(-Math.sin(lngS - lngE) * clatE, clatS * slatE - slatS * clatE * Math.cos(lngS - lngE));
  }

  /**
   * Map a latitude,longitude pair to 3D X-Y-Z coordinates, using athe WGS84
   * ellipsoid.
   * 
   * The coordinate system is chosen such that the earth rotates around the Z
   * axis.
   * 
   * @param lat Latitude in degree
   * @param lng Longitude in degree
   * @return Coordinate triple
   */
  public static double[] latLngDegToXZYWGS84(double lat, double lng) {
    // Switch to radians:
    lat = Math.toRadians(lat);
    lng = Math.toRadians(lng);
    // Sine and cosines:
    final double clat = Math.cos(lat), slat = MathUtil.cosToSin(lat, clat);
    final double clng = Math.cos(lng), slng = MathUtil.cosToSin(lng, clng);

    // Eccentricity squared
    final double v = WGS84_RADIUS / (Math.sqrt(1 - WGS84_ECCENTRICITY_SQUARED * slat * slat));

    return new double[] { v * clat * clng, v * clat * slng, (1 - WGS84_ECCENTRICITY_SQUARED) * v * slat };
  }

  /**
   * Convert Latitude-Longitude pair to X-Y-Z coordinates using a spherical
   * approximation of the earth.
   * 
   * The coordinate system is chosen such that the earth rotates around the Z
   * axis.
   * 
   * @param lat Latitude in degree
   * @param lng Longitude in degree
   * @return Coordinate triple
   */
  public static double[] latLngDegToXZY(double lat, double lng) {
    // Map to radians.
    lat = MathUtil.rad2deg(lat);
    lng = MathUtil.rad2deg(lng);
    // Sine and cosines:
    final double clat = Math.cos(lat), slat = MathUtil.cosToSin(lat, clat);
    final double clng = Math.cos(lng), slng = MathUtil.cosToSin(lng, clng);
    return new double[] { EARTH_RADIUS * clat * clng, EARTH_RADIUS * clat * slng, EARTH_RADIUS * slat };
  }

  /**
   * Convert a 3D coordinate pair to the corresponding longitude.
   * 
   * Only x and y are required - z gives the latitude.
   * 
   * @param x X value
   * @param y Y value
   * @return Latitude
   */
  public static double xyzToLatDegWGS84(double x, double y, double z) {
    final double p = Math.sqrt(x * x + y * y);
    double lat = Math.atan2(z, p * (1 - WGS84_ECCENTRICITY_SQUARED));

    // Iteratively improving the lat value
    // TODO: instead of a fixed number of iterations, check for convergence.
    for (int i = 0; i < 10; i++) {
      final double slat = Math.sin(lat);
      final double v = WGS84_RADIUS / (Math.sqrt(1 - WGS84_ECCENTRICITY_SQUARED * slat * slat));
      lat = Math.atan2(z + WGS84_ECCENTRICITY_SQUARED * v * slat, p);
    }

    return MathUtil.rad2deg(lat);
  }

  /**
   * Convert a 3D coordinate pair to the corresponding latitude.
   * 
   * Only the z coordinate is required.
   * 
   * @param z Z value
   * @return Latitude
   */
  public static double xyzToLatDeg(double z) {
    return MathUtil.rad2deg(Math.asin(z / EARTH_RADIUS));
  }

  /**
   * Convert a 3D coordinate pair to the corresponding longitude.
   * 
   * Only x and y are required - z gives the latitude.
   * 
   * @param x X value
   * @param y Y value
   * @return Latitude
   */
  public static double xyzToLngDeg(double x, double y) {
    return MathUtil.rad2deg(Math.atan2(y, x));
  }
}
