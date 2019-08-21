package de.lmu.ifi.dbs.elki.math.statistics.dependence;


import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utils.containers.RankStruct;
import java.util.Arrays;
import java.util.Random;


// TODO: Write tests

/**
 * Implementation of bivariate Monte Carlo Density Estimation as described in paper with the only exception that an
 * additional parameter beta is introduced (see comment below).
 *
 * This is an abstract class. In order to use MCDE extend it and implement an appropriate statistical test that
 * returns a p-value and index structure for efficient computation of the statistical test.
 *
 * For MCDE with MWP as the statistical test (as described in the paper) see McdeMwpDependenceMeasure.
 *
 * @author Alan Mazankiewicz
 * @author Edouard Fouché
 */

@Reference(authors = "Edouard Fouché, Klemens Böhm", //
        title = "Monte Carlo Density Estimation", //
        booktitle = "Proc. 2013 ACM Int. Conf. on Management of Data (SIGMOD 2013)", // TODO: Fill out
        url = "https://doi.org/10.1145/2463676.2463696", //
        bibkey = "DBLP:conf/sigmod/AchtertKSZ13")

public abstract class MCDEDependenceMeasure<R extends RankStruct> extends AbstractDependenceMeasure {

    /**
     * Monte-Carlo iterations.
     */

    protected int m = 50;

    /**
     * Expected share of instances in slice (independent dimensions).
     */
    protected double alpha = 0.5;

    /**
     * Expected share of instances in marginal restriction (reference dimension). Note that in the original paper
     * alpha = beta and as such there is no explicit distinction between the parameters.
     */

    protected double beta = 0.5;

    /**
     * Random generator.
     */

    protected RandomFactory rnd;

    public MCDEDependenceMeasure(int m, double alpha, double beta, RandomFactory rnd){
        if((beta > 1.0) || beta < 0.0) throw new AbortException("beta has to be in (0,1) range (inclusive)");
        if((alpha > 1.0) || alpha < 0.0) throw new AbortException("beta has to be in (0,1) range (inclusive)");
        if(m < 1) throw new AbortException("m has to be > 0");

        this.m = m;
        this.alpha = alpha;
        this.beta = beta;
        this.rnd = rnd;
    }

    /**
     * Overloaded wrapper for corrected_ranks()
     */

    protected <A> R[] corrected_ranks(final NumberArrayAdapter<?, A> adapter, final A data, int len) { // TODO: could also be hiding ranks() but problem with static...
        return corrected_ranks(adapter, data, sortedIndex(adapter, data, len));
    }

    /**
     * Subclass should implement computation of corrected rank index used for precomputation.
     * Adjusted for bivariate ELKI interface.
     *
     * @param adapter ELKI NumberArrayAdapter Subclass
     * @param data One dimensional array containing one dimension of the data
     * @param idx Return value of sortedIndex()
     * @return Array of RankStruct, acting as rank index
     */

    protected abstract <A> R[] corrected_ranks(final NumberArrayAdapter<?, A> adapter, final A data, int[] idx);

    /**
     * Data Slicing
     *
     * @param len No of data instances
     * @param nonRefIndex Index (see correctedRank()) computed for the dimension that is not the reference dimension
     * @return Array of booleans that states which instances are part of the slice
     */


    protected boolean[] randomSlice(int len, R[] nonRefIndex){
        final Random random = rnd.getSingleThreadedRandom();
        boolean slice[] = new boolean[len];
        Arrays.fill(slice, Boolean.TRUE);

        final int slizeSize = (int) Math.ceil(Math.pow(this.alpha, 1.0) * len);
        final int start = random.nextInt(len - slizeSize);
        final int end = start + slizeSize;

        for(int j = 0; j < start; j++){
            slice[nonRefIndex[j].index] = false;
        }

        for(int j = end; j < len; j++){
            slice[(int) nonRefIndex[j].index] = false;
        }

        return slice;
    }

    /**
     * Subclass should implement statistical test returning a p-value
     *
     * @param len No of data instances
     * @param slice Return value of randomSlice(), boolean array indicating which instance is in the slice (by index)
     * @param corrected_ranks Index of the reference dimension, return value of corrected_ranks() computed for reference dimension
     * @return p-value of given statistical test
     */

    protected abstract double statistical_test(int len, boolean[] slice, R[] corrected_ranks);

    /**
     * Implements dependence from DependenceMeasure superclass. Corresponds to Algorithm 4 in source paper.
     * Note: Data must not contain NaN values.
     *
     * @param adapter1 First data adapter
     * @param data1 First data set
     * @param adapter2 Second data adapter
     * @param data2 Second data set
     * @param <A> Numeric data type, such as double
     * @param <B> Numeric data type, such as double
     * @return MCDE result
     */

    @Override
    public <A, B> double dependence(final NumberArrayAdapter<?, A> adapter1, final A data1, final NumberArrayAdapter<?, B> adapter2, final B data2){
        final Random random = rnd.getSingleThreadedRandom();
        final int len = adapter1.size(data1);

        if(len != adapter2.size(data2))
            throw new AbortException("Size of both arrays must match!");

        final R[] index_0 = corrected_ranks(adapter1, data1, len);
        final R[] index_1 = corrected_ranks(adapter2, data2, len);

        double mwp = 0;
        for(int i = 0; i < this.m; i++){ // TODO: could also be done through modulo so that we avoid the random generation
            int r = random.nextInt(2);
            R[] ref_index;
            R[] other_index;

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