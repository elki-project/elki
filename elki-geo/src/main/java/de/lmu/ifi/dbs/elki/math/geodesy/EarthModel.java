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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * API for handling different earth models.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 * 
 * @assoc - - - SphereUtil
 */
public interface EarthModel {
  /**
   * Parameter to choose the earth model to use.
   */
  OptionID MODEL_ID = new OptionID("geo.model", "Earth model to use for projection. Default: spherical model.");

  /**
   * Map a degree latitude, longitude pair to 3D X-Y-Z coordinates, using a
   * spherical earth model.
   * <p>
   * The coordinate system is usually chosen such that the earth rotates around
   * the Z axis and X points to the prime meridian and Equator.
   * 
   * @param lat Latitude in degree
   * @param lng Longitude in degree
   * @return Coordinate triple, in meters.
   */
  double[] latLngDegToECEF(double lat, double lng);

  /**
   * Map a radians latitude, longitude pair to 3D X-Y-Z coordinates, using a
   * spherical earth model.
   * <p>
   * The coordinate system is usually chosen such that the earth rotates around
   * the Z axis and X points to the prime meridian and Equator.
   * 
   * @param lat Latitude in radians
   * @param lng Longitude in radians
   * @return Coordinate triple, in meters.
   */
  double[] latLngRadToECEF(double lat, double lng);

  /**
   * Map a degree latitude, longitude pair to 3D X-Y-Z coordinates, using a
   * spherical earth model.
   * <p>
   * The coordinate system is usually chosen such that the earth rotates around
   * the Z axis and X points to the prime meridian and Equator.
   * 
   * @param lat Latitude in degree
   * @param lng Longitude in degree
   * @param h Height
   * @return Coordinate triple, in meters.
   */
  double[] latLngDegToECEF(double lat, double lng, double h);

  /**
   * Map a radians latitude, longitude pair to 3D X-Y-Z coordinates, using a
   * spherical earth model.
   * <p>
   * The coordinate system is usually chosen such that the earth rotates around
   * the Z axis and X points to the prime meridian and Equator.
   * 
   * @param lat Latitude in radians
   * @param lng Longitude in radians
   * @param h Height
   * @return Coordinate triple, in meters.
   */
  double[] latLngRadToECEF(double lat, double lng, double h);

  /**
   * Convert a 3D coordinate pair to the corresponding latitude.
   * 
   * @param x X value
   * @param y Y value
   * @param z Z value
   * @return Latitude in degrees
   */
  double ecefToLatDeg(double x, double y, double z);

  /**
   * Convert a 3D coordinate pair to the corresponding latitude.
   * 
   * @param x X value
   * @param y Y value
   * @param z Z value
   * @return Latitude in radians
   */
  double ecefToLatRad(double x, double y, double z);

  /**
   * Convert a 3D coordinate pair to the corresponding longitude.
   * 
   * @param x X value
   * @param y Y value
   * @return Longitude in degrees
   */
  double ecefToLngDeg(double x, double y);

  /**
   * Convert a 3D coordinate pair to the corresponding longitude.
   * 
   * @param x X value
   * @param y Y value
   * @return Longitude in radians
   */
  double ecefToLngRad(double x, double y);

  /**
   * Convert a 3D coordinate pair to the corresponding latitude, longitude and
   * height.
   * <p>
   * Note: if you are not interested in the height, use {@link #ecefToLatDeg}
   * and {@link #ecefToLngDeg} instead, which has a smaller memory footprint.
   * 
   * @param x X value
   * @param y Y value
   * @param z Z value
   * @return Array containing (latitude, longitude, height).
   */
  double[] ecefToLatLngDegHeight(double x, double y, double z);

  /**
   * Convert a 3D coordinate pair to the corresponding latitude, longitude and
   * height.
   * <p>
   * Note: if you are not interested in the height, use {@link #ecefToLatRad}
   * and {@link #ecefToLngRad} instead, which has a smaller memory footprint.
   * 
   * @param x X value
   * @param y Y value
   * @param z Z value
   * @return Array containing (latitude, longitude, height).
   */
  double[] ecefToLatLngRadHeight(double x, double y, double z);

  /**
   * Compute the geodetic distance between two surface coordinates.
   * 
   * @param lat1 Latitude of first in degrees.
   * @param lng1 Longitude of first in degrees.
   * @param lat2 Latitude of second in degrees.
   * @param lng2 Longitude of second in degrees.
   * @return Distance in meters.
   */
  double distanceDeg(double lat1, double lng1, double lat2, double lng2);

  /**
   * Compute the geodetic distance between two surface coordinates.
   * 
   * @param lat1 Latitude of first in radians.
   * @param lng1 Longitude of first in radians.
   * @param lat2 Latitude of second in radians.
   * @param lng2 Longitude of second in radians.
   * @return Distance in meters.
   */
  double distanceRad(double lat1, double lng1, double lat2, double lng2);

  /**
   * Compute a lower bound for the geodetic distance point to rectangle.
   * 
   * @param plat Latitude of point in degrees.
   * @param plng Longitude of point in degrees.
   * @param rminlat Min latitude of rectangle in degrees.
   * @param rminlng Min Longitude of rectangle in degrees.
   * @param rmaxlat Max Latitude of rectangle in degrees.
   * @param rmaxlng Max Longitude of rectangle in degrees.
   * @return Distance in meters.
   */
  double minDistDeg(double plat, double plng, double rminlat, double rminlng, double rmaxlat, double rmaxlng);

  /**
   * Compute a lower bound for the geodetic distance point to rectangle.
   * 
   * @param plat Latitude of point in radians.
   * @param plng Longitude of point in radians.
   * @param rminlat Min latitude of rectangle in radians.
   * @param rminlng Min Longitude of rectangle in radians.
   * @param rmaxlat Max Latitude of rectangle in radians.
   * @param rmaxlng Max Longitude of rectangle in radians.
   * @return Distance in meters.
   */
  double minDistRad(double plat, double plng, double rminlat, double rminlng, double rmaxlat, double rmaxlng);

  /**
   * Equatorial radius
   * 
   * @return Radius
   */
  double getEquatorialRadius();

  /**
   * Polar distance.
   * 
   * @return Distance to poles (= minor radius)
   */
  double getPolarDistance();
}
