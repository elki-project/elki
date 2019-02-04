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
package de.lmu.ifi.dbs.elki.math.statistics.distribution.estimator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import de.lmu.ifi.dbs.elki.math.statistics.distribution.AbstractDistributionTest;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArray;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ClassInstantiationException;
import de.lmu.ifi.dbs.elki.utilities.io.TokenizedReader;
import de.lmu.ifi.dbs.elki.utilities.io.Tokenizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Abstract base class for estimator unit testing.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class AbstractDistributionEstimatorTest {
  HashMap<String, double[]> data;

  protected void load(String name) {
    data = new HashMap<>();
    try (
        InputStream in = new GZIPInputStream(AbstractDistributionTest.class.getResourceAsStream(name)); //
        TokenizedReader reader = new TokenizedReader(Pattern.compile(" "), "\"", Pattern.compile("^\\s*#.*"))) {
      Tokenizer t = reader.getTokenizer();
      DoubleArray buf = new DoubleArray();
      reader.reset(in);
      while(reader.nextLineExceptComments()) {
        assertTrue(t.valid());
        String key = t.getStrippedSubstring();
        buf.clear();
        for(t.advance(); t.valid(); t.advance()) {
          buf.add(t.getDouble());
        }
        data.put(key, buf.toArray());
      }
    }
    catch(IOException e) {
      fail("Cannot load data.");
    }
  }

  protected void assertStat(String stat, double observed, double desired, double expecterr) {
    if(expecterr != expecterr) {
      System.err.println(this.getClass().getSimpleName() + " " + stat + " " + desired + " " + observed + " -> " + (observed - desired));
      return;
    }
    assertEquals(stat + " does not match.", desired + expecterr, observed, 1e-13);
  }

  /**
   * This function is not strictly necessary, as all the estimators will have a
   * {@code STATIC} instance that we can use. But this way, we also cover the
   * parameterization API.
   * 
   * We also verify the {@link DistributionEstimator#getDistributionClass()}
   * method here.
   * 
   * @param cls Class
   * @param <D> Distribution
   * @param <T> Estimator class
   * @return Instance
   */
  public static <D extends Distribution, T extends DistributionEstimator<D>> T instantiate(Class<T> cls, Class<D> dist) {
    try {
      T est = ClassGenericsUtil.tryInstantiate(cls, cls, new ListParameterization());
      assertEquals("Estimator has default toString.", -1, est.toString().indexOf('@'));
      assertEquals("Estimator returns unexpected distribution.", dist, est.getDistributionClass());
      return est;
    }
    catch(ClassInstantiationException e) {
      fail("Could not instantiate");
      return null;
    }
  }
}
