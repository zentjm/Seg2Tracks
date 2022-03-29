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
import externalSegmentation.ExternalSegmentation;
import internalSegmentation.InternalSegmentation;
import linkage.Linkage;

public class Seg2TracksClassLoader extends ClassLoader {

	String fileName = "seg2tracks.config";
	
	//For operation panel
	ArrayList<String> linkageClasses = new ArrayList<String>();
	ArrayList<String> internalSegmentationClasses = new ArrayList<String>();
	ArrayList<String> externalSegmentationClasses = new ArrayList<String>();
	
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
            System.out.println("Loaded class name: " + clazz.getName());
             
            //load initial constructor
            Constructor<?> constructor = clazz.getConstructor();
            clazzObject = constructor.newInstance();
       
            
            System.out.println("Class name is: " + clazz.getName());
            
            
            //output
            Method method1 = clazz.getMethod("getName");
            System.out.println("Method 1 name is: " + method1.getName());
            System.out.println("Method 1 output is: " + method1.invoke(clazzObject));
            
            Method method2 = clazz.getMethod("getDescription");
            System.out.println("Method 2 name is: " + method2.getName());
            System.out.println("Method 2 output is: " + method2.invoke(clazzObject));
        	
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
            		if(spl[0].equals("internalSegmentation")) {
            			internalSegmentationClasses.add(line);
            		}
            		if(spl[0].equals("externalSegmentation")) {
            			externalSegmentationClasses.add(line);
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
	
	public InternalSegmentation[] getInternalSegmentationMethods() {
		InternalSegmentation [] arr = new InternalSegmentation[internalSegmentationClasses.size()];
		for (int i = 0; i < internalSegmentationClasses.size(); i ++) {
			arr[i] = (InternalSegmentation) initializeClass(internalSegmentationClasses.get(i));
		}
		return arr;
	}
	
	public ExternalSegmentation[] getExternalSegmentationMethods() {
		ExternalSegmentation [] arr = new ExternalSegmentation[externalSegmentationClasses.size()];
		for (int i = 0; i < externalSegmentationClasses.size(); i ++) {
			arr[i] = (ExternalSegmentation) initializeClass(externalSegmentationClasses.get(i));
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
		String [] arr= new String[internalSegmentationClasses.size()];
		arr = internalSegmentationClasses.toArray(arr);
		return arr;
	}
	
	//TODO: Depreciated?
	public String[] getExternalSegmentationMethodsBinary() {
		String [] arr= new String[externalSegmentationClasses.size()];
		arr = externalSegmentationClasses.toArray(arr);
		return arr;
	}
	
	
	
}



