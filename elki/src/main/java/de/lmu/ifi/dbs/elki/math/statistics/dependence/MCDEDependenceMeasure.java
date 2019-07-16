package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import de.lmu.ifi.dbs.elki.math.statistics.tests.GoodnessOfFitTest;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

import java.util.Arrays;
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
     * Data Slicing
     *
     * @param len No of data instances
     * @param nonRefIndex Index (see correctedRank()) for the dimension that is not the reference dimension
     * @return Array of booleans that states which instances are part of the slice
     */

    protected abstract boolean[] randomSlice(int len, double[] nonRefIndex);

    protected abstract double statistical_test(int len, boolean[] slice, double[] corrected_ranks);

    @Override
    public <A, B> double dependence(final NumberArrayAdapter<?, A> adapter1, final A data1, final NumberArrayAdapter<?, B> adapter2, final B data2){
        return 1.0;
    }
}
