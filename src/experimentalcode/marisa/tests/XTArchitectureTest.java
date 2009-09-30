package experimentalcode.marisa.tests;

import java.io.IOException;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import experimentalcode.marisa.index.xtree.common.XTree;

public class XTArchitectureTest {

  public static void main(String[] args) throws NumberFormatException, ParameterException, IOException {
    LoggingConfiguration.reconfigureLogging("experimentalcode.marisa.tests", "logging-cli.properties");
    String queryFileName = "C:/WORK/Theseus/data/synthetic/15Dqueries.csv";
    String xtDataName = "C:/WORK/Theseus/data/synthetic/5DUniform.csv";
    XTree<DoubleVector> xt = XTreeIO.buildXTree(xtDataName, "", 1024, .9, .3, 100000, false);
    System.out.println("XT: " + xt.toString());
    KNNTests.amICorrect(xt, queryFileName);
    KNNTests.speedTest(xt, queryFileName);
    
    xt = XTreeIO.buildXTree(xtDataName, "", 1024, .75, .3, 100000, false);
    System.out.println("XT: " + xt.toString());
    KNNTests.amICorrect(xt, queryFileName);
    KNNTests.speedTest(xt, queryFileName);
    
    xt = XTreeIO.buildXTree(xtDataName, "", 1024, .45, .2, 100000, false);
    System.out.println("XT: " + xt.toString());
    KNNTests.amICorrect(xt, queryFileName);
    KNNTests.speedTest(xt, queryFileName);
    
    xt = XTreeIO.buildXTree(xtDataName, "", 1024, .3, .3, 100000, false);
    System.out.println("XT: " + xt.toString());
    KNNTests.amICorrect(xt, queryFileName);
    KNNTests.speedTest(xt, queryFileName);
    
    xt = XTreeIO.buildXTree(xtDataName, "", 1024, .1, .3, 100000, false);
    System.out.println("XT: " + xt.toString());
    KNNTests.amICorrect(xt, queryFileName);
    KNNTests.speedTest(xt, queryFileName);
  }
}
