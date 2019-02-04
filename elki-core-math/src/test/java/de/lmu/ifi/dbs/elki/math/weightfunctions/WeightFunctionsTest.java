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
package de.lmu.ifi.dbs.elki.math.weightfunctions;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.ConstantWeight;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.ErfcStddevWeight;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.ErfcWeight;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.ExponentialWeight;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.GaussStddevWeight;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.GaussWeight;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.LinearWeight;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.weightfunctions.WeightFunction;

/**
 * JUnit test to assert consistency of a couple of Weight functions
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 */
public class WeightFunctionsTest {
  /**
   * Just a 'boring' value test for completeness.
   */
  @Test
  public void testGetWeight() {
    WeightFunction[] wf = { new ConstantWeight(), new ErfcWeight(), new ErfcStddevWeight(), new GaussWeight(), new GaussStddevWeight(), new LinearWeight(), new ExponentialWeight() };
    double[] at0 = { 1.0, 1.0, 1.0, 1.0, 0.3989422804014327, 1.0, 1.0 };
    double[] at01 = { 1.0, 0.8693490686884612, 0.920344325445942, 0.9772372209558107, 0.3969525474770118, 0.91, 0.7943282347242815 };
    double[] at09 = { 1.0, 0.13877499454059491, 0.36812025069351895, 0.15488166189124816, 0.2660852498987548, 0.18999999999999995, 0.12589254117941673 };
    double[] at10 = { 1.0, 0.10000000000000016, 0.31731050786291404, 0.10000000000000002, 0.24197072451914337, 0.09999999999999998, 0.10000000000000002 };

    assert (wf.length == at0.length);
    assert (wf.length == at01.length);
    assert (wf.length == at09.length);
    assert (wf.length == at10.length);

    for(int i = 0; i < wf.length; i++) {
      double val0 = wf[i].getWeight(0, 1, 1);
      double val01 = wf[i].getWeight(0.1, 1, 1);
      double val09 = wf[i].getWeight(0.9, 1, 1);
      double val10 = wf[i].getWeight(1.0, 1, 1);
      assertEquals(wf[i].getClass().getSimpleName() + " at 0.0", at0[i], val0, 1e-15);
      assertEquals(wf[i].getClass().getSimpleName() + " at 0.1", at01[i], val01, 1e-15);
      assertEquals(wf[i].getClass().getSimpleName() + " at 0.9", at09[i], val09, 1e-15);
      assertEquals(wf[i].getClass().getSimpleName() + " at 1.0", at10[i], val10, 1e-15);
    }
  }

}
