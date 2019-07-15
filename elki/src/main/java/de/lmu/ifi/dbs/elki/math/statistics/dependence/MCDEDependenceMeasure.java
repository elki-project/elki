package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import de.lmu.ifi.dbs.elki.math.statistics.tests.GoodnessOfFitTest;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;


public class MCDEDependenceMeasure extends AbstractDependenceMeasure {
    private int m = 50;
    private double alpha = 0.5;
    private double beta = 0.5;
    private GoodnessOfFitTest statTest;
    private RandomFactory rnd;

    public MCDEDependenceMeasure(GoodnessOfFitTest statTest, int m, double alpha, double beta, RandomFactory rnd){
        this.m = m;
        this.alpha = alpha;
        this.beta = beta;
        this.statTest = statTest;
        this.rnd = rnd;
    }

    protected static <A> double[][] correctedRank(final NumberArrayAdapter<?, A> adapter, final A data, int len) {
        return correctedRank(adapter, data, sortedIndex(adapter, data, len));
    }

    protected static <A> double[][] correctedRank(final NumberArrayAdapter<?, A> adapter, final A data, int[] idx){
        double[][] array = {{1.0, 2.0}, {4.0, 5.0}};
        return array;
    }


    @Override
    public <A, B> double dependence(final NumberArrayAdapter<?, A> adapter1, final A data1, final NumberArrayAdapter<?, B> adapter2, final B data2){
        return 1.0;
    }
}
