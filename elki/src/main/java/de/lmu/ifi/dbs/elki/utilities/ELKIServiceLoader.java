package de.lmu.ifi.dbs.elki.utilities;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.BufferedLineReader;

/**
 * Class that emulates the behavior of an java ServiceLoader, except that the
 * classes are <em>not</em> automatically instantiated. This is more lazy, but
 * also we need to do the instantiations our way with the parameterizable API.
 *
 * @author Erich Schubert
 */
public class ELKIServiceLoader {
  /**
   * Resource name prefix for the ELKI functionality discovery.
   *
   * Note: resources are always separated with /, even on Windows.
   */
  public static final String RESOURCE_PREFIX = "META-INF/elki/";

  /**
   * File name prefix for the ELKI functionality discovery.
   */
  public static final String FILENAME_PREFIX = "META-INF" + File.separator + "elki" + File.separator;

  /**
   * Comment character
   */
  public static final char COMMENT_CHAR = '#';

  /**
   * Parent class
   */
  private Class<?> parent;

  /**
   * Classloader
   */
  private ClassLoader cl;

  /**
   * Constructor.
   *
   * @param parent Parent class
   * @param cl Classloader to use for loading resources
   */
  public ELKIServiceLoader(Class<?> parent, ClassLoader cl) {
    this.parent = parent;
    this.cl = cl;
  }

  /**
   * Constructor, using the system class loader.
   *
   * @param parent Parent class
   */
  public ELKIServiceLoader(Class<?> parent) {
    this(parent, ClassLoader.getSystemClassLoader());
  }

  /**
   * Load the service file.
   */
  public void load() {
    ELKIServiceRegistry registry = ELKIServiceRegistry.singleton();
    try {
      String fullName = RESOURCE_PREFIX + parent.getName();
      Enumeration<URL> configfiles = cl.getResources(fullName);
      while(configfiles.hasMoreElements()) {
        URL nextElement = configfiles.nextElement();
        try (
            InputStreamReader is = new InputStreamReader(nextElement.openStream(), "utf-8");
            BufferedLineReader r = new BufferedLineReader(is)) {
          while(r.nextLine()) {
            parseLine(r.getBuffer(), registry, nextElement);
          }
        }
        catch(IOException x) {
          throw new AbortException("Error reading configuration file", x);
        }
      }
    }
    catch(IOException x) {
      throw new AbortException("Could not load service configuration files.", x);
    }
  }

  /**
   * Parse a single line from a service registry file.
   *
   * @param line Line to read
   * @param registry Registry to update
   * @param nam File name for error reporting
   */
  private void parseLine(CharSequence line, ELKIServiceRegistry registry, URL nam) {
    if(line == null) {
      return;
    }
    int begin = 0, end = line.length();
    while(begin < end && line.charAt(begin) == ' ') {
      begin++;
    }
    if(begin >= end || line.charAt(begin) == '#') {
      return; // Empty/comment lines are okay, continue
    }
    assert(begin == 0 || line.charAt(begin - 1) == ' ');
    // Find end of class name:
    int cend = begin + 1;
    while(cend < end && line.charAt(cend) != ' ') {
      cend++;
    }
    // Class name:
    String cname = line.subSequence(begin, cend).toString();
    registry.register(parent, cname);
    for(int abegin = cend + 1, aend = -1; abegin < end; abegin = aend + 1) {
      // Skip whitespace:
      while(abegin < end && line.charAt(abegin) == ' ') {
        abegin++;
      }
      // Find next whitespace:
      aend = abegin + 1;
      while(aend < end && line.charAt(aend) != ' ') {
        aend++;
      }
      if(abegin < aend) {
        registry.registerAlias(parent, line.subSequence(abegin, aend).toString(), cname);
      }
    }
    return;
  }
}
