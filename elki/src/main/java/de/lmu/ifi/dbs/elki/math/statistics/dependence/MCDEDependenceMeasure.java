package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import de.lmu.ifi.dbs.elki.math.statistics.tests.GoodnessOfFitTest;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

import java.util.Arrays;
import java.util.Random;



public class MCDEDependenceMeasure extends AbstractDependenceMeasure {

    /**
     * Monte-Carlo iterations.
     */

    private int m = 50;

    /**
     * Alpha threshold.
     */

    /**
     * Expected share of instances in slice (independent dimensions).
     */
    private double alpha = 0.5;

    /**
     * Expected share of instances in marginal restriction (reference dimension). Note that in the original paper
     * alpha = beta and there is no explicit distinction between the parameters. TODO: Implement correctly that beta is used
     */

    private double beta = 0.5;

    /**
     * Statistical test to use. IMPORTANT: The implementation of the test has to implement a method compute_pval() that
     * returns the p-value not the given test statistic. At time of writing this only applies to MannWhitneyUTest.
     */

    private GoodnessOfFitTest statTest;

    /**
     * Random generator.
     */

    private RandomFactory rnd;

    /**
     * Data container to mimic a triple to be used for index construction.
     */

    static protected class IndexTriple {
        final int rank;
        final double adjusted_index;
        final double correction;

        protected IndexTriple(int rank, double adjusted_index, double correction){
            this.rank = rank;
            this.adjusted_index = adjusted_index;
            this.correction = correction;
        }
    }

    /**
     * Constructor
     */

    public MCDEDependenceMeasure(GoodnessOfFitTest statTest, int m, double alpha, double beta, RandomFactory rnd){
        this.m = m;
        this.alpha = alpha;
        this.beta = beta;
        this.statTest = statTest;
        this.rnd = rnd;
    }

    /**
     * Overloaded wrapper for correctedRank (see below)
     */

    protected static <A> IndexTriple[] correctedRank(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
        return correctedRank(adapter, data, sortedIndex(adapter, data, len));
    }

    /**
     * Computes Corrected Rank Index as described in Algorithm 1 of source paper, adjusted for bivariate ELKI interface.
     * Notation as ELKI convention if applicable, else as in paper.
     *
     * @param adapter ELKI NumberArrayAdapter Subclass
     * @param data One dimensional array containing one dimension of the data
     * @param idx Return value of sortedIndex()
     * @return Array of triples containing sorted (ascending) row numbers, adjusted ranks and tying value corrections
     * as required by MWP test.
     */

    protected static <A> IndexTriple[] correctedRank(final NumberArrayAdapter<?, A> adapter, final A data, int[] idx){
        final int len = adapter.size(data);
        IndexTriple[] I = new IndexTriple[len];

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
                    I[m] = new IndexTriple(idx[m], adjusted, correction);
                }
            }
            else {
                I[j] = new IndexTriple(idx[j], j, correction);
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

    protected boolean[] randomSlice(int len, IndexTriple[] nonRefIndex){
        final Random random = rnd.getSingleThreadedRandom();
        boolean slice[] = new boolean[len];
        Arrays.fill(slice, Boolean.TRUE);

        final int slizeSize = (int) Math.ceil(Math.pow(this.alpha, 1.0) * len);
        final int start = random.nextInt(len - slizeSize);
        final int end = start + slizeSize;

        for(int j = 0; j < start; j++){
            slice[nonRefIndex[j].rank] = false;
        }

        for(int j = end; j < len; j++){
            slice[nonRefIndex[j].rank] = false;
        }

        return slice;
    }

    @Override
    public <A, B> double dependence(final NumberArrayAdapter<?, A> adapter1, final A data1, final NumberArrayAdapter<?, B> adapter2, final B data2){
        return 1.0;
    }
}
