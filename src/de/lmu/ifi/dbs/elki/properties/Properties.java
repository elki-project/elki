package de.lmu.ifi.dbs.elki.properties;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.logging.AbstractLoggable;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Provides management of properties.
 *
 * @author Arthur Zimek
 */
public final class Properties extends AbstractLoggable {

    /**
     * The pattern to split for separate entries in a property string, which is
     * a &quot;,&quot;.
     */
    public static final Pattern PROPERTY_SEPARATOR = Pattern.compile(",");

    /**
     *
     */
    private static Properties temporalProperties;

    static {
        File propertiesfile = new File(Properties.class.getPackage().getName().replace('.', File.separatorChar) + File.separatorChar + "KDDFramework.prp");
        if (propertiesfile.exists() && propertiesfile.canRead()) {
            temporalProperties = new Properties(propertiesfile.getAbsolutePath());
        }
        else // otherwise, the property-file should at least be available within the jar-archive
        {
            temporalProperties = new Properties(Properties.class.getPackage().getName().replace('.', '/') + '/' + "KDDFramework.prp");
        }
    }

    /**
     * The Properties for the KDDFramework.
     */
    public static final Properties KDD_FRAMEWORK_PROPERTIES = temporalProperties;

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
        super(LoggingConfiguration.DEBUG);
        if (LoggingConfiguration.isChangeable()) {
            LoggingConfiguration.configureRoot(LoggingConfiguration.CLI);
        }
        this.PROPERTIES = new java.util.Properties();
        try {
            PROPERTIES.load(ClassLoader.getSystemResourceAsStream(filename));
        }
        catch (Exception e) {
            warning("Unable to load properties file " + filename + ".\n");
        }
        if (PROPERTIES.containsKey(PropertyName.DEBUG_LEVEL.getName()) && LoggingConfiguration.isChangeable()) {
            LoggingConfiguration.configureRoot(LoggingConfiguration.CLI);
        }
    }

    /**
     * Provides the entries (as separated by
     * {@link #PROPERTY_SEPARATOR PROPERTY_SEPARATOR}) for a specified
     * PropertyName.
     *
     * @param propertyName the PropertyName of the property to retrieve
     * @return the entries (separated by
     *         {@link #PROPERTY_SEPARATOR PROPERTY_SEPARATOR}) for the
     *         specified PropertyName - if the property is undefined, the
     *         returned array is of length 0
     */
    public String[] getProperty(PropertyName propertyName) {
        String property = propertyName == null ? null : PROPERTIES.getProperty(propertyName.getName());
        return property == null ? new String[0] : PROPERTY_SEPARATOR.split(property);
    }

    /**
     * Provides a description string listing all classes for the given
     * superclass or interface as specified in the properties.
     *
     * @param superclass the class to be extended or interface to be implemented
     * @return a description string listing all classes for the given superclass
     *         or interface as specified in the properties
     */
    @SuppressWarnings("unchecked")
    public String restrictionString(Class superclass) {
        StringBuilder info = new StringBuilder();
        info.append("(");
        if (superclass.isInterface()) {
            info.append("implementing ");
        }
        else {
            info.append("extending ");
        }
        info.append(superclass.getName());
        PropertyName propertyName = PropertyName.getOrCreatePropertyName(superclass);
        if (propertyName == null) {
            warning("Could not create PropertyName for " + superclass.toString() + "\n");
        }
        else {
            String[] classNames = getProperty(propertyName);
            if (classNames.length > 0) {
                info.append(" -- available classes:\n");
                for (String name : classNames) {
                    try {
                        if (superclass.isAssignableFrom(Class.forName(name))) {
                            info.append("-->");
                            info.append(name);
                            info.append('\n');
                        }
                        else {
                            warning("Invalid classname \"" + name + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file\n");
                        }
                    }
                    catch (ClassNotFoundException e) {
                        warning("Invalid classname \"" + name + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file - " + e.getMessage() + " - " + e.getClass().getName() + "\n");
                    }
                    catch (ClassCastException e) {
                        warning("Invalid classname \"" + name + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file - " + e.getMessage() + " - " + e.getClass().getName() + "\n");
                    }
                    catch (NullPointerException e) {
                        if (this.debug) {

                            debugFinest(e.getClass().getName() + ": " + e.getMessage());
                        }
                    }
                    catch (Exception e) {
                        exception(e.getMessage(), e);
                    }
                }
            }
            else {
                warning("Not found properties for property name: " + propertyName.getName() + "\n");
            }
        }
        info.append(")");
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
        for (String entry : entries) {
            try {
                String desc = "";
                Object propertyInstance = propertyName.getType().cast(propertyName.classForName(entry).newInstance());
                if (propertyInstance instanceof Algorithm) {
                    // TODO: description -- check whether this provides the
                    // desired result
                    desc = ((Algorithm<?>) propertyInstance).getDescription().toString();
                }
                else if (propertyInstance instanceof Parameterizable) {
                    desc = ((Parameterizable) propertyInstance).parameterDescription();
                }
                result.add(new PropertyDescription(entry, desc));
            }
            catch (InstantiationException e) {
                warning("Invalid classname \"" + entry + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file: " + e.getMessage() + " - " + e.getClass().getName() + "\n");
            }
            catch (IllegalAccessException e) {
                warning("Invalid classname \"" + entry + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file: " + e.getMessage() + " - " + e.getClass().getName() + "\n");
            }
            catch (ClassNotFoundException e) {
                warning("Invalid classname \"" + entry + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file: " + e.getMessage() + " - " + e.getClass().getName() + "\n");
            }
        }
        PropertyDescription[] propertyDescription = new PropertyDescription[result.size()];
        result.toArray(propertyDescription);
        return propertyDescription;

    }

    /**
     * Provides a listing of all subclasses for the given
     * superclass or interface as specified in the properties.
     *
     * @param superclass the class to be extended or interface to be implemented
     * @return a listing of all subclasses for the given
     *         superclass or interface as specified in the properties
     */
    @SuppressWarnings("unchecked")
    public List<Class<?>> subclasses(Class superclass) {
        List<Class<?>> subclasses = new ArrayList<Class<?>>();
        PropertyName propertyName = PropertyName.getOrCreatePropertyName(superclass);
        if (propertyName == null) {
            warning("Could not create PropertyName for " + superclass.toString() + "\n");
        }
        else {
            String[] classNames = getProperty(propertyName);
            if (classNames.length > 0) {
                for (String className : classNames) {
                    try {
                        if (superclass.isAssignableFrom(Class.forName(className))) {
                            subclasses.add(Class.forName(className));
                        }
                        else {
                            warning("Invalid classname \"" + className + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file\n");
                        }
                    }
                    catch (ClassNotFoundException e) {
                        warning("Invalid classname \"" + className + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file - " + e.getMessage() + " - " + e.getClass().getName() + "\n");
                    }
                    catch (ClassCastException e) {
                        warning("Invalid classname \"" + className + "\" for property \"" + propertyName.getName() + "\" of class \"" + propertyName.getType().getName() + "\" in property-file - " + e.getMessage() + " - " + e.getClass().getName() + "\n");
                    }
                    catch (NullPointerException e) {
                        if (this.debug) {

                            debugFinest(e.getClass().getName() + ": " + e.getMessage());
                        }
                    }
                    catch (Exception e) {
                        exception(e.getMessage(), e);
                    }
                }
            }
            else {
                warning("Not found properties for property name: " + propertyName.getName() + "\n");
            }
        }
        return subclasses;
    }
}
