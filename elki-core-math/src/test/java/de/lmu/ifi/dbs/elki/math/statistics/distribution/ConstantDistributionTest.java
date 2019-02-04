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
package de.lmu.ifi.dbs.elki.math.statistics.distribution;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ClassInstantiationException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test for the constant distribution in ELKI.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ConstantDistributionTest extends AbstractDistributionTest {
  @Test
  public void testPDF() {
    final double val = 2.;
    ConstantDistribution d = new ConstantDistribution(val);
    assertEquals("Not zero at neginf", 0., d.pdf(Double.NEGATIVE_INFINITY), 0.);
    assertEquals("Not zero at almost neginf", 0., d.pdf(-Double.MAX_VALUE), 0.);
    assertEquals("Not zero just below x", 0., d.pdf(Math.nextDown(val)), 0.);
    assertEquals("Not infinite at x", Double.MAX_VALUE, d.pdf(val), 0.);
    assertEquals("Not zero just after x", 0., d.pdf(Math.nextUp(val)), 0.);
    assertEquals("Not zero at posinf", 0., d.pdf(Double.POSITIVE_INFINITY), 0.);
    assertEquals("Not zero at almost posinf", 0., d.pdf(Double.MAX_VALUE), 0.);
  }

  @Test
  public void testLogPDF() {
    final double val = 2.;
    ConstantDistribution d = new ConstantDistribution(val);
    assertEquals("Not zero at neginf", Double.NEGATIVE_INFINITY, d.logpdf(Double.NEGATIVE_INFINITY), 0.);
    assertEquals("Not zero at almost neginf", Double.NEGATIVE_INFINITY, d.logpdf(-Double.MAX_VALUE), 0.);
    assertEquals("Not zero just below x", Double.NEGATIVE_INFINITY, d.logpdf(Math.nextDown(val)), 0.);
    assertEquals("Not infinite at x", Double.MAX_VALUE, d.logpdf(val), 0.);
    assertEquals("Not zero just after x", Double.NEGATIVE_INFINITY, d.logpdf(Math.nextUp(val)), 0.);
    assertEquals("Not zero at posinf", Double.NEGATIVE_INFINITY, d.logpdf(Double.POSITIVE_INFINITY), 0.);
    assertEquals("Not zero at almost posinf", Double.NEGATIVE_INFINITY, d.logpdf(Double.MAX_VALUE), 0.);
  }

  @Test
  public void testCDF() {
    final double val = 2.;
    ConstantDistribution d = new ConstantDistribution(val);
    assertEquals("Not zero at neginf", 0., d.cdf(Double.NEGATIVE_INFINITY), 0.);
    assertEquals("Not zero at almost neginf", 0., d.cdf(-Double.MAX_VALUE), 0.);
    assertEquals("Not zero just below x", 0., d.cdf(Math.nextDown(val)), 0.);
    assertEquals("Not infinite at x", 1, d.cdf(val), 0.);
    assertEquals("Not one just after x", 1., d.cdf(Math.nextUp(val)), 0.);
    assertEquals("Not one at posinf", 1., d.cdf(Double.POSITIVE_INFINITY), 0.);
    assertEquals("Not one at almost posinf", 1., d.cdf(Double.MAX_VALUE), 0.);
  }

  @Test
  public void testQuantile() {
    final double val = 2.;
    ConstantDistribution d = new ConstantDistribution(val);
    assertEquals("Not x at zero", val, d.quantile(0.), 0.);
    assertEquals("Not x at 0.5", val, d.quantile(.5), 0.);
    assertEquals("Not x at one", val, d.quantile(1.), 0.);
  }

  @Test
  public void testParameterizer() throws ClassInstantiationException {
    ListParameterization params = new ListParameterization();
    params.addParameter(ConstantDistribution.Parameterizer.CONSTANT_ID, 2.);
    Distribution dist = ClassGenericsUtil.parameterizeOrAbort(ConstantDistribution.class, params);
    assertEquals(dist.nextRandom(), 2, 0.);
  }

  @Test
  public void testRandom() {
    final double val = 2.;
    ConstantDistribution d = new ConstantDistribution(val);
    assertEquals("Random not x", val, d.nextRandom(), 0.);
    assertEquals("Random not x", val, d.nextRandom(), 0.);
    assertEquals("Random not x", val, d.nextRandom(), 0.);
  }
}
