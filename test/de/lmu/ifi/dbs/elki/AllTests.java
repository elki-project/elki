package de.lmu.ifi.dbs.elki;

import java.util.List;

import junit.framework.JUnit4TestAdapter;
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
  /**
   * Build a test suite with all tests included in ELKI.
   * 
   * @return Test suite
   */
  public static Test suite() {
    TestSuite suite = new TestSuite();
    List<Class<?>> tests = InspectionUtil.findAllImplementations(TestCase.class, false);
    tests.addAll(InspectionUtil.findAllImplementations(JUnit4Test.class, false));
    for(Class<?> cls : tests) {
      if (cls == AllTests.class) {
        continue;
      }
      Test test = new JUnit4TestAdapter(cls);
      if(test != null) {
        suite.addTest(test);
      }
    }
    return suite;
  }
}