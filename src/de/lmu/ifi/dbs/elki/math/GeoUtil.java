package de.lmu.ifi.dbs.elki.math;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
 * TODO: add ellipsoid version of Vinentry formula.
 * 
 * @author Erich Schubert
 * @author Niels Dörre
 */
public final class GeoUtil {
  /**
   * Earth radius approximation we are using.
   */
  public static final double EARTH_RADIUS = 6371.009; // km.

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
  public static double haversineFormula(double lat1, double lon1, double lat2, double lon2) {
    // Work in radians
    lat1 = MathUtil.deg2rad(lat1);
    lat2 = MathUtil.deg2rad(lat2);
    lon1 = MathUtil.deg2rad(lon1);
    lon2 = MathUtil.deg2rad(lon2);
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
  public static double sphericalVincentyFormula(double lat1, double lon1, double lat2, double lon2) {
    // Work in radians
    lat1 = MathUtil.deg2rad(lat1);
    lat2 = MathUtil.deg2rad(lat2);
    lon1 = MathUtil.deg2rad(lon1);
    lon2 = MathUtil.deg2rad(lon2);
    // Delta
    final double dlon = lon1 - lon2;

    // Spherical special case of Vincenty's formula - no iterations needed
    final double slat1 = Math.sin(lat1);
    final double slat2 = Math.sin(lat2);
    final double slond = Math.sin(dlon * .5);
    final double clat1 = Math.cos(lat1);
    final double clat2 = Math.cos(lat2);
    final double clond = Math.cos(dlon * .5);
    final double a = clat2 * slond;
    final double b = (clat1 * slat2) - (slat1 * clat2 * clond);
    final double d = Math.atan2(Math.sqrt(a * a + b * b), slat1 * slat2 + clat1 * clat2 * clond);
    return EARTH_RADIUS * d;
  }

  /**
   * Compute the cross-track distance.
   * 
   * @param latS Latitude of starting point.
   * @param lonS Longitude of starting point.
   * @param latQ Latitude of query point.
   * @param lonQ Longitude of query point.
   * @param latE Latitude of destination point.
   * @param lonE Longitude of destination point.
   * @param distSE Distance from starting point to query point.
   * @return Cross-track distance in km. May be negative - this gives the side.
   */
  public static double crossTrackDistance(double latS, double lonS, double latQ, double lonQ, double latE, double lonE, double distSQ) {
    // Convert to radians.
    latS = MathUtil.deg2rad(latS);
    latQ = MathUtil.deg2rad(latQ);
    latE = MathUtil.deg2rad(latE);
    lonS = MathUtil.deg2rad(lonS);
    lonQ = MathUtil.deg2rad(lonQ);
    lonE = MathUtil.deg2rad(lonE);
    final double dlonSE = lonE - lonS;
    final double dlonSQ = lonQ - lonS;

    // Compute trigonometric functions only once.
    final double slatS = Math.sin(latS);
    final double slatQ = Math.sin(latQ);
    final double slatE = Math.sin(latE);
    final double clatS = Math.cos(latS);
    final double clatQ = Math.cos(latQ);
    final double clatE = Math.cos(latE);

    // Compute the course
    final double crsSE, crsSQ;
    {
      // y = sin(dlon) * cos(lat2)
      double yE = Math.sin(dlonSE) * clatE;
      double yQ = Math.sin(dlonSQ) * clatQ;

      // x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dlon)
      double xE = clatS * slatE - slatS * clatE * Math.cos(dlonSE);
      double xQ = clatS * slatQ - slatS * clatQ * Math.cos(dlonSQ);

      crsSE = Math.atan2(yE, xE);
      crsSQ = Math.atan2(yQ, xQ);
    }

    // Calculate cross-track distance
    return EARTH_RADIUS * Math.asin(Math.sin(distSQ / EARTH_RADIUS) * Math.sin(crsSQ - crsSE));
  }

  /**
   * Compute the cross-track distance.
   * 
   * XTD = asin(sin(dist_SQ)*sin(crs_SQ-crs_SE))
   * 
   * @param latS Latitude of starting point.
   * @param lonS Longitude of starting point.
   * @param latQ Latitude of query point.
   * @param lonQ Longitude of query point.
   * @param latE Latitude of destination point.
   * @param lonE Longitude of destination point.
   * @return Cross-track distance in km. May be negative - this gives the side.
   */
  public static double crossTrackDistance(double latS, double lonS, double latQ, double lonQ, double latE, double lonE) {
    // Convert to radians.
    latS = MathUtil.deg2rad(latS);
    latQ = MathUtil.deg2rad(latQ);
    latE = MathUtil.deg2rad(latE);
    lonS = MathUtil.deg2rad(lonS);
    lonQ = MathUtil.deg2rad(lonQ);
    lonE = MathUtil.deg2rad(lonE);
    final double dlonSE = lonE - lonS;
    final double dlonSQ = lonQ - lonS;

    // Compute trigonometric functions only once.
    final double clatS = Math.cos(latS);
    final double clatQ = Math.cos(latQ);
    final double clatE = Math.cos(latE);
    final double slatS = Math.sin(latS);
    final double slatQ = Math.sin(latQ);
    final double slatE = Math.sin(latE);

    // Haversine formula, higher precision at < 1 meters but maybe issues at
    // antipodal points - we do not yet multiply with the radius!
    double angDistSQ;
    {
      final double slat = Math.sin((latQ - latS) * .5);
      final double slon = Math.sin(dlonSQ * .5);
      final double a = slat * slat + slon * slon * clatS * clatQ;
      angDistSQ = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // Compute the course
    final double crsSE, crsSQ;
    {
      // y = sin(dlon) * cos(lat2)
      double yE = Math.sin(dlonSE) * clatE;
      double yQ = Math.sin(dlonSQ) * clatQ;

      // x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dlon)
      double xE = clatS * slatE - slatS * clatE * Math.cos(dlonSE);
      double xQ = clatS * slatQ - slatS * clatQ * Math.cos(dlonSQ);

      crsSE = Math.atan2(yE, xE);
      crsSQ = Math.atan2(yQ, xQ);
    }

    // Calculate cross-track distance
    return EARTH_RADIUS * Math.asin(Math.sin(angDistSQ) * Math.sin(crsSQ - crsSE));
  }

  /**
   * The along track distance, is the distance from S to Q along the track S to
   * E.
   * 
   * ATD=acos(cos(dist_SQ)/cos(XTD))
   * 
   * FIXME: can we get a proper sign into this?
   * 
   * @param latS Latitude of starting point.
   * @param lonS Longitude of starting point.
   * @param latQ Latitude of query point.
   * @param lonQ Longitude of query point.
   * @param latE Latitude of destination point.
   * @param lonE Longitude of destination point.
   * @return Along-track distance in km. May be negative - this gives the side.
   */
  public static double alongTrackDistance(double latS, double lonS, double latQ, double lonQ, double latE, double lonE) {
    double distSQ = haversineFormula(latS, lonS, latQ, lonQ);
    double ctd = crossTrackDistance(latS, lonS, latQ, lonQ, latE, lonE, distSQ);
    return EARTH_RADIUS * Math.acos(Math.cos(distSQ / EARTH_RADIUS) / Math.cos(ctd / EARTH_RADIUS));
    // TODO: for short distances, use this instead:
    // asin(sqrt( (sin(dist_AD))^2 - (sin(XTD))^2 )/cos(XTD))
  }

  /**
   * Point to rectangle minimum distance.
   * 
   * @param plat Latitude of query point.
   * @param plng Longitude of query point.
   * @param rminlat Min latitude of rectangle.
   * @param rminlng Min longitude of rectangle.
   * @param rmaxlat Max latitude of rectangle.
   * @param rmaxlng Max longitude of rectangle.
   * @return Distance
   */
  public static double latlngMinDist(double plat, double plng, double rminlat, double rminlng, double rmaxlat, double rmaxlng) {
    // FIXME: handle rectangles crossing the +-180 deg boundary correctly!

    // Degenerate rectangles:
    if ((rminlat >= rmaxlat) && (rminlng >= rmaxlng)) {
      return haversineFormula(rminlat, rminlng, plat, plng);
    }

    // The simplest case is when the query point is in the same "slice":
    if (rminlng <= plng && plng <= rmaxlng) {
      // Inside rectangle:
      if (rminlat <= plat && plat <= rmaxlat) {
        return 0;
      }
      // South:
      if (plat < rminlat) {
        return EARTH_RADIUS * MathUtil.deg2rad(rminlat - plat);
      } else {
        // plat > rmaxlat
        return EARTH_RADIUS * MathUtil.deg2rad(plat - rmaxlat);
      }
    }

    // Determine whether going east or west is shorter.
    double lngE = rminlng - plng;
    if (lngE < 0) {
      lngE += 360.;
    }
    double lngW = plng - rmaxlng;
    if (lngW < 0) {
      lngW += 360.;
    }
    // East, to min edge:
    if (lngE <= lngW) {
      double distN = haversineFormula(plat, plng, rmaxlat, rminlng);
      double distS = haversineFormula(plat, plng, rminlat, rminlng);
      double distC = crossTrackDistance(rminlat, rminlng, plat, plng, rmaxlat, rminlng);
      return Math.min(distN, Math.min(distS, distC));
    } else { // West, to max edge
      double distN = haversineFormula(plat, plng, rmaxlat, rmaxlng);
      double distS = haversineFormula(plat, plng, rminlat, rmaxlng);
      double distC = crossTrackDistance(rminlat, rmaxlng, plat, plng, rmaxlat, rmaxlng);
      return Math.min(distN, Math.min(distS, distC));
    }
  }
}
