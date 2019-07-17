package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

import java.util.stream.IntStream;

public class Testing {

    public static void main(String[] args) {
        double[] data1 = new double [100];
        double[] data2 = new double[100];

        for(int i = 0; i < 100; i++){
            data1[i] = Math.random();
            data2[i] = Math.random();
        }

        double[] data3 = new double [100];
        double[] data4 = new double[100];

        for(int i = 0; i < 100; i++){
            data3[i] = i;
            data4[i] = i*2;
        }

        NumberArrayAdapter adapter = DoubleArrayAdapter.STATIC;
        RandomFactory rnd = new RandomFactory(5);
        McdeMwpDependenceMeasure mwp = new McdeMwpDependenceMeasure(50, 0.5, 0.5, rnd);
        for(int i = 0; i < 20; i++){
            double res = mwp.dependence(adapter, data3, adapter, data4);
            System.out.println(res);
        }
    }
}
