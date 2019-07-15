package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.math.statistics.tests.GoodnessOfFitTest;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

import java.util.stream.DoubleStream;
import java.util.stream.IntStream;


public class MCDEDependenceMeasure extends AbstractDependenceMeasure {
    private int m = 50;
    private double alpha = 0.5;
    private double beta = 0.5;
    private GoodnessOfFitTest statTest;
    private RandomFactory rnd;

    static protected class IndexTriple {
        int rank;
        double adjusted_index;
        double correction;

        protected IndexTriple(int rank, double adjusted_index, double correction){
            this.rank = rank;
            this.adjusted_index = adjusted_index;
            this.correction = correction;
        }
    }

    public MCDEDependenceMeasure(GoodnessOfFitTest statTest, int m, double alpha, double beta, RandomFactory rnd){
        this.m = m;
        this.alpha = alpha;
        this.beta = beta;
        this.statTest = statTest;
        this.rnd = rnd;
    }

    protected static <A> IndexTriple[] correctedRank(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
        return correctedRank(adapter, data, sortedIndex(adapter, data, len));
    }

    /**
     * Computes Corrected Rank Index as described in Algorithm 1 of source paper, adjusted for bivariate ELKI interface.
     * Notation as ELKI convention if applicable, else as in paper.
     *
     * @param adapter
     * @param data
     * @param idx
     * @param <A>
     * @return
     */

    protected static <A> IndexTriple[] correctedRank(final NumberArrayAdapter<?, A> adapter, final A data, int[] idx){
        final int len = adapter.size(data);
        // final double[] r = IntStream.range(0, len).mapToDouble(x -> x).toArray();
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


    @Override
    public <A, B> double dependence(final NumberArrayAdapter<?, A> adapter1, final A data1, final NumberArrayAdapter<?, B> adapter2, final B data2){
        return 1.0;
    }
}
