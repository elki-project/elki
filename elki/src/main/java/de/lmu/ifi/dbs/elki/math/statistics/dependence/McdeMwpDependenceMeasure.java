package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import java.lang.reflect.Array;
import java.util.Random;
import static de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution.erf;
import de.lmu.ifi.dbs.elki.utils.containers.MwpIndex;

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
        booktitle = "Proceedings of the 31st International Conference on Scientific and Statistical Database Management (SSDBM 2019)",
        url = "http://doi.acm.org/10.1145/3335783.3335795", //
        bibkey = "DBLP:conf/ssdbm/FouchéB19")

public class McdeMwpDependenceMeasure extends MCDEDependenceMeasure<MwpIndex> {

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
     * of source paper.
     * @param len No of data instances
     * @param slice Return value of randomSlice() created with the index that is not for the reference dimension
     * @param corrected_ranks Index of the reference dimension, return value of corrected_ranks() computed for reference dimension
     * @return p-value from two sided Mann-Whitney-U test
     */

    protected double statistical_test(int len, boolean[] slice, MwpIndex[] corrected_ranks){
        final Random random = rnd.getSingleThreadedRandom();
        final int start = random.nextInt((int) (len * (1 - this.beta)));
        final int end = start + (int) Math.ceil(len * this.beta);

        double R = 0.0; long n1 = 0;
        for(int j = start; j < end; j++){

            if(slice[corrected_ranks[j].index]){
                R += corrected_ranks[j].adjusted;
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

        final double b_end = corrected_ranks[(end-1)].correction;
        final double b_start = start == 0 ? 0 : corrected_ranks[(start-1)].correction;
        final double correction = (b_end - b_start) / (cutLength * (cutLength -1));
        final double std = Math.sqrt(( ((double) (n1 * n2)) / 12) * (cutLength + 1 - correction));

        if(std == 0) return 0;
        else{
            final double mean = ((double) (n1 * n2)) / 2;
            final double Z = Math.abs((U - mean) / std);
            return erf(Z / Math.sqrt(2)); // TODO: Potential for Error
        }
    }

    /**
     * Parameterization class.
     *
     * @author Alan Mazankiewicz
     * @author Edouard Fouché
     */

    public static class Parameterizer extends AbstractParameterizer {

        /**
         * Parameter that specifies the number of iterations in the Monte-Carlo
         * process of identifying high contrast subspaces.
         */

        public static final OptionID M_ID = new OptionID("McdeMwp.m", "No. of Monte-Carlo iterations.");

        /**
         * Parameter that specifies the size of the slice
         */

        public static final OptionID ALPHA_ID = new OptionID("McdeMwp.alpha", "Expected share of instances in slice (independent dimensions).");

        /**
         * Parameter that specifies the size of the marginal restriction. Note that in the original paper
         * alpha = beta and as such there is no explicit distinction between the parameters.
         */

        public static final OptionID BETA_ID = new OptionID("McdeMwp.beta", "Expected share of instances in marginal restriction (dependent dimensions).");

        /**
         * Parameter that specifies the random seed.
         */

        public static final OptionID SEED_ID = new OptionID("McdeMwp.seed", "The random seed.");

        /**
         * Holds the value of {@link #M_ID}.
         */

        protected int m = 50;

        /**
         * Holds the value of {@link #ALPHA_ID}.
         */

        protected double alpha = 0.5;

        /**
         * Holds the value of {@link #BETA_ID}.
         */

        protected double beta = 0.5;

        /**
         * Random generator.
         */

        protected RandomFactory rnd;

        @Override
        protected void makeOptions(Parameterization config) {
            super.makeOptions(config);

            final IntParameter mP = new IntParameter(M_ID, 50) //
                    .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT);
            if(config.grab(mP)) {
                m = mP.intValue();
            }

            final DoubleParameter alphaP = new DoubleParameter(ALPHA_ID, 0.5) //
                    .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE); // actually > 0 and < 1 but no such constrain available
            if(config.grab(alphaP)) {
                alpha = alphaP.doubleValue();
            }

            final DoubleParameter betaP = new DoubleParameter(BETA_ID, 0.5) //
                    .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE); // actually > 0 and < 1 but no such constrain available
            if(config.grab(betaP)) {
                beta = betaP.doubleValue();
            }

            final RandomParameter rndP = new RandomParameter(SEED_ID);
            if(config.grab(rndP)) {
                rnd = rndP.getValue();
            }
        }

        @Override
        protected McdeMwpDependenceMeasure makeInstance() { return new McdeMwpDependenceMeasure(m, alpha, beta, rnd); }
    }
}
