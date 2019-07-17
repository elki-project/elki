package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import java.util.Arrays;
import java.util.Random;
import static de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution.erf;

/**
 * Implementation of bivariate Monte Carlo Density Estimation using MWP statistical test as described in paper with the
 * only exception that an additional parameter beta is introduced (see comment below).
 *
 * This class extends MCDEDependenceMeasure and implements the Mann-Whitney-U statistical test (as well as p-value computation from the test statistic)
 * and an appropriate index structure.
 *
 * @author Alan Mazankiewicz
 * @author Edouard Fouché
 */

@Reference(authors = "Edouard Fouché, Klemens Böhm", //
        title = "Monte Carlo Density Estimation", //
        booktitle = "Proc. 2013 ACM Int. Conf. on Management of Data (SIGMOD 2013)", // TODO: Fill out
        url = "https://doi.org/10.1145/2463676.2463696", //
        bibkey = "DBLP:conf/sigmod/AchtertKSZ13")

public class McdeMwpDependenceMeasure extends MCDEDependenceMeasure {

    /**
     * Constructor
     */

    public McdeMwpDependenceMeasure(int m, double alpha, double beta, RandomFactory rnd){
        super(m, alpha, beta, rnd);
    }

    /**
     * Computes Corrected Rank Index as described in Algorithm 1 of source paper, adjusted for bivariate ELKI interface.
     * Notation as ELKI convention if applicable, else as in paper.
     *
     * @param adapter ELKI NumberArrayAdapter Subclass
     * @param data One dimensional array containing one dimension of the data
     * @param idx Return value of sortedIndex()
     * @return Array of doubles, 3 subsequent values being assigned to one data instance.
     * Containing sorted (ascending) row numbers, adjusted ranks and tying value corrections
     * as required by MWP test. Example:
     * double[] corrected_ranks = corrected_ranks(...);
     * double l = corrected_rank[0]; double adjusted_rank = corrected_rank[1]; double correction = corrected_rank[2];
     * // correspond to one instance of the original data
     */

    protected <A> double[] corrected_ranks(final NumberArrayAdapter<?, A> adapter, final A data, int[] idx){
        final int len = adapter.size(data);
        double[] I = new double[len * 3];

        int j = 0; int correction = 0;
        while(j < len){
            int k = j; int t = 1; double adjust = 0.0;

            while((k < len - 1) && (adapter.getDouble(data, idx[k]) == adapter.getDouble(data, idx[k+1]))){
                adjust += k;
                k++; t++;
            }

            if(k > j){
                double adjusted = (adjust + k) / t;
                correction += (t*t*t) - t;

                for(int m = j; m <= k; m++){
                    int p = m*3;
                    I[p] = (double) idx[m];
                    I[p+1] = adjusted;
                    I[p+2] = correction;
                }
            }
            else {
                int p = j*3;
                I[p] = (double)idx[j];
                I[p+1] = j;
                I[p+2] = correction;
            }
            j += t;
        }

        return I;
    }

    /**
     * Data Slicing
     *
     * @param len No of data instances
     * @param nonRefIndex Index (see correctedRank()) for the dimension that is not the reference dimension
     * @return Array of booleans that states which instances are part of the slice
     */


    protected boolean[] randomSlice(int len, double[] nonRefIndex){
        final Random random = rnd.getSingleThreadedRandom();
        boolean slice[] = new boolean[len];
        Arrays.fill(slice, Boolean.TRUE);

        final int slizeSize = (int) Math.ceil(Math.pow(this.alpha, 1.0) * len);
        final int start = random.nextInt(len - slizeSize);
        final int end = start + slizeSize;

        for(int j = 0; j < start; j++){
            slice[(int) nonRefIndex[j*3]] = false;
        }

        for(int j = end; j < len; j++){
            slice[(int) nonRefIndex[j*3]] = false;
        }

        return slice;
    }

    /**
     * Efficient implementation of MWP statistical test using appropriate index structure as described in Algorithm 3
     * of source paper.
     * @param len No of data instances
     * @param slice Return value of randomSlice() created with the index that is not for the reference dimension
     * @param corrected_ranks Index, return value of corrected_ranks() of the reference dimension
     * @return p-value from two sided Mann-Whitney-U test
     */

    protected double statistical_test(int len, boolean[] slice, double[] corrected_ranks){
        final Random random = rnd.getSingleThreadedRandom(); // TODO: No "safeCut", make safecut?
        final int start = random.nextInt((int) (len * (1 - this.beta)));
        final int end = start + (int) Math.ceil(len * this.beta);

        double R = 0.0; long n1 = 0;
        for(int j = start; j < end; j++){

            if(slice[(int) corrected_ranks[j*3]]){
                R += corrected_ranks[j*3 + 1];
                n1++;
            }
        }

        R -= start * n1; // TODO: Potential for Error

        final int cutLength = end - start;
        if((n1 == 0) || (n1 == cutLength)) return 1;

        final double U = R - ((double)(n1 * (n1 - 1))) / 2; // TODO: Potential for Error
        final long n2 = cutLength - n1;

        final long two_times_sqrt_max_long = 6074000999L;
        if(n1 + n2 > two_times_sqrt_max_long)
            throw new AbortException("Long type overflowed. Dataset has to many dataobjects. Please subsample and try again with smaller dataset.");

        final double b_end = corrected_ranks[(end-1) *3 +2];
        final double b_start = start == 0 ? 0 : corrected_ranks[(start-1) *3 +2];
        final double correction = (b_end - b_start) / (cutLength * (cutLength -1));
        final double std = Math.sqrt(( ((double) (n1 * n2)) / 12) * (cutLength + 1 - correction));

        if(std == 0) return 0;
        else{
            final double mean = ((double) (n1 * n2)) / 2;
            final double Z = Math.abs((U - mean) / std);
            return erf(Z / Math.sqrt(2)); // TODO: Potential for Error
        }
    }

    // TODO: Parametizer class
}
