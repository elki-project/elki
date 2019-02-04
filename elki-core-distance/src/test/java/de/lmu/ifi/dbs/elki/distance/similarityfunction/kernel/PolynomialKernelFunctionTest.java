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
package de.lmu.ifi.dbs.elki.distance.similarityfunction.kernel;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;

/**
 * Unit test for Kernel implementation.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class PolynomialKernelFunctionTest {
  @Test
  public void testToyExamplesPoly2() {
    DoubleVector v1 = DoubleVector.wrap(new double[] { 1, 0, 0 });
    DoubleVector v2 = DoubleVector.wrap(new double[] { 0, 1, 0 });
    DoubleVector v3 = DoubleVector.wrap(new double[] { 1, 1, 1 });
    DoubleVector v4 = DoubleVector.wrap(new double[] { .1, .2, .3 });

    PolynomialKernelFunction kernel = new PolynomialKernelFunction(2);
    assertEquals("Linear kernel not correct.", 0., kernel.similarity(v1, v2), 0.);
    assertEquals("Linear kernel not correct.", 1., kernel.similarity(v1, v3), 0.);
    assertEquals("Linear kernel not correct.", .01, kernel.similarity(v1, v4), 1e-13);
    assertEquals("Linear kernel not correct.", 1., kernel.similarity(v2, v3), 0.);
    assertEquals("Linear kernel not correct.", .04, kernel.similarity(v2, v4), 1e-13);
    assertEquals("Linear kernel not correct.", .36, kernel.similarity(v3, v4), 1e-13);
  }

  @Test
  public void testToyExamplesPoly3() {
    DoubleVector v1 = DoubleVector.wrap(new double[] { 1, 0, 0 });
    DoubleVector v2 = DoubleVector.wrap(new double[] { 0, 1, 0 });
    DoubleVector v3 = DoubleVector.wrap(new double[] { 1, 1, 1 });
    DoubleVector v4 = DoubleVector.wrap(new double[] { .1, .2, .3 });

    PolynomialKernelFunction kernel = new PolynomialKernelFunction(3);
    assertEquals("Linear kernel not correct.", 0., kernel.similarity(v1, v2), 0.);
    assertEquals("Linear kernel not correct.", 1., kernel.similarity(v1, v3), 0.);
    assertEquals("Linear kernel not correct.", .001, kernel.similarity(v1, v4), 1e-13);
    assertEquals("Linear kernel not correct.", 1., kernel.similarity(v2, v3), 0.);
    assertEquals("Linear kernel not correct.", .008, kernel.similarity(v2, v4), 1e-13);
    assertEquals("Linear kernel not correct.", .216, kernel.similarity(v3, v4), 1e-13);
  }
}