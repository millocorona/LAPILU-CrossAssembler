package edu.millocorona.LAPILU.CrossAssembler.util;

import java.util.LinkedList;

public class StringUtils {
	
	public static int countMatches(String str,String findStr) {
		int lastIndex = 0;
		int count = 0;
		while(lastIndex != -1){
		    lastIndex = str.indexOf(findStr,lastIndex);
		    if(lastIndex != -1){
		        count ++;
		        lastIndex += findStr.length();
		    }
		}
		
		return count;
	}
	
	public static LinkedList<Integer> getIndexesOfMatches(String str,String findStr) {
		LinkedList<Integer> indexes = new LinkedList<Integer>();
		int lastIndex = 0;
		while(lastIndex != -1){
		    lastIndex = str.indexOf(findStr,lastIndex);
		    if(lastIndex != -1){
		    	indexes.add(lastIndex);
		        lastIndex += findStr.length();
		    }
		}
		return indexes;
	}
}
