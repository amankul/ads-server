package com.phunware.ads.utilities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class PropertiesFileUtility {
	
	private Properties pro;
	private FileInputStream fi=null;
	
	public PropertiesFileUtility(String path){
		pro= new Properties();
		try {
			fi= new FileInputStream(path);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} 
		try {
			pro.load(fi);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public String getProperty(String propertyName){
		return pro.getProperty(propertyName);
	}
	

}
