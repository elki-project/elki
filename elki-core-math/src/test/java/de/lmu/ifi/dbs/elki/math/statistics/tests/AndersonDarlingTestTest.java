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
package de.lmu.ifi.dbs.elki.math.statistics.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Validate the Anderson Darling test.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class AndersonDarlingTestTest {
  double[][] noncentral = { //
  { -1.877827740862672, -1.2459409569318343, -0.61175121460353787, -0.55626980422641181, //
  -0.35332777321703612, -0.24551480507545068, -0.23557108046219447, -0.080637593585376352, //
  -0.077502677843308831, -0.0011801602029529352, 0.098513344627586555, 0.22387706772067983, //
  0.38308837044892374, 0.74731974202783025, 0.95771043891514229, 1.1182514995437112, //
  1.2388846366569823, 1.2656650520576955, 1.6201272842150696, 1.8546234450024779 //
  }, //
  { -1.877827740862672, -1.2459409569318343, -0.61175121460353787, -0.55626980422641181, //
  -0.35332777321703612, -0.24551480507545068, -0.23557108046219447, -0.080637593585376352, //
  -0.077502677843308831, -0.0011801602029529352, 0, 0.098513344627586555, 0.22387706772067983, //
  0.38308837044892374, 0.74731974202783025, 0.95771043891514229, 1.1182514995437112, //
  1.2388846366569823, 1.2656650520576955, 1.6201272842150696, 1.8546234450024779 //
  }, // With extra 0
  { -1.877827740862672, -1.2459409569318343, -0.61175121460353787, -0.55626980422641181, //
  -0.35332777321703612, -0.24551480507545068, -0.23557108046219447, -0.080637593585376352, //
  -0.077502677843308831, -0.0011801602029529352, 0.098513344627586555, 0.22387706772067983, //
  0.38308837044892374, 0.74731974202783025, 0.95771043891514229, 1.1182514995437112, //
  1.2388846366569823, 1.2656650520576955, 1.6201272842150696, 1.8546234450024779, //
  2, }, //
  { -1.877827740862672, -1.2459409569318343, -0.61175121460353787, -0.55626980422641181, //
  -0.35332777321703612, -0.24551480507545068, -0.23557108046219447, -0.080637593585376352, //
  -0.077502677843308831, -0.0011801602029529352, 0.098513344627586555, 0.22387706772067983, //
  0.38308837044892374, 0.74731974202783025, 0.95771043891514229, 1.1182514995437112, //
  1.2388846366569823, 1.2656650520576955, 1.6201272842150696, 1.8546234450024779, //
  5, }, //
  };

  // Data with mean=0, stddev=1.
  double[][] central = { {//
  -1.7187795778260955, -1.2241890398985336, -1.0887488236579657, -0.9520119517912673, //
  -0.92307167034200843, -0.91511576786724602, -0.88107551241137438, -0.510688871250774, //
  -0.21979733834708015, 0.01595315017109037, 0.29295514173678655, 0.4314599320312914, //
  0.48111670258585942, 0.68219769209038816, 0.73724620797860296, 0.83953102892042453, //
  0.88067069928766939, 0.88856190876314567, 0.94779106981452654, 2.2359950200125591 } };

  double[] scipy_noncentral = { //
  0.25778652975096605, // Cannot be rejected as normal.
  0.31326405765500809, // With extra 0
  0.26251514767422179, // With extra 2
  0.76515121738120229, // With extra 5
  };

  double[] scipy_central = { //
  0.5185360909507537, //
  };

  @Test
  public void testAndersonDarlingTestNoncentral() {
    for(int i = 0; i < noncentral.length; i++) {
      double A2nc = AndersonDarlingTest.A2Noncentral(noncentral[i]);
      assertEquals("A2 does not match for " + i, scipy_noncentral[i], A2nc, 1e-13);
    }
  }

  @Test
  public void testAndersonDarlingTestCentral() {
    for(int i = 0; i < central.length; i++) {
      double A2 = AndersonDarlingTest.A2StandardNormal(central[i]);
      double A2nc = AndersonDarlingTest.A2Noncentral(central[i]);
      assertEquals("A2 does not match for " + i, scipy_central[i], A2, 1e-14);
      assertEquals("A2 does not match for " + i, scipy_central[i], A2nc, 1e-14);
    }
  }
}
