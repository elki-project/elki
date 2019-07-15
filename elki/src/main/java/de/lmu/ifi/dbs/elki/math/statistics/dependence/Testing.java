package de.lmu.ifi.dbs.elki.math.statistics.dependence;

import de.lmu.ifi.dbs.elki.math.statistics.tests.KolmogorovSmirnovTest;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.DoubleArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.arraylike.NumberArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;

import java.util.stream.IntStream;

public class Testing {

    public static void main(String[] args) {
        double[] data1 = {9.0, 9.0, 9.0, 3.0, 7.0, 1.0, 1.0, 1.0, 10.0};
        // double[] data2 = {18.0, 10.0, 6.0, 14.0, 2.0, 4.0};
        NumberArrayAdapter adapter = DoubleArrayAdapter.STATIC;

        KolmogorovSmirnovTest kogo = new KolmogorovSmirnovTest();
        RandomFactory rnd = new RandomFactory(5);
        HiCSDependenceMeasure HiCS = new HiCSDependenceMeasure(kogo, 50, 0.1, rnd);
        MCDEDependenceMeasure MCDE = new MCDEDependenceMeasure(kogo, 50, 0.5, 0.5, rnd);
        // System.out.print(HiCS.dependence(data1, data2));

        double[] rank = HiCS.ranks(adapter, data1, 9);
        int[] sortInd = HiCS.sortedIndex(adapter, data1, 9);
        double[] r = IntStream.range(1, 10).mapToDouble(x -> x).toArray();
        MCDEDependenceMeasure.IndexTriple[] testTriple = MCDEDependenceMeasure.correctedRank(adapter, data1, 9);

        System.out.println("KKK");
    }
}
