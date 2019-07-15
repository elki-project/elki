package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

public class Testing {

    public static void main(String[] args) {
        double[] data1 = {9.0, 5.0, 3.0, 7.0, 1.0, 2.0};
        double[] data2 = {18.0, 10.0, 6.0, 14.0, 2.0, 4.0};
        NumberArrayAdapter adapter = DoubleArrayAdapter.STATIC;

        KolmogorovSmirnovTest kogo = new KolmogorovSmirnovTest();
        RandomFactory rnd = new RandomFactory(5);
        HiCSDependenceMeasure HiCS = new HiCSDependenceMeasure(kogo, 50, 0.1, rnd);
        // System.out.print(HiCS.dependence(data1, data2));

        double[] rank = HiCS.ranks(adapter, data1, 6);
        int[] sortInd = HiCS.sortedIndex(adapter, data1, 6);
        System.out.println("done");
    }
}
