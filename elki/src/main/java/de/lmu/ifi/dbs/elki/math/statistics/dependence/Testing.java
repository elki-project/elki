package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.random.FastNonThreadsafeRandom;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utils.containers.MwpIndex;

import java.util.Random;


// TODO: In final test using import static org.junit.Assert.{assertEquals, assertTrue};
// Note: activate -ea im JVM run

/**
 * @author Alan Mazankiewicz
 * @author Edouard Fouché
 */

public class Testing {

    public static void main(String[] args) {

        // independent
        double[] indi1 = new double[1000];
        double[] indi2 = new double[1000];

        for (int i = 0; i < 1000; i++) {
            indi1[i] = Math.random();
            indi2[i] = Math.random();
        }

        // linear
        double[] lin1 = new double[1000];
        double[] lin2 = new double[1000];

        for (int i = 0; i < 1000; i++) {
            lin1[i] = i;
            lin2[i] = i * 2;
        }

        // linear with small noise
        Random r = new FastNonThreadsafeRandom((long) Math.random());
        double[] small_noise_lin1 = new double[1000];
        double[] small_noise_lin2 = new double[1000];

        for (int i = 0; i < 1000; i++) {
            small_noise_lin1[i] = i;
            small_noise_lin2[i] = (i * 2) + r.nextGaussian() * 100;
        }

        double[] more_noise_lin1 = new double[1000];
        double[] more_noise_lin2 = new double[1000];

        for (int i = 0; i < 1000; i++) {
            more_noise_lin1[i] = i;
            more_noise_lin2[i] = (i * 2) + r.nextGaussian() * 100;
        }

        NumberArrayAdapter adapter = DoubleArrayAdapter.STATIC;
        RandomFactory rnd = new RandomFactory((long) Math.random());
        McdeMwpDependenceMeasure mwp = new McdeMwpDependenceMeasure(1000, 0.5, 0.5, rnd);

        test_result(indi1, indi2, 0.65, 0.35, 0.67, 0.33, adapter, mwp);
        test_result(lin1, lin2, 1.0, 0.99, 1.0, 0.97, adapter, mwp);
        test_result(small_noise_lin1, small_noise_lin2, 1.0, 0.96, 1.0, 0.85, adapter, mwp);
        test_result(more_noise_lin1, more_noise_lin2, 1.0, 0.85, 1.0, 0.83, adapter, mwp);

        test_rankIndex(adapter, mwp);
    }

    public static void test_result(double[] data1, double[] data2, double strict_upper, double strict_lower,
                            double lax_upper, double lax_lower, NumberArrayAdapter adapter, McdeMwpDependenceMeasure mwp) {
        double total_res = 0;
        for (int i = 0; i < 100; i++) {
            double res = mwp.dependence(adapter, data1, adapter, data2);
            total_res += res;
            assert (res <= lax_upper) : "MWP result out of acceptable range. Result: " + res + ". Lax upper bound: " + lax_upper;
            assert (res >= lax_lower) : "MWP result out of acceptable range. Result: " + res + ". Lax lower bound: " + lax_lower;
        }
        total_res /= 100;
        assert (total_res <= strict_upper) : "MWP result out of acceptable range. Result: " + total_res + ". Strict upper bound: " + strict_upper;
        assert (total_res >= strict_lower) : "MWP result out of acceptable range. Result: " + total_res + ". Strict lower bound: " + strict_lower;
    }

    public static void test_rankIndex(NumberArrayAdapter adapter, McdeMwpDependenceMeasure mwp)
    {
        double[] input_no_duplicates = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        MwpIndex[] result_no_duplicates = mwp.corrected_ranks(adapter, input_no_duplicates, 10);

        for(int i = 0; i < 10; i++){
            assert input_no_duplicates[i] == result_no_duplicates[i].adjusted : "Error in corrected_rank index construction for MCDE MWP";
        }

        double[] input_duplicates_middle = {0, 1, 1, 2, 3, 4, 5, 5, 5, 6, 7, 8, 9};
        MwpIndex[] output_duplicates_middle = mwp.corrected_ranks(adapter, input_duplicates_middle, 13);
        assert output_duplicates_middle[1].adjusted == 1.5 : "Error in corrected_rank index construction for MCDE MWP";
        assert output_duplicates_middle[2].adjusted == 1.5 : "Error in corrected_rank index construction for MCDE MWP";
        assert output_duplicates_middle[6].adjusted == 7.0 : "Error in corrected_rank index construction for MCDE MWP";
        assert output_duplicates_middle[7].adjusted == 7.0 : "Error in corrected_rank index construction for MCDE MWP";
        assert output_duplicates_middle[8].adjusted == 7.0 : "Error in corrected_rank index construction for MCDE MWP";

        double[] input_duplicates_start_end = {0, 0, 0, 2, 3, 4, 5, 5, 5, 6, 9, 9, 9};
        MwpIndex[] output_duplicates_start_end = mwp.corrected_ranks(adapter, input_duplicates_start_end, 13);
        assert output_duplicates_start_end[0].adjusted == 1.0 : "Error in corrected_rank index construction for MCDE MWP";
        assert output_duplicates_start_end[1].adjusted == 1.0 : "Error in corrected_rank index construction for MCDE MWP";
        assert output_duplicates_start_end[2].adjusted == 1.0 : "Error in corrected_rank index construction for MCDE MWP";
        assert output_duplicates_start_end[10].adjusted == 11.0 : "Error in corrected_rank index construction for MCDE MWP";
        assert output_duplicates_start_end[11].adjusted == 11.0 : "Error in corrected_rank index construction for MCDE MWP";
        assert output_duplicates_start_end[12].adjusted == 11.0 : "Error in corrected_rank index construction for MCDE MWP";
    }
}
