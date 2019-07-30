package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import java.lang.reflect.Array;
import java.util.Random;
import static de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution.erf;
import de.lmu.ifi.dbs.elki.utils.containers.MwpIndex;

/**
 * Implementation of bivariate Monte Carlo Density Estimation using Mann-Withney U test, known as MWP. See
 * Edouard Fouché & Klemens Böhm<br>
 * Monte Carlo Density Estimation<br>
 * Proc. 2019 ACM Int. Conf. on Scientific and Statistical Database Management (SSDBM 2019)
 *
 * This class extends MCDEDependenceMeasure and implements the Mann-Whitney-U statistical test and an appropriate index structure.
 *
 * @author Alan Mazankiewicz
 * @author Edouard Fouché
 */

@Reference(authors = "Edouard Fouché, Klemens Böhm", //
        title = "Monte Carlo Density Estimation", //
        booktitle = "Proc. 2019 ACM Int. Conf. on Scientific and Statistical Database Management (SSDBM 2019)", // TODO: Check
        url = "https://doi.org/10.1145/3335783.3335795", //
        bibkey = "DBLP:conf/ssdbm/FoucheB19")

public class McdeMwpDependenceMeasure extends MCDEDependenceMeasure<MwpIndex> {

    /**
     * Constructor
     *
     * @param m Monte-Carlo iterations
     * @param alpha Expected share of instances in slice (under independence)
     * @param beta Share of instances in marginal restriction (reference dimension)
     * @param rnd Random source
     */
    public McdeMwpDependenceMeasure(int m, double alpha, double beta, RandomFactory rnd){
        super(m, alpha, beta, rnd);
    }

    /**
     * Computes Corrected Rank Index as described in Algorithm 1 of reference paper.
     * The notation follows ELKI convention if applicable, else we use the notation from the reference paper.
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
    protected <A> MwpIndex[] corrected_ranks(final NumberArrayAdapter<?, A> adapter, final A data, int[] idx){
        final int len = adapter.size(data);
        MwpIndex[] I = (MwpIndex[]) Array.newInstance(MwpIndex.class, len);

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
                    I[m] = new MwpIndex(idx[m], adjusted, correction);
                }
            }
            else
                I[j] = new MwpIndex(idx[j], j, correction);
            j += t;
        }

        return I;
    }

    /**
     * Efficient implementation of MWP statistical test using appropriate index structure as described in Algorithm 3
     * of reference paper.
     *
     * @param len No of data instances
     * @param slice Return value of randomSlice() created with the index that is not for the reference dimension
     * @param corrected_ranks Index, return value of corrected_ranks() of the reference dimension
     * @return p-value from two-sided Mann-Whitney-U test
     */

    protected double statistical_test(int len, boolean[] slice, MwpIndex[] corrected_ranks){
        final Random random = rnd.getSingleThreadedRandom(); // Note: No "safecut".
        final int start = random.nextInt((int) (len * (1 - this.beta)));
        final int end = start + (int) Math.ceil(len * this.beta);

        double R = 0.0; long n1 = 0;
        for(int j = start; j < end; j++){

            if(slice[corrected_ranks[j].index]){
                R += corrected_ranks[j].adjusted;
                n1++;
            }
        }

        // This is to cancel the offset in case the marginal restriction does not start from 0
        // see "acc - (cutStart * count)" is reference implementation of MWP
        R -= start * n1;

        final int cutLength = end - start;
        if((n1 == 0) || (n1 == cutLength)) return 1;

        final double U = R - ((double)(n1 * (n1 - 1))) / 2;
        final long n2 = cutLength - n1;

        final long two_times_sqrt_max_long = 6074000999L;
        if(n1 + n2 > two_times_sqrt_max_long)
            throw new AbortException("Long type overflowed. Too many objects: Please subsample and try again with smaller data set.");

        final double b_end = corrected_ranks[(end-1)].correction;
        final double b_start = start == 0 ? 0 : corrected_ranks[(start-1)].correction;
        final double correction = (b_end - b_start) / (cutLength * (cutLength -1));
        final double std = Math.sqrt(( ((double) (n1 * n2)) / 12) * (cutLength + 1 - correction));

        if(std == 0) return 0;
        else{
            final double mean = ((double) (n1 * n2)) / 2;
            final double Z = Math.abs((U - mean) / std);
            return erf(Z / Math.sqrt(2)); // Note that this is equivalent to do 1-2*(1-cdf(Z,0,1));
            // erf(Z / Math.sqrt(2)) is the cdf of the half-normal distribution
        }
    }

    // TODO: Parametizer class
}
