package de.lmu.ifi.dbs.logging;

public class StaticLogger extends AbstractLoggable {

	
	public StaticLogger(String className){
		super(LoggingConfiguration.DEBUG,className);
	}
	
	public boolean debug(){
		return this.debug;
	}
	

	
}
