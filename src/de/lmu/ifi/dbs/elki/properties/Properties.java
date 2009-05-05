package de.lmu.ifi.dbs.elki.properties;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Provides management of properties.
 * 
 * @author Arthur Zimek
 */
public final class Properties {
  private static Logging logger = Logging.getLogger(Properties.class);

  /**
   * The pattern to split for separate entries in a property string, which is a
   * &quot;,&quot;.
   */
  public static final Pattern PROPERTY_SEPARATOR = Pattern.compile(",");

  /**
   * Non-breaking unicode space character.
   */
  public static final String NONBREAKING_SPACE = "\u00a0";

  /**
     *
     */
  private static Properties temporalProperties;

  static {
    File propertiesfile = new File(Properties.class.getPackage().getName().replace('.', File.separatorChar) + File.separatorChar + "ELKI.prp");
    if(propertiesfile.exists() && propertiesfile.canRead()) {
      temporalProperties = new Properties(propertiesfile.getAbsolutePath());
    }
    else // otherwise, the property-file should at least be available within the
         // jar-archive
    {
      temporalProperties = new Properties(Properties.class.getPackage().getName().replace('.', '/') + '/' + "ELKI.prp");
    }
  }

  /**
   * The Properties for ELKI.
   */
  public static final Properties ELKI_PROPERTIES = temporalProperties;

  /**
   * Stores the properties as defined by a property-file.
   */
  private final java.util.Properties PROPERTIES;

  /**
   * Provides the properties as defined in the designated file.
   * 
   * @param filename name of a file to provide property-definitions.
   */
  private Properties(String filename) {
    LoggingConfiguration.assertConfigured();
    this.PROPERTIES = new java.util.Properties();
    try {
      PROPERTIES.load(ClassLoader.getSystemResourceAsStream(filename));
    }
    catch(Exception e) {
      logger.warning("Unable to load properties file " + filename + ".\n");
    }
    // if (PROPERTIES.containsKey(PropertyName.DEBUG_LEVEL.getName()) &&
    // LoggingConfiguration.isChangeable()) {
    // LoggingConfiguration.configureRoot(LoggingConfiguration.CLI);
    // }
  }

  /**
   * Provides the entries (as separated by {@link #PROPERTY_SEPARATOR
   * PROPERTY_SEPARATOR}) for a specified PropertyName.
   * 
   * @param propertyName the PropertyName of the property to retrieve
   * @return the entries (separated by {@link #PROPERTY_SEPARATOR
   *         PROPERTY_SEPARATOR}) for the specified PropertyName - if the
   *         property is undefined, the returned array is of length 0
   */
  public String[] getProperty(PropertyName propertyName) {
    String property = propertyName == null ? null : PROPERTIES.getProperty(propertyName.getName());
    return property == null ? new String[0] : PROPERTY_SEPARATOR.split(property);
  }

  /**
   * Provides a description string listing all classes for the given superclass
   * or interface as specified in the properties.
   * 
   * @param superclass the class to be extended or interface to be implemented
   * @return a description string listing all classes for the given superclass
   *         or interface as specified in the properties
   */
  public String restrictionString(Class<?> superclass) {
    String prefix = superclass.getPackage().getName() + ".";
    StringBuilder info = new StringBuilder();
    if(superclass.isInterface()) {
      info.append("Implementing ");
    }
    else {
      info.append("Extending ");
    }
    info.append(superclass.getName());
    PropertyName propertyName = PropertyName.getOrCreatePropertyName(superclass);
    if(propertyName == null) {
      logger.warning("Could not create PropertyName for " + superclass.toString());
    }
    else {
      String[] classNames = getProperty(propertyName);
      if(classNames.length > 0) {
        info.append(FormatUtil.NEWLINE);
        info.append("Known classes (default package " + prefix + "):");
        info.append(FormatUtil.NEWLINE);
        for(String name : classNames) {
          // skip commented classes.
          if (name.charAt(0) == '#') {
            continue;
          }
          try {
            if(superclass.isAssignableFrom(Class.forName(name))) {
              info.append("->" + NONBREAKING_SPACE);
              if(name.startsWith(prefix)) {
                info.append(name.substring(prefix.length()));
              }
              else {
                info.append(name);
              }
              info.append(FormatUtil.NEWLINE);
            }
            else {
              logger.warning("Invalid classname \"" + name + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file\n");
            }
          }
          catch(ClassNotFoundException e) {
            logger.warning("Invalid classname \"" + name + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file - " + e.getMessage() + " - " + e.getClass().getName() + "\n");
          }
          catch(ClassCastException e) {
            logger.warning("Invalid classname \"" + name + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file - " + e.getMessage() + " - " + e.getClass().getName() + "\n");
          }
          catch(NullPointerException e) {
            if(logger.isDebuggingFinest()) {
              logger.debugFinest(e.getClass().getName() + ": " + e.getMessage());
            }
          }
          catch(Exception e) {
            logger.exception("Exception building class restriction string.", e);
          }
        }
      }
      else {
        if(logger.isDebugging()) {
          logger.debug("Not found properties for property name: " + propertyName.getName() + "\n");
        }
      }
    }
    return info.toString();
  }

  /**
   * Returns an array of PropertyDescription for all entries for the given
   * PropertyName.
   * 
   * @param propertyName the Propertyname of the property to retrieve
   * @return PropertyDescriptins for all entries of the given PropertyName
   */
  public PropertyDescription[] getProperties(PropertyName propertyName) {
    String[] entries = getProperty(propertyName);
    List<PropertyDescription> result = new ArrayList<PropertyDescription>();
    for(String entry : entries) {
      try {
        String desc = "";
        Object propertyInstance = propertyName.getType().cast(propertyName.classForName(entry).newInstance());
        if(propertyInstance instanceof Algorithm) {
          // TODO: description -- check whether this provides the
          // desired result
          desc = ((Algorithm<?, ?>) propertyInstance).getDescription().toString();
        }
        else if(propertyInstance instanceof Parameterizable) {
          desc = ((Parameterizable) propertyInstance).parameterDescription();
        }
        result.add(new PropertyDescription(entry, desc));
      }
      catch(InstantiationException e) {
        logger.warning("Invalid classname \"" + entry + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file: " + e.getMessage() + " - " + e.getClass().getName() + "\n");
      }
      catch(IllegalAccessException e) {
        logger.warning("Invalid classname \"" + entry + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file: " + e.getMessage() + " - " + e.getClass().getName() + "\n");
      }
      catch(ClassNotFoundException e) {
        logger.warning("Invalid classname \"" + entry + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file: " + e.getMessage() + " - " + e.getClass().getName() + "\n");
      }
    }
    PropertyDescription[] propertyDescription = new PropertyDescription[result.size()];
    result.toArray(propertyDescription);
    return propertyDescription;

  }

  /**
   * Provides a listing of all subclasses for the given superclass or interface
   * as specified in the properties.
   * 
   * @param superclass the class to be extended or interface to be implemented
   * @return a listing of all subclasses for the given superclass or interface
   *         as specified in the properties
   */
  @SuppressWarnings("unchecked")
  public List<Class<?>> subclasses(Class superclass) {
    List<Class<?>> subclasses = new ArrayList<Class<?>>();
    PropertyName propertyName = PropertyName.getOrCreatePropertyName(superclass);
    if(propertyName == null) {
      logger.warning("Could not create PropertyName for " + superclass.toString() + "\n");
    }
    else {
      String[] classNames = getProperty(propertyName);
      if(classNames.length > 0) {
        for(String className : classNames) {
          try {
            if(superclass.isAssignableFrom(Class.forName(className))) {
              subclasses.add(Class.forName(className));
            }
            else {
              logger.warning("Invalid classname \"" + className + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file\n");
            }
          }
          catch(ClassNotFoundException e) {
            logger.warning("Invalid classname \"" + className + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file - " + e.getMessage() + " - " + e.getClass().getName() + "\n");
          }
          catch(ClassCastException e) {
            logger.warning("Invalid classname \"" + className + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file - " + e.getMessage() + " - " + e.getClass().getName() + "\n");
          }
          catch(NullPointerException e) {
            if(logger.isDebuggingFinest()) {

              logger.debugFinest(e.getClass().getName() + ": " + e.getMessage());
            }
          }
          catch(Exception e) {
            logger.exception(e.getMessage(), e);
          }
        }
      }
      else {
        logger.warning("Not found properties for property name: " + propertyName.getName() + "\n");
      }
    }
    return subclasses;
  }
}
