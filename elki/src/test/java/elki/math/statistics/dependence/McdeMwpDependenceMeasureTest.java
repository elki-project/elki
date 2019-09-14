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
package elki.math.statistics.dependence;

import elki.utilities.ELKIBuilder;
import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import elki.utilities.random.RandomFactory;
import elki.utils.containers.MwpIndex;

import java.util.Random;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static elki.math.statistics.dependence.MCDEDependenceMeasure.Par.M_ID;

import org.junit.Test;

/**
 * @author Alan Mazankiewicz
 * @author Edouard Fouché
 */

public class McdeMwpDependenceMeasureTest {

  @Test public void runTest() {

    // independent
    double[] indi1 = new double[1000];
    double[] indi2 = new double[1000];

    RandomFactory rnd = new RandomFactory(0);
    Random generator = rnd.getRandom();

    for(int i = 0; i < 1000; i++) {
      indi1[i] = generator.nextDouble();
      indi2[i] = generator.nextDouble();
    }

    // linear
    double[] lin1 = new double[1000];
    double[] lin2 = new double[1000];

    for(int i = 0; i < 1000; i++) {
      lin1[i] = i;
      lin2[i] = i * 2;
    }

    // linear with small noise
    double[] small_noise_lin1 = new double[1000];
    double[] small_noise_lin2 = new double[1000];

    for(int i = 0; i < 1000; i++) {
      small_noise_lin1[i] = i;
      small_noise_lin2[i] = (i * 2) + generator.nextGaussian() * 100;
    }

    double[] more_noise_lin1 = new double[1000];
    double[] more_noise_lin2 = new double[1000];

    for(int i = 0; i < 1000; i++) {
      more_noise_lin1[i] = i;
      more_noise_lin2[i] = (i * 2) + generator.nextGaussian() * 500;
    }

    DoubleArrayAdapter adapter = DoubleArrayAdapter.STATIC;

    McdeMwpDependenceMeasure mwp = new ELKIBuilder<>(McdeMwpDependenceMeasure.class) //
        .with(M_ID, 1000) //
        .build();

    test_result(indi1, indi2, 0.65, 0.35, 0.67, 0.33, adapter, mwp);
    test_result(lin1, lin2, 1.0, 0.99, 1.0, 0.97, adapter, mwp);
    test_result(small_noise_lin1, small_noise_lin2, 1.0, 0.96, 1.0, 0.85, adapter, mwp);
    test_result(more_noise_lin1, more_noise_lin2, 1.0, 0.85, 1.0, 0.83, adapter, mwp);

    test_rankIndex(adapter, mwp);
  }

  public static void test_result(double[] data1, double[] data2, double strict_upper, double strict_lower, double lax_upper, double lax_lower, NumberArrayAdapter adapter, McdeMwpDependenceMeasure mwp) {
    double total_res = 0;
    for(int i = 0; i < 100; i++) {
      double res = mwp.dependence(adapter, data1, adapter, data2);
      total_res += res;
      assertTrue("MWP result out of acceptable range. Result: " + res + ". Lax upper bound: " + lax_upper, (res <= lax_upper));
      assertTrue("MWP result out of acceptable range. Result: " + res + ". Lax lower bound: " + lax_lower, (res >= lax_lower));
    }
    total_res /= 100;
    assertTrue("MWP result out of acceptable range. Result: " + total_res + ". Strict upper bound: " + strict_upper, (total_res <= strict_upper));
    assertTrue("MWP result out of acceptable range. Result: " + total_res + ". Strict lower bound: " + strict_lower, (total_res >= strict_lower));
  }

  public static void test_rankIndex(NumberArrayAdapter adapter, McdeMwpDependenceMeasure mwp) {
    double[] input_no_duplicates = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    MwpIndex[] result_no_duplicates = mwp.corrected_ranks(adapter, input_no_duplicates, 10);

    for(int i = 0; i < 10; i++) {
      assertTrue("Error in corrected_rank index construction for MCDE MWP", input_no_duplicates[i] == result_no_duplicates[i].adjusted);
    }

    double[] input_duplicates_middle = { 0, 1, 1, 2, 3, 4, 5, 5, 5, 6, 7, 8, 9 };
    MwpIndex[] output_duplicates_middle = mwp.corrected_ranks(adapter, input_duplicates_middle, 13);
    assertEquals("Error in corrected_rank index construction for MCDE MWP", output_duplicates_middle[1].adjusted, 1.5, 0);
    assertEquals("Error in corrected_rank index construction for MCDE MWP", output_duplicates_middle[2].adjusted, 1.5, 0);
    assertEquals("Error in corrected_rank index construction for MCDE MWP", output_duplicates_middle[6].adjusted, 7.0, 0);
    assertEquals("Error in corrected_rank index construction for MCDE MWP", output_duplicates_middle[7].adjusted, 7.0, 0);
    assertEquals("Error in corrected_rank index construction for MCDE MWP", output_duplicates_middle[8].adjusted, 7.0, 0);

    double[] input_duplicates_start_end = { 0, 0, 0, 2, 3, 4, 5, 5, 5, 6, 9, 9, 9 };
    MwpIndex[] output_duplicates_start_end = mwp.corrected_ranks(adapter, input_duplicates_start_end, 13);
    assertEquals("Error in corrected_rank index construction for MCDE MWP", output_duplicates_start_end[0].adjusted, 1.0, 0);
    assertEquals("Error in corrected_rank index construction for MCDE MWP", output_duplicates_start_end[1].adjusted, 1.0, 0);
    assertEquals("Error in corrected_rank index construction for MCDE MWP", output_duplicates_start_end[2].adjusted, 1.0, 0);
    assertEquals("Error in corrected_rank index construction for MCDE MWP", output_duplicates_start_end[10].adjusted, 11.0, 0);
    assertEquals("Error in corrected_rank index construction for MCDE MWP", output_duplicates_start_end[11].adjusted, 11.0, 0);
    assertEquals("Error in corrected_rank index construction for MCDE MWP", output_duplicates_start_end[12].adjusted, 11.0, 0);
  }
}
