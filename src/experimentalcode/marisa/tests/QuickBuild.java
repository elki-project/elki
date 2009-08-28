package experimentalcode.marisa.tests;

import java.io.IOException;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import experimentalcode.marisa.index.xtree.common.XTree;

public class QuickBuild {
  public static void main(String[] args) throws NumberFormatException, ParameterException, IOException {
    LoggingConfiguration.reconfigureLogging("experimentalcode.marisa.tests", "logging-cli.properties");

    XTree<DoubleVector> xt = XTreeIO.buildXTree("C:/WORK/Theseus/data/synthetic/15DUniform.csv", "", 4096, .1, .3, 10000, false);
    System.out.println("XT: " + xt.toString());
  }
}
