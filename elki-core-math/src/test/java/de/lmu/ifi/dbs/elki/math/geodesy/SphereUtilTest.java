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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.MathUtil;

/**
 * Unit tests for SphereUtil.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class SphereUtilTest {
  @Test
  public void testMinDist() {
    // Bounding box of Argentinia:
    double[] a = new double[] { -55.1850776672, -73.5603103638, -21.7811660767, -53.6374511719 };
    // Test cases.
    // Lon, lat, min dist, distance to top right corner, earth radius, error
    double[][] tests = { //
        // Low precision reference values computed with
        // http://www.movable-type.co.uk/scripts/latlong.html
        // Halifax (north), Sao Paolo and Pelotas (east), Christchurch
        // (southwest), Honolulu (northwest), Guam (southwest)
        // Kritimati (northwest), Hanga Roa (west)
        { 44.64533, -63.57239, 7386, 7455, 6371., 1e-4 }, // HX
        { -23.5475, -46.63611, 713.6, 744.4, 6371., 1e-4 }, // SP
        { -31.77194, -52.3425, 122.4, 1118, 6371., 1e-4 }, // PE
        { -43.53333, 172.63333, 7397, 11350, 6371., 7e-4 }, // CC
        { 21.30694, -157.85833, 10320, 12270, 6371., 7e-4 }, // HO
        { 13.4441674, 126.8578317, 15050, 19090, 6371., 7e-4 }, // GU
        { 1.8709366, -157.5032895, 9460, 11520, 6371., 7e-4 }, // KI
        { -27.15474, -109.43241, 3494, 5634, 6371., 1e-4 }, // HR
    };
    for(double[] t : tests) {
      assertEquals("Distance does not match", t[2], SphereUtil.latlngMinDistDeg(t[0], t[1], a[0], a[1], a[2], a[3]) * t[4], t[5] * t[4]);
      assertEquals("Distance does not match", t[2], SphereUtil.latlngMinDistRadFull( MathUtil.deg2rad(t[0]), MathUtil.deg2rad(t[1]),
          MathUtil.deg2rad(a[0]), MathUtil.deg2rad(a[1]), MathUtil.deg2rad(a[2]), MathUtil.deg2rad(a[3])) * t[4], t[5] * t[4]);
      assertEquals("Distance does not match", t[3], SphereUtil.haversineFormulaDeg(t[0], t[1], a[2], a[3]) * t[4], t[5] * t[4]);
    }
  }
}
