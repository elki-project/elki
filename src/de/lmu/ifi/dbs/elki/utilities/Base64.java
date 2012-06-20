package de.lmu.ifi.dbs.elki.utilities;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Class to wrap various Base64 encoders that could be available.
 * 
 * This is a rather ugly hack; it would maybe have been sensible to just import
 * one of the publicly available (and fast) Base64 encoders. The expectation was
 * that at some point, Oracle will acutally include a public and fast Base64
 * encoder in Java.
 * 
 * @author Erich Schubert
 */
public final class Base64 {
  /**
   * Instance of sun.misc.BASE64Encoder
   */
  private static Object sunj6i;

  /**
   * Encode method
   */
  private static Method sunj6m;

  /**
   * Instance of java.util.prefs.Base64
   */
  private static Object jup6i;

  /**
   * Encode method
   */
  private static Method jup6m;

  // Initialize
  static {
    // Try Java 6
    {
      try {
        Class<?> c = ClassLoader.getSystemClassLoader().loadClass("sun.misc.BASE64Encoder");
        sunj6i = c.newInstance();
        sunj6m = c.getMethod("encode", byte[].class);
      }
      catch(Throwable e) {
        // de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
        // Ignore.
        sunj6i = null;
        sunj6m = null;
      }
    }
    // Try private class in Java6 preferences
    {
      try {
        Class<?> c = ClassLoader.getSystemClassLoader().loadClass("java.util.prefs.Base64");
        Constructor<?> cons = c.getDeclaredConstructor();
        cons.setAccessible(true);
        jup6i = cons.newInstance();
        jup6m = c.getDeclaredMethod("byteArrayToBase64", byte[].class);
        jup6m.setAccessible(true);
      }
      catch(Throwable e) {
        // de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
        // Ignore.
        jup6i = null;
        jup6m = null;
      }
    }
    if(sunj6i == null && jup6i == null) {
      de.lmu.ifi.dbs.elki.logging.LoggingUtil.warning("No usable Base64 encoders detected.");
    }
  }

  /**
   * Encode a string as Base64.
   * 
   * @param s Bytes to encode
   * @return Result string
   */
  public static final String encodeBase64(byte[] s) {
    if(jup6i != null && jup6m != null) {
      try {
        return (String) jup6m.invoke(jup6i, s);
      }
      catch(Exception e) {
        throw new RuntimeException("java.util.prefs.Base64 is not working.");
      }
    }
    if(sunj6i != null && sunj6m != null) {
      try {
        return (String) sunj6m.invoke(sunj6i, s);
      }
      catch(Exception e) {
        throw new RuntimeException("sun.misc.BASE64Encoder is not working.");
      }
    }
    throw new RuntimeException("No usable Base64 encoder detected.");
  }
}