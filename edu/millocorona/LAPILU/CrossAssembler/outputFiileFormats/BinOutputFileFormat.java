package edu.millocorona.LAPILU.CrossAssembler.outputFiileFormats;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.LinkedList;

import edu.millocorona.LAPILU.CrossAssembler.exceptions.AssemblyException;

public class BinOutputFileFormat implements OutputFileFormat{

	@Override
	public void outputFormatedFile(String outputFileName,LinkedList<String> binaryMachineCodeStrings) throws AssemblyException,IOException {
		if(!outputFileName.endsWith(".bin")) {
			outputFileName+=".bin";	
		}
		File outputFile = new File(outputFileName);
		if(outputFile.getParentFile().exists()) {
			outputFile.createNewFile();
			StringBuilder binaryMachineCodeStringBuilder = new StringBuilder();
			for(String binaryMachineCodeString:binaryMachineCodeStrings) {
				binaryMachineCodeStringBuilder.append(binaryMachineCodeString);
			}
			String binaryMachineCode = binaryMachineCodeStringBuilder.toString();
			BitSet bitSet = new BitSet(binaryMachineCode.length());
			int bitcounter = 0;
			for(char bitAsChar : binaryMachineCode.toCharArray()) {
			    if(bitAsChar == '1') {
			        bitSet.set(bitcounter);
			    }
			    bitcounter++;
			}
			FileOutputStream fileOutputStream = new FileOutputStream(outputFileName);
			fileOutputStream.write(bitSet.toByteArray());
			fileOutputStream.close();
		}else {
			throw new AssemblyException("The directory: "+outputFile.getParentFile().getAbsolutePath()+" does not exist");
		}
		
		
	}

}
