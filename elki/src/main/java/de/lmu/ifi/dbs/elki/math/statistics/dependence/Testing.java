package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

import java.util.stream.IntStream;

public class Testing {

    public static void main(String[] args) {
        double[] data1 = {9.0, 9.0, 9.0, 3.0, 7.0, 1.0, 1.0, 1.0, 10.0, 10.0};
        double[] data2 = new double[10];

        for(int i = 0; i < 10; i++){
            double x = data1[i]*2;
            data2[i] = x;
        }

        NumberArrayAdapter adapter = DoubleArrayAdapter.STATIC;
        RandomFactory rnd = new RandomFactory(5);
        // McdeMwpDependenceMeasure mwp = new McdeMwpDependenceMeasure(50, 0.5, 0.5, rnd);
        // double res = mwp.dependence(adapter, data1, adapter, data2);
        System.out.println("lol");
    }
}
