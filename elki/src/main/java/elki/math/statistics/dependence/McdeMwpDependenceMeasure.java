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

import elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import elki.utilities.documentation.Reference;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.random.RandomFactory;
import java.lang.reflect.Array;
import java.util.Random;
import static elki.math.statistics.distribution.NormalDistribution.erf;
import elki.utils.containers.MwpIndex;

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
        booktitle = "Proc. 2019 ACM Int. Conf. on Scientific and Statistical Database Management (SSDBM 2019)",
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
     * 
     * Correspond to one instance of the original data
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
     * @param corrected_ranks Index of the reference dimension, return value of corrected_ranks() computed for reference dimension
     * @return p-value from two sided Mann-Whitney-U test
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

    /**
     * Parameterization class.
     *
     * @author Alan Mazankiewicz
     * @author Edouard Fouché
     */
    public static class Par implements Parameterizer {

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
        public void configure(Parameterization config) {
            new IntParameter(M_ID, 50) //
                    .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT)
                    .grab(config, x -> m = x);

            new DoubleParameter(ALPHA_ID, 0.5) //
                    .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE)
                    .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE)
                    .grab(config, x -> alpha = x);

            new DoubleParameter(BETA_ID, 0.5) //
                    .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE)
                    .addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE)
                    .grab(config, x -> beta = x);

            new RandomParameter(SEED_ID)
            .grab(config, x -> rnd = x);
        }

        @Override
        public McdeMwpDependenceMeasure make() { return new McdeMwpDependenceMeasure(m, alpha, beta, rnd); }
    }
}
