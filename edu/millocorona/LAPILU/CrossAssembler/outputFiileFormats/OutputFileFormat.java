package edu.millocorona.LAPILU.CrossAssembler.outputFiileFormats;

import java.io.IOException;
import java.util.LinkedList;

import edu.millocorona.LAPILU.CrossAssembler.exceptions.AssemblyException;

public interface OutputFileFormat {
	
	public void outputFormatedFile(String outputFileName,LinkedList<String> binaryMachineCodeStrings) throws AssemblyException,IOException;
	
}
