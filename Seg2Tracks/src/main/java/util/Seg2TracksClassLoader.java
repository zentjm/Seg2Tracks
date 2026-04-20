package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;

import analysisMethod.AnalysisMethod;
import linkage.Linkage;
import sarn.Sarn;
import segmentation.Segmentation;

/**
 * Dynamic class loader for Seg2Tracks plugin extensibility. Reads seg2tracks.config file and
 * dynamically instantiates segmentation, linkage, SARN, and analysis method plugins at runtime.
 * Enables the plugin architecture allowing third-party developers to add custom algorithms
 * without modifying core plugin code.
 */
public class Seg2TracksClassLoader extends ClassLoader {

	String fileName = "seg2tracks.config";
	
	//For operation panel
	ArrayList<String> linkageClasses = new ArrayList<String>();
	ArrayList<String> segmentationClasses = new ArrayList<String>();
	ArrayList<String> sarnClasses = new ArrayList<String>();
	
	//For analysis panel
	ArrayList<String> analysisMethodClasses = new ArrayList<String>();
	
	
	public Seg2TracksClassLoader() {
		//parse config file TODO: Automatically load package contents - remove need for config file
		InputStream stream = getConfigFile(fileName);
		parseConfigFile(stream);
	}
	
	//TODO: Delete, depreciated
	public void obtainClassInfo (String className) {
		
		Object clazzObject = null;
		try {
            
            // Create a new JavaClassLoader 
            ClassLoader classLoader = this.getClass().getClassLoader();
             
            // Load the target class using its binary name
            Class<?> clazz = classLoader.loadClass(className);
            //System.out.println("Loaded class name: " + clazz.getName());
             
            //load initial constructor
            Constructor<?> constructor = clazz.getConstructor();
            clazzObject = constructor.newInstance();
       
            
            //System.out.println("Class name is: " + clazz.getName());
            
            
            //output
            Method method1 = clazz.getMethod("getName");
            //System.out.println("Method 1 name is: " + method1.getName());
            //System.out.println("Method 1 output is: " + method1.invoke(clazzObject));
            
            Method method2 = clazz.getMethod("getDescription");
            //System.out.println("Method 2 name is: " + method2.getName());
            //System.out.println("Method 2 output is: " + method2.invoke(clazzObject));
        	
            if (clazzObject instanceof Linkage) {
    			//System.out.println("Name is " + clazzObject.invoke())
    		}
            
           
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
		
	}
		
	public Object initializeClass (String className) {
		
		Object clazzObject = null;
		try {
            
            // Create a new JavaClassLoader 
            ClassLoader classLoader = this.getClass().getClassLoader();
             
            // Load the target class using its binary name
            Class<?> clazz = classLoader.loadClass(className);
            System.out.println("Loaded class: " + clazz.getName());
             
            //load initial constructor
            Constructor<?> constructor = clazz.getConstructor();
            clazzObject = constructor.newInstance();
            
            return clazzObject;
            
  
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
		
	}
	
	private InputStream getConfigFile (String fileName) {
		   ClassLoader classLoader = getClass().getClassLoader();
		   InputStream inputStream = classLoader.getResourceAsStream(fileName);
		   
		   // the stream holding the file content
	        if (inputStream == null) {
	            throw new IllegalArgumentException("file not found! " + fileName);
	        } else {
	            return inputStream;
	        }
	}
	
	private void parseConfigFile(InputStream is) {

        try (InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader)) {
            	String line;
            	while ((line = reader.readLine()) != null) {
            		
            		//Modify file for linkage.
            		line = line.replace(" ", "");
            		String[] spl = line.split(",");
            		line = line.replace(",",".");
            		
            		if(spl[0].equals("linkage")) {
            			linkageClasses.add(line);
            		}
            		if(spl[0].equals("segmentation")) {
            			segmentationClasses.add(line);
            		}
            		if(spl[0].equals("sarn")) {
            			sarnClasses.add(line);
            		}
            		if(spl[0].equals("analysisMethod")) {
            			analysisMethodClasses.add(line);
            		}
            	}

        } catch (IOException e) {
            e.printStackTrace();
        }
        

    }
	
	public Linkage[] getLinkageMethods() {
		Linkage [] arr = new Linkage[linkageClasses.size()];
		for (int i = 0; i < linkageClasses.size(); i ++) {
			arr[i] = (Linkage) initializeClass(linkageClasses.get(i));
		}
		return arr;
	}
	
	public Segmentation[] getSegmentationMethods() {
		Segmentation [] arr = new Segmentation[segmentationClasses.size()];
		for (int i = 0; i < segmentationClasses.size(); i ++) {
			arr[i] = (Segmentation) initializeClass(segmentationClasses.get(i));
		}
		return arr;
	}
	
	public Sarn[] getSarnMethods() {
		Sarn [] arr = new Sarn[sarnClasses.size()];
		for (int i = 0; i < sarnClasses.size(); i ++) {
			arr[i] = (Sarn) initializeClass(sarnClasses.get(i));
		}
		return arr;
	}
	
	public AnalysisMethod[] getAnalysisMethods() {
		AnalysisMethod [] arr = new AnalysisMethod[analysisMethodClasses.size()];
		for (int i = 0; i < analysisMethodClasses.size(); i ++) {
			arr[i] = (AnalysisMethod) initializeClass(analysisMethodClasses.get(i));
		}
		return arr;
	}
	
	
	
	//TODO: Depreciated?
	public String[] getInternalSegmentationMethodsBinary() {
		String [] arr= new String[segmentationClasses.size()];
		arr = segmentationClasses.toArray(arr);
		return arr;
	}
	
	//TODO: Depreciated?
	public String[] getExternalSegmentationMethodsBinary() {
		String [] arr= new String[sarnClasses.size()];
		arr = sarnClasses.toArray(arr);
		return arr;
	}
	
	
	
}



