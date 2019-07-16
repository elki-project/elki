package de.lmu.ifi.dbs.elki.math.statistics.dependence;


import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import java.util.Random;



public abstract class MCDEDependenceMeasure extends AbstractDependenceMeasure {

    /**
     * Monte-Carlo iterations.
     */

    protected int m = 50;

    /**
     * Alpha threshold.
     */

    /**
     * Expected share of instances in slice (independent dimensions).
     */
    protected double alpha = 0.5;

    /**
     * Expected share of instances in marginal restriction (reference dimension). Note that in the original paper
     * alpha = beta and there is no explicit distinction between the parameters. TODO: Implement correctly that beta is used
     */

    protected double beta = 0.5;

    /**
     * Random generator.
     */

    protected RandomFactory rnd;

    /**
     * Overloaded wrapper for corrected_ranks()
     */

    protected <A> double[] corrected_ranks(final NumberArrayAdapter<?, A> adapter, final A data, int len) { // TODO: could also be hiding ranks but problem with static...
        return corrected_ranks(adapter, data, sortedIndex(adapter, data, len));
    }

    /**
     * Subclass should implement computation of corrected rank index used for precomputation.
     * Adjusted for bivariate ELKI interface.
     *
     * @param adapter ELKI NumberArrayAdapter Subclass
     * @param data One dimensional array containing one dimension of the data
     * @param idx Return value of sortedIndex()
     * @return Array of doubles, acting as rank index
     */

    protected abstract <A> double[] corrected_ranks(final NumberArrayAdapter<?, A> adapter, final A data, int[] idx);

    /**
     * Data Slicing
     *
     * @param len No of data instances
     * @param nonRefIndex Index (see correctedRank()) for the dimension that is not the reference dimension
     * @return Array of booleans that states which instances are part of the slice
     */

    protected abstract boolean[] randomSlice(int len, double[] nonRefIndex);

    protected abstract double statistical_test(int len, boolean[] slice, double[] corrected_ranks);

    // TODO: Look at HiCS and what can be done here from there, e.g Nan protection
    @Override
    public <A, B> double dependence(final NumberArrayAdapter<?, A> adapter1, final A data1, final NumberArrayAdapter<?, B> adapter2, final B data2){
        final Random random = rnd.getSingleThreadedRandom();
        final int len = adapter1.size(data1);

        if(len != adapter2.size(data2))
            throw new AbortException("Size of both arrays must match!");

        final double[] index_0 = corrected_ranks(adapter1, data1, len);
        final double[] index_1 = corrected_ranks(adapter2, data2, len);

        double mwp = 0;
        for(int i = 0; i < this.m; i++){ // TODO: could also be done through modulo so that we avoid the random generation
            int r = random.nextInt(2);
            double[] ref_index;
            double[] other_index;

            if(r == 1) {
                ref_index = index_1;
                other_index = index_0;
            }
            else {
                ref_index = index_0;
                other_index = index_1;
            }

            mwp += statistical_test(len, randomSlice(len, other_index), ref_index);
        }
        return mwp / m;
    }
}
