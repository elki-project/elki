package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

import java.util.stream.IntStream;

public class Testing {

    public static void main(String[] args) {
        double[] data1 = {9.0, 9.0, 9.0, 3.0, 7.0, 1.0, 1.0, 1.0, 10.0, 10.0};
        double[] data2 = new double[100];

        for(int i = 0; i < 100; i++){
            double x = Math.random();
            data2[i] = x;
        }

        NumberArrayAdapter adapter = DoubleArrayAdapter.STATIC;

        KolmogorovSmirnovTest kogo = new KolmogorovSmirnovTest();
        RandomFactory rnd = new RandomFactory(5);
        HiCSDependenceMeasure HiCS = new HiCSDependenceMeasure(kogo, 50, 0.1, rnd);
        MCDEDependenceMeasure MCDE = new MCDEDependenceMeasure(kogo, 50, 0.5, 0.5, rnd);
        // System.out.print(HiCS.dependence(data1, data2));

        double[] rank = HiCS.ranks(adapter, data1, 10);
        int[] sortInd = HiCS.sortedIndex(adapter, data1, 10);


        MCDEDependenceMeasure.IndexTriple[] testTriple = MCDEDependenceMeasure.correctedRank(adapter, data2, 100);
        boolean[] slice = MCDE.randomSlice(100, testTriple);

        int counter = 0;
        for(int i = 0; i < 100; i++){

            if(slice[i]) ++counter;
        }

        System.out.println(counter);
    }
}
