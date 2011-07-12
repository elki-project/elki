package experimentalcode.marisa.tests;

import java.io.IOException;

import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import experimentalcode.shared.index.xtree.XTree;

public class QuickBuild {
  public static void main(String[] args) throws NumberFormatException, ParameterException, IOException {
    LoggingConfiguration.reconfigureLogging("experimentalcode.marisa.tests", "logging-cli.properties");
//    XTree<DoubleVector> xt = XTreeIO.buildXTree("C:/WORK/Theseus/data/synthetic/5DUniform.csv", "", 1024, .45, .3, 10000, false);
//    XTreeIO.REINSERT_FRACTION = .2;
    XTree xt = XTreeIO.buildXTree("C:/WORK/Theseus/data/synthetic/5DUniform.csv", "", 1024, .25, .3, 10000, false);
    System.out.println("XT: " + xt.toString());
  }
}
