package de.lmu.ifi.dbs.elki.test;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;

/**
 * Build a test suite with all tests included with ELKI
 * 
 * @author Erich Schubert
 */
public class AllTests extends TestSuite {
  public static Test suite() {
    TestSuite suite = new TestSuite();
    List<Class<?>> tests = InspectionUtil.findAllImplementations(TestCase.class, false);
    for(Class<?> cls : tests) {
      if (cls == AllTests.class) {
        continue;
      }
      Class<? extends TestCase> tcls = cls.asSubclass(TestCase.class);
      if(tcls != null) {
        suite.addTestSuite(tcls);
      }
    }
    return suite;
  }
}
