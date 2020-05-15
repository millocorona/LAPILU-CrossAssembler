package edu.millocorona.LAPILU.CrossAssembler.outputFiileFormats;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

import edu.millocorona.LAPILU.CrossAssembler.exceptions.AssemblyException;

public class CoeOutputFileFormat implements OutputFileFormat{

	@Override
	public void outputFormatedFile(String outputFileName,LinkedList<String> binaryMachineCodeStrings) throws AssemblyException,IOException {
		if(!outputFileName.endsWith(".coe")) {
			outputFileName+=".coe";	
		}
		File outputFile = new File(outputFileName);
		if(outputFile.getParentFile().exists()) {
			outputFile.createNewFile();
			PrintWriter writer = new PrintWriter(outputFile, "UTF-8");
			writer.println("memory_initialization_radix=2;");
			writer.println("memory_initialization_vector=");
			for(String binaryMachineCodeString:binaryMachineCodeStrings) {
				writer.println(binaryMachineCodeString+",");
			}
			writer.close();
		}else {
			throw new AssemblyException("The directory: "+outputFile.getParentFile().getAbsolutePath()+" does not exist");
		}
	}

}
