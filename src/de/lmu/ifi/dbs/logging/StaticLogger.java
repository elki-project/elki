package de.lmu.ifi.dbs.logging;

/**
 * Subclass of {@link #AbstractLoggable} , can be used in static environmens.
 * 
 * @author Steffi Wanka
 *
 */
public class StaticLogger extends AbstractLoggable {

	
	/**
	 * Initializes a logger with the given name.
	 * 
	 * @param className the name of the logger.
	 */
	public StaticLogger(String className){
		super(LoggingConfiguration.DEBUG,className);
	}
	
	/**
	 * Return true if the logger is enabled for debugging, false otherwise.
	 * 
	 * @return true if the logger is enables for debugging, false otherwise.
	 */
	public boolean debug(){
		return this.debug;
	}
	

	
}
