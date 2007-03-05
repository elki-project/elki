package de.lmu.ifi.dbs.gui;

import java.awt.BorderLayout;
import java.beans.*;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;

import javax.swing.JFrame;

import de.lmu.ifi.dbs.algorithm.clustering.DBSCAN;

public class KDDGui extends JFrame {

	public static final String FRAME_TITLE = "KDD Workbench";
	
	private KDDTabbedPane tabbedPane; 
	
	private MessagePanel messagePanel = new MessagePanel();
	
	public KDDGui(){
		
//		try{
//		Pattern.compile("A||.[B");
//		}
//		catch(PatternSyntaxException e){
//			System.out.println(e.getMessage());
//		}
//		
		setTitle(FRAME_TITLE);
		
		// add the tabbedPane
		tabbedPane = new KDDTabbedPane(this);
		add(tabbedPane, BorderLayout.CENTER);
		
		// add the message panel
		add(messagePanel,BorderLayout.SOUTH);
		
		// if the window is closed exit the program
//		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		pack();
		setSize(700,500);
		setVisible(true);
		
//		test();
		
	}
	
	
	/*private void test(){
		
		
		try {
			
		      BeanInfo bi = Introspector.getBeanInfo(new DBSCAN().getClass());
		      PropertyDescriptor[] m_Properties = bi.getPropertyDescriptors();
		      MethodDescriptor[] m_Methods = bi.getMethodDescriptors();
		      BeanDescriptor bDes = bi.getBeanDescriptor();
		      
		      System.out.println(bDes.getDisplayName());
		      
		      Enumeration<String> attributes = bDes.attributeNames();
		      
		     Object o = new DBSCAN();
		      
//		      for(Field f : (o.getClass().getFields())){
//		    	  String fieldName = f.getName();
//		          Class typeClass = f.getType();
//		          String fieldType = typeClass.getName();
//		          System.out.println( "  " + fieldType + " " + fieldName + ";" );
//		      }
//		      
//		      for(Method m : (o.getClass().getMethods())){
//		    	  String mName = m.getName();
//		          System.out.println("method name: "+mName);
//		          if(mName.equals("getParameters")){
//		        	  try {
//						Object result = m.invoke(o, null);
//						
//						System.out.println(result);
//						if(result.getClass().isArray()){
//							System.out.println("yo");
//						}
//					} catch (IllegalArgumentException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (IllegalAccessException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					} catch (InvocationTargetException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//		          }
//		          
//		      }
		      
		      
		      System.out.println("possible parameters:");
		      for(String param : (new DBSCAN().getParameters())){
		    	  
		    	  System.out.println("param: "+param);
		      }
		      System.out.println("");
		     while(attributes.hasMoreElements()){
		    	 System.out.println("hello");
		    	  System.out.println("attributes: "+attributes.nextElement());
		      }
		      
		      for (int i = 0;i < m_Methods.length; i++) {
			        String name = m_Methods[i].getDisplayName();
			        Method meth = m_Methods[i].getMethod();
			        
			        System.out.println("method name: "+name);
			        
		      }
		      
		      for (int i = 0; i < m_Properties.length; i++) {
		    	  
		    	  String name = m_Properties[i].getDisplayName();
		          Class type = m_Properties[i].getPropertyType();
		          Method getter = m_Properties[i].getReadMethod();
		          Method setter = m_Properties[i].getWriteMethod();
		          
		          System.out.println("property: "+name);
		          System.out.println("property type: "+type);
		          
		          if(type.isArray() && getter.getName().equals("getOptions")){
		        	  System.out.println("yo!");
		        	 try {
						Object a = getter.invoke(o, null);
						 int length = Array.getLength(a);
						 System.out.println("length: "+length);
			        	  for(int j = 0; j < length; j++){
			        		  System.out.println(Array.get(a, j));
			        	  }
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		        	 
						
		          }
		      }
		      
		    } catch (IntrospectionException ex) {
		      System.err.println("PropertySheet: Couldn't introspect");
		      return;
		    }
		   
	}
	
	*/
	
	
	
	
	public static void main(String[] args) {

		KDDGui workbench = new KDDGui();
	}
	
	
	
}