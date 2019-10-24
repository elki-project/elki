package elki.math.statistics.dependence.mcde;

import elki.math.statistics.dependence.MCDEDependence;
import elki.utilities.ELKIBuilder;
import elki.utilities.datastructures.arraylike.DoubleArrayAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Test {

  public static void main(String[] args){
    MCDEDependence mwp = new ELKIBuilder<>(MCDEDependence.class) //
        .with(MCDEDependence.Par.M_ID, 1000) //
        .with(MCDEDependence.Par.TEST_ID, MWPTest.STATIC) //
        .build();
    DoubleArrayAdapter adapter = DoubleArrayAdapter.STATIC;
    Random random = new Random();

    List data = new ArrayList();
    double[] data3 = new double[10];
    double[] data4 = new double[10];

    for(int j = 1; j < 5; j++){
      double[] data1 = new double[10];

      for(int i = 0; i < 10; i++){
        data1[i] = i*j;
      }
      data.add(data1);
    }

    for (int i = 0; i < 1; i++) {
      double val = mwp.higherOrderDependence(adapter, data);
      // val = mwp.dependence(data3, data4);
      System.out.println(val);
    }
  }
}
