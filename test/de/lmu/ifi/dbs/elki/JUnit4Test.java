package de.lmu.ifi.dbs.elki;

/**
 * This interface is used for test-discovery by the {@link AllTests} TestSuite.
 * 
 * While it would be convenient to use {@link junit.framework.TestCase}, this
 * causes at least Eclipse to assume it is a JUnit3 test, and thus will not
 * process {@link org.junit.Before} and {@link org.junit.After} annotations
 * or run tests that don't start with "test" in their method name.
 * 
 * @author Erich Schubert
 */
public interface JUnit4Test {
  // empty marker interface
}