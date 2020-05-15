package edu.millocorona.LAPILU.CrossAssembler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

import edu.millocorona.LAPILU.CrossAssembler.exceptions.AssemblyException;
import edu.millocorona.LAPILU.CrossAssembler.outputFiileFormats.OutputFileFormat;
import edu.millocorona.LAPILU.CrossAssembler.util.StringUtils;

public class LAPILUCrossAssembler {
	private int dataBusLength;
	private int addressBusLength;
	private String inputFileName;
	private LinkedList<String> binaryMachineCodeStrings;
	
	public LAPILUCrossAssembler(int dataBusLength, int addressBusLength, String inputFileName) {
		this.dataBusLength = dataBusLength;
		this.addressBusLength = addressBusLength;
		this.inputFileName = inputFileName;
	}
	
	@SuppressWarnings("unchecked")
	public void assembleFile() throws AssemblyException, IOException {		
		String fileContent = new String (Files.readAllBytes(Paths.get(inputFileName)));
		//First we need to remove all the empty lines
		fileContent = fileContent.replaceAll("(?m)^\\s+", "");
		//Now we need to remove the comments
		StringBuilder fileContentWithoutCommentsBuilder = new StringBuilder();
		for(String line:fileContent.split("\n")) {
			if(!line.startsWith("//")) {
				if(line.contains("//")) {
					fileContentWithoutCommentsBuilder.append(line.substring(0,line.indexOf("//")));
					fileContentWithoutCommentsBuilder.append("\n");
				}else {
					fileContentWithoutCommentsBuilder.append(line);
					fileContentWithoutCommentsBuilder.append("\n");
				}
			}
		}
		String fileContentWithoutComments = fileContentWithoutCommentsBuilder.toString().toUpperCase();
		String headSection = "";
		String codeSection = "";
		//Now we need to divide the code in the two sections it can contain, the .head and the .code
		if(fileContentWithoutComments.contains(".head")) {
			int startOfHeadSection = fileContentWithoutComments.indexOf(".head")+6;
			int endOfHeadSection = fileContentWithoutComments.indexOf(".endhead");
			headSection = fileContentWithoutComments.substring(startOfHeadSection,endOfHeadSection);
		}else {
			throw new AssemblyException("The head section is not present in the file");
		}
		if(fileContentWithoutComments.contains(".code")) {
			int startOfCodeSection = fileContentWithoutComments.indexOf(".code")+6;
			int endOfCodeSection = fileContentWithoutComments.indexOf(".endcode");
			codeSection = fileContentWithoutComments.substring(startOfCodeSection,endOfCodeSection);
		}else {
			throw new AssemblyException("The code section is not present in the file");
		}
		HashMap<String,String> constantLabels = new HashMap<String,String>();
		//Now we need to process the head section
		for(String headLine:headSection.split("\n")) {
			if(headLine.contains("=")) {
				//is a constant label
				String labelName  = headLine.split("=")[0].replaceAll("\\s","");
				if(labelName.isEmpty()) {
					throw new AssemblyException("The label name is empty");
				}else if(labelName.contains("¬") || labelName.contains("$") || labelName.contains("%") || labelName.contains("#") || labelName.contains(":")) {
					throw new AssemblyException("The label name "+labelName+" is invalid, it cannot contain ¬, $, : or % characters");
				}
				String labelValue = headLine.split("=")[1].replaceAll("\\s","");
				if( !(labelValue.matches("\\$([0-9a-fA-F])+") || labelValue.matches("¬([0-1])+") || labelValue.matches("%([0-9])+")) ) {
					throw new AssemblyException("The value "+labelValue+" of the label "+labelName+" is invalid");
				}
				constantLabels.put(labelName, labelValue);
			}
		}
		//Now we need to process the .code section this section is divided into more sections, one mandatory .main section, one optional .interrupt section, and 0 or more .org sections
		
		//First lets find the .main section
		String mainSection = "";
		if(codeSection.contains(".main")) {
			int startOfMainSection = codeSection.indexOf(".main")+6;
			int endOfMainSection = codeSection.indexOf(".endmain");
			if(endOfMainSection == -1) {
				throw new AssemblyException("The .main section is not properly closed with an .endmain");
			}
			mainSection = codeSection.substring(startOfMainSection,endOfMainSection);
			mainSection = mainSection.replaceAll("(?m)^\\s+", "");
		}else {
			throw new AssemblyException("The main code section is not present in the file");
		}
		
		System.out.println("Main section:"+mainSection);
		
		//Now lets find the .interrupt section
		String interruptSection = "";
		if(codeSection.contains(".interrupt")) {
			int startOfInterruptSection = codeSection.indexOf(".interrupt")+11;
			int endOfInterruptSection = codeSection.indexOf(".endinterrupt");
			if(endOfInterruptSection == -1) {
				throw new AssemblyException("The .interrupt section is not properly closed with an .endinterrupt");
			}
			interruptSection = codeSection.substring(startOfInterruptSection,endOfInterruptSection);
			interruptSection = interruptSection.replaceAll("(?m)^\\s+", "");
		}
		
		System.out.println("Interrupt section:"+interruptSection);
		
		LinkedList<String[]> orgSections = new LinkedList<String[]>();
		if(codeSection.contains(".org")) {
			for (int startIndexOfOrgSection:StringUtils.getIndexesOfMatches(codeSection,".org")) {
				String orgStartLine = codeSection.substring(startIndexOfOrgSection,codeSection.indexOf("\n",startIndexOfOrgSection)+1);//I added the +1 as a little hack to preser the \n character
				String orgSectionLocation = "";
				if(orgStartLine.contains("$")) {
					orgSectionLocation = orgStartLine.substring(orgStartLine.indexOf("$"),orgStartLine.indexOf("\n",orgStartLine.indexOf("$"))); //Maybe later we can add a token like : at the end of the section declaration to rely less on the new line 
				}else if (orgStartLine.contains("¬")) {
					orgSectionLocation = orgStartLine.substring(orgStartLine.indexOf("¬"),orgStartLine.indexOf("\n",orgStartLine.indexOf("¬")));
				}else if (orgStartLine.contains("%")) {
					orgSectionLocation = orgStartLine.substring(orgStartLine.indexOf("%"),orgStartLine.indexOf("\n",orgStartLine.indexOf("%")));
				}
				orgSectionLocation = orgSectionLocation.trim();
				if(orgSectionLocation.trim().isEmpty()) {
					throw new AssemblyException("The assembler cannot find the location of the .org section declared in the line: "+orgStartLine+" please meke sure that the location is provided.");
				}
				int startOfOrgSection= startIndexOfOrgSection+5+orgSectionLocation.length();
				int endOfOrgSection = codeSection.indexOf(".endorg",startOfOrgSection);
				String orgSection = codeSection.substring(startOfOrgSection, endOfOrgSection);
				orgSection=orgSection.replaceAll("(?m)^\\s+", "");
				orgSections.add(new String[] {orgSection,orgSectionLocation});
				System.out.println("Org section location:"+orgSectionLocation+"Content:"+orgSection);
			}
		}
		
		
		//At this point we have every section of the code correctly grouped, now we create the machine code for all instructions in all sections, except for the code labels
		HashMap<String,Integer> codeLabels = new HashMap<String,Integer>();
		
		LinkedList<Object[]> machineCodeWithoutReplacingCodeLabelsForAllSections = new LinkedList<Object[]>();
		
		LinkedList<String> machineCodeWithoutReplacingCodeLabelsForMainSection = new LinkedList<String>();
		
		//First we start with the main section
		
		int mainSectionMemoryAddress = 0;
		int mainSectionLine = 1;
		for(String line:mainSection.split("\n")) {
			//We have 2 options, the line is a CODELABEL or the line is an instruction
			line = line.trim();
			if(mainSectionMemoryAddress<256) {
				if (line.endsWith(":")) {
					//Is a label
					String labelName = line.substring(0,line.indexOf(":")).trim();
					if(labelName.isEmpty()) {
						throw new AssemblyException("The label name at line "+mainSectionLine+" of main section is empty");
					}else if(labelName.contains("¬") || labelName.contains("$") || labelName.contains("%") || labelName.contains("#")) {
						throw new AssemblyException("The label name "+labelName+" at line "+mainSectionLine+" of main section is invalid, it cannot contain ¬, $ or % characters");
					}else {
						if(constantLabels.containsKey(labelName)) {
							throw new AssemblyException("The label: "+labelName+" at line "+mainSectionLine+" of main section is already defined as a constant label");
						}else if(codeLabels.containsKey(labelName)) {
							throw new AssemblyException("The label: "+labelName+" at line "+mainSectionLine+" of main section is already defined as a code label");
						}else {
							codeLabels.put(labelName, mainSectionMemoryAddress);
						}
					}
				}else{
					String instructionMnemonic   = line.split(" ")[0].trim();
					String instructionParameter = line.split(" ")[1].trim();
					if(constantLabels.containsKey(instructionParameter)) {
						//The parameter of the instruction is a constant label, we replace it
						instructionParameter = constantLabels.get(instructionParameter).trim();
					}
					try {
						LinkedList<String> instructionBinaryRepresentation = getnstructionBinaryRepresentationAsStringList(instructionMnemonic, instructionParameter);
						machineCodeWithoutReplacingCodeLabelsForMainSection.addAll(instructionBinaryRepresentation);
						mainSectionMemoryAddress+=instructionBinaryRepresentation.size();
					}catch(AssemblyException ex) {
						throw new AssemblyException("Error at line "+mainSectionLine+" of main section: "+ex.toString());
					}	
				}
			}else {
				throw new AssemblyException("The main section occupies more than 255 (0 to 255) memory addresses, you can reduce it by jumping to another memory section outside the main one and continuing the execution from that point");
			}
			mainSectionLine++;
		}
		machineCodeWithoutReplacingCodeLabelsForAllSections.add(new Object[] {machineCodeWithoutReplacingCodeLabelsForMainSection,0,machineCodeWithoutReplacingCodeLabelsForMainSection.size()-1});
		
		//Then we continue with the Interrupt vector section
		
		LinkedList<String> machineCodeWithoutReplacingCodeLabelsForInterruptVectorSection = new LinkedList<String>();
		
		int interruptVectorSectionMemoryAddress = 256;
		int interruptVectorSectionLine = 1;
		for(String line:interruptSection.split("\n")) {
			//We have 2 options, the line is a CODELABEL or the line is an instruction
			line = line.trim();
			if(interruptVectorSectionMemoryAddress<1024) {
				if (line.endsWith(":")) {
					//Is a label
					String labelName = line.substring(0,line.indexOf(":")).trim();
					if(labelName.isEmpty()) {
						throw new AssemblyException("The label name at line "+interruptVectorSectionLine+" of interrupt vector section is empty");
					}else if(labelName.contains("¬") || labelName.contains("$") || labelName.contains("%") || labelName.contains("#")) {
						throw new AssemblyException("The label name "+labelName+" at line "+interruptVectorSectionLine+" of interrupt vector section is invalid, it cannot contain ¬, $ or % characters");
					}else {
						if(constantLabels.containsKey(labelName)) {
							throw new AssemblyException("The label: "+labelName+" at line "+interruptVectorSectionLine+" of interrupt vector section is already defined as a constant label");
						}else if(codeLabels.containsKey(labelName)) {
							throw new AssemblyException("The label: "+labelName+" at line "+interruptVectorSectionLine+" of interrupt vector section is already defined as a code label");
						}else {
							codeLabels.put(labelName, interruptVectorSectionMemoryAddress);
						}
					}
				}else{
					String instructionMnemonic   = line.split(" ")[0].trim();
					String instructionParameter = line.split(" ")[1].trim();
					if(constantLabels.containsKey(instructionParameter)) {
						//The parameter of the instruction is a constant label, we replace it
						instructionParameter = constantLabels.get(instructionParameter).trim();
					}
					try {
						LinkedList<String> instructionBinaryRepresentation = getnstructionBinaryRepresentationAsStringList(instructionMnemonic, instructionParameter);
						machineCodeWithoutReplacingCodeLabelsForInterruptVectorSection.addAll(instructionBinaryRepresentation);
						interruptVectorSectionMemoryAddress+=instructionBinaryRepresentation.size();
					}catch(AssemblyException ex) {
						throw new AssemblyException("Error at line "+interruptVectorSectionLine+" of interrupt vector section: "+ex.toString());
					}	
				}
			}else {
				throw new AssemblyException("The interrupt vector section occupies more than 768 (256 - 1023) memory addresses, you can reduce it by jumping to another memory section outside the main one and continuing the execution from that point");
			}
			interruptVectorSectionLine++;
		}
		
		machineCodeWithoutReplacingCodeLabelsForAllSections.add(new Object[] {machineCodeWithoutReplacingCodeLabelsForInterruptVectorSection,256,machineCodeWithoutReplacingCodeLabelsForInterruptVectorSection.size()-1});

		
		for(String[] orgSection:orgSections) {
			String orgSectionLocation = orgSection[1];
			LinkedList<String> machineCodeWithoutReplacingCodeLabelsForCurrentOrgSection = new LinkedList<String>();
			if(parameterNumberIsRepresentableWithAddressBusLength(orgSectionLocation)) {
				int currentOrgSectionMemoryAddress = convertParameterNumberValueToDecimal(orgSectionLocation);
				int currentOrgSectionLine = 1;
				for(String line:orgSection[0].split("\n")) {
					//We have 2 options, the line is a CODELABEL or the line is an instruction
					if(currentOrgSectionMemoryAddress<Math.pow(2,addressBusLength)-1) {
						line = line.trim();
						if (line.endsWith(":")) {
							//Is a label
							String labelName = line.substring(0,line.indexOf(":")).trim();
							if(labelName.isEmpty()) {
								throw new AssemblyException("The label name at line "+currentOrgSectionLine+" of org section located at "+orgSectionLocation+" is empty");
							}else if(labelName.contains("¬") || labelName.contains("$") || labelName.contains("%") || labelName.contains("#")) {
								throw new AssemblyException("The label name "+labelName+" at line "+currentOrgSectionLine+" of org section located at "+orgSectionLocation+" is invalid, it cannot contain ¬, $ or % characters");
							}else {
								if(constantLabels.containsKey(labelName)) {
									throw new AssemblyException("The label: "+labelName+" at line "+currentOrgSectionLine+" of org section located at "+orgSectionLocation+" is already defined as a constant label");
								}else if(codeLabels.containsKey(labelName)) {
									throw new AssemblyException("The label: "+labelName+" at line "+currentOrgSectionLine+" of org section located at "+orgSectionLocation+" is already defined as a code label");
								}else {
									codeLabels.put(labelName, currentOrgSectionMemoryAddress);
								}
							}
						}else{
							String instructionMnemonic   = line.split(" ")[0].trim();
							String instructionParameter = line.split(" ")[1].trim();
							if(constantLabels.containsKey(instructionParameter)) {
								//The parameter of the instruction is a constant label, we replace it
								instructionParameter = constantLabels.get(instructionParameter).trim();
							}
							try {
								LinkedList<String> instructionBinaryRepresentation = getnstructionBinaryRepresentationAsStringList(instructionMnemonic, instructionParameter);
								machineCodeWithoutReplacingCodeLabelsForCurrentOrgSection.addAll(instructionBinaryRepresentation);
								currentOrgSectionMemoryAddress+=instructionBinaryRepresentation.size();
							}catch(AssemblyException ex) {
								throw new AssemblyException("Error at line "+currentOrgSectionLine+" of org section located at "+orgSectionLocation+": "+ex.toString());
							}	
						}
						currentOrgSectionLine++;
					}else {
						throw new AssemblyException("The org section goes outside of memory");
					}
				}
				machineCodeWithoutReplacingCodeLabelsForAllSections.add(new Object[] {machineCodeWithoutReplacingCodeLabelsForCurrentOrgSection,convertParameterNumberValueToDecimal(orgSectionLocation),machineCodeWithoutReplacingCodeLabelsForCurrentOrgSection.size()-1});
			}else {
				throw new AssemblyException("The org section location "+orgSectionLocation+" is invalid");
			}
		}
		
		//At this point we already populated the machineCodeWithoutReplacingCodeLabelsForAllSections list, now its time to construct the binary representation
		//First we need to make sure that the sections doesn't repeat or overlap
		//For this first we make sure that the .org sections starting point are not in the region of the .main and the .interrupt sections
		for(int i = 2;i<machineCodeWithoutReplacingCodeLabelsForAllSections.size();i++) {
			Object[] sectionAInfo = machineCodeWithoutReplacingCodeLabelsForAllSections.get(i);
			int sectionAStart = (Integer) sectionAInfo[1];
			if(sectionAStart<=255) {
				throw new AssemblyException("The .org section number "+(i-1)+" overlaps with the .main section");
			}
			if(sectionAStart>=256 && sectionAStart<=1023) {
				throw new AssemblyException("The .org section number "+(i-1)+" overlaps with the .interrupt section");
			}
		}
		//Now we need to make sure that the .org sections doesn't overlap in between them
		for(int i = 2;i<machineCodeWithoutReplacingCodeLabelsForAllSections.size();i++) {
			Object[] sectionAInfo = machineCodeWithoutReplacingCodeLabelsForAllSections.get(i);
			int sectionAStart = (Integer) sectionAInfo[1];
			for(int j = 2;j<machineCodeWithoutReplacingCodeLabelsForAllSections.size();j++) {
				Object[] sectionBInfo = machineCodeWithoutReplacingCodeLabelsForAllSections.get(j);
				int sectionBStart = (Integer) sectionBInfo[1];
				int sectionBEnd = (Integer) sectionBInfo[2];
				if(sectionAStart>=sectionBStart && sectionAStart<=sectionBEnd) {
					throw new AssemblyException("The .org section number "+(i-1)+" overlaps with the .org section number "+(j+1)+" section");
				}
			}
		}
		
		//At this point we know that the sections doesn't overlap, its time to replace all 
		//the code tags with the address values and fill the 
		//empty spaces with 0 but first we need to order the sections
	
		machineCodeWithoutReplacingCodeLabelsForAllSections.sort(new Comparator<Object[]>() {
			@Override
			public int compare(Object[] o1, Object[] o2) {
				if(((Integer)o1[1])>((Integer)o2[1])) {
					return 1;
				}else if(((Integer)o1[1])<((Integer)o2[1])) {
					return -1;
				}else {
					return 0;
				}
			}
		});
		String valueCeroAsBinaryString = convertNumberToBinaryStringOfDataBusSize(0);
		for(int j = 0;j<machineCodeWithoutReplacingCodeLabelsForAllSections.size();j++) {
			Object[] sectionInfo = machineCodeWithoutReplacingCodeLabelsForAllSections.get(j);
			if((j+1)<machineCodeWithoutReplacingCodeLabelsForAllSections.size()) {
				int startOfNextSection = (Integer) machineCodeWithoutReplacingCodeLabelsForAllSections.get(j+1)[1];
				//Intermediate section
				for(int i = (Integer)sectionInfo[1];i<startOfNextSection;i++ ) {
					LinkedList<String> sectionBinaryMachineCode = (LinkedList<String>) sectionInfo[0];
					if(i<sectionBinaryMachineCode.size()) {
						if(!sectionBinaryMachineCode.get(i).matches("([0-1])+")) {
							//Is a label, we need to replace it 
							if(codeLabels.containsKey(sectionBinaryMachineCode.get(i).trim())) {
								int addressOfLabel = codeLabels.get(sectionBinaryMachineCode.get(i).trim());
								String[] addressOfLabelAsBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(addressOfLabel);
								binaryMachineCodeStrings.add(addressOfLabelAsBinaryStrings[0]);
								binaryMachineCodeStrings.add(addressOfLabelAsBinaryStrings[1]);
								i++;
							}else {
								throw new AssemblyException("The code label: "+sectionBinaryMachineCode.get(i)+" dosen't exists");
							}
						}else {
							binaryMachineCodeStrings.add(sectionBinaryMachineCode.get(i));
						}
					}else {
						binaryMachineCodeStrings.add(valueCeroAsBinaryString);
					}
				}
			}else {
				//Last section
				for(int i = (Integer)sectionInfo[1];i<(Integer)sectionInfo[2];i++ ) {
					LinkedList<String> sectionBinaryMachineCode = (LinkedList<String>) sectionInfo[0];
					if(i<sectionBinaryMachineCode.size()) {
						if(!sectionBinaryMachineCode.get(i).matches("([0-1])+")) {
							//Is a label, we need to replace it 
							if(codeLabels.containsKey(sectionBinaryMachineCode.get(i).trim())) {
								int addressOfLabel = codeLabels.get(sectionBinaryMachineCode.get(i).trim());
								String[] addressOfLabelAsBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(addressOfLabel);
								binaryMachineCodeStrings.add(addressOfLabelAsBinaryStrings[0]);
								binaryMachineCodeStrings.add(addressOfLabelAsBinaryStrings[1]);
								i++;
							}else {
								throw new AssemblyException("The code label: "+sectionBinaryMachineCode.get(i)+" dosen't exists");
							}
						}else {
							binaryMachineCodeStrings.add(sectionBinaryMachineCode.get(i));
						}
					}else {
						binaryMachineCodeStrings.add(valueCeroAsBinaryString);
					}
				}
			}
		}
	}
	
	public void outputAssembledFile(String outputFileName,OutputFileFormat outputFileFormat) throws AssemblyException, IOException{
		outputFileFormat.outputFormatedFile(outputFileName,binaryMachineCodeStrings);
	}
	
	private LinkedList<String> getnstructionBinaryRepresentationAsStringList(String instructionMnemonic,String instructionParameter) throws AssemblyException {
		switch(instructionMnemonic) {
			case "ADC":
				if(instructionParameter.contains("#")) {
					//Direct
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(1)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(2)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(3)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}
			case "SUB":
				if(instructionParameter.contains("#")) {
					//Direct
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(4)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(5)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(6)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}
			case "INC":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(1)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "OR":
				if(instructionParameter.contains("#")) {
					//Direct
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(8)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(9)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(10)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}
			case "AND":
				if(instructionParameter.contains("#")) {
					//Direct
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(11)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(12)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(13)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}
			case "NOT":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(14)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "XOR":
				if(instructionParameter.contains("#")) {
					//Direct
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(15)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(16)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(17)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}
			case "RTCL":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(18)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "RTCR":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(19)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "DEC":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(20)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "CLCF":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(21)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "CLZF":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(22)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "CLNF":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(23)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "CLOF":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(24)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "CLIDF":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(25)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "SCF":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(26)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "SZF":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(27)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "SNF":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(28)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "SOF":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(29)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "SIDF":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(30)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "LDA":
				if(instructionParameter.contains("#")) {
					//Direct
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(31)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(32)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(33)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}
			case "LDX":
				if(instructionParameter.contains("#")) {
					//Direct
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(34)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(35)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(36)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}
			case "LDY":
				if(instructionParameter.contains("#")) {
					//Direct
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(37)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(38)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(39)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}
			case "STA":
				if(instructionParameter.contains("#")) {
					throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" indicates a direct adressing mode but this instruction does not support it");
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(40)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(41)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}
			case "STX":
				if(instructionParameter.contains("#")) {
					throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" indicates a direct adressing mode but this instruction does not support it");
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(42)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(43)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}
			case "STY":
				if(instructionParameter.contains("#")) {
					throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" indicates a direct adressing mode but this instruction does not support it");
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(44)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(45)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" is invalid");
					}
				}
			case "TAX":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(46)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "TAY":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(47)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "TXA":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(48)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
				
			case "TXY":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(49)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "TYA":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(50)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "TYX":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(51)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "PSHA":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(52)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "PSHS":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(53)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "PSHX":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(54)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "PSHY":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(55)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "POPA":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(56)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "POPS":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(57)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "POPX":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(58)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "POPY":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(59)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "NOP":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(60)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "JMP":
				if(instructionParameter.contains("#")) {
					throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" indicates a direct adressing mode but this instruction does not support it");
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(61)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(62)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						//Is a code label, we reserve 2 memory addresses for it, as absolute addressing
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(62)); //Instruction binary representation
						instructionBinaryRepresentation.add(instructionParameter);
						instructionBinaryRepresentation.add(instructionParameter);
						return instructionBinaryRepresentation;
					}
				}
			case "JSR":
				if(instructionParameter.contains("#")) {
					throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" indicates a direct adressing mode but this instruction does not support it");
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(63)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(0));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(63)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						//Is a code label, we reserve 2 memory addresses for it, as absolute addressing
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(63)); //Instruction binary representation
						instructionBinaryRepresentation.add(instructionParameter);
						instructionBinaryRepresentation.add(instructionParameter);
						return instructionBinaryRepresentation;
					}
				}
			case "RETS":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(64)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			case "BCC":
				if(instructionParameter.contains("#")) {
					throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" indicates a direct adressing mode but this instruction does not support it");
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(65)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(66)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						//Is a code label, we reserve 2 memory addresses for it, as absolute addressing
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(66)); //Instruction binary representation
						instructionBinaryRepresentation.add(instructionParameter);
						instructionBinaryRepresentation.add(instructionParameter);
						return instructionBinaryRepresentation;
					}
				}
			case "BCS":
				if(instructionParameter.contains("#")) {
					throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" indicates a direct adressing mode but this instruction does not support it");
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(67)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(68)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						//Is a code label, we reserve 2 memory addresses for it, as absolute addressing
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(68)); //Instruction binary representation
						instructionBinaryRepresentation.add(instructionParameter);
						instructionBinaryRepresentation.add(instructionParameter);
						return instructionBinaryRepresentation;
					}
				}
			case "BZC":
				if(instructionParameter.contains("#")) {
					throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" indicates a direct adressing mode but this instruction does not support it");
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(69)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(70)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						//Is a code label, we reserve 2 memory addresses for it, as absolute addressing
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(70)); //Instruction binary representation
						instructionBinaryRepresentation.add(instructionParameter);
						instructionBinaryRepresentation.add(instructionParameter);
						return instructionBinaryRepresentation;
					}
				}
			case "BZS":
				if(instructionParameter.contains("#")) {
					throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" indicates a direct adressing mode but this instruction does not support it");
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(71)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(72)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						//Is a code label, we reserve 2 memory addresses for it, as absolute addressing
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(72)); //Instruction binary representation
						instructionBinaryRepresentation.add(instructionParameter);
						instructionBinaryRepresentation.add(instructionParameter);
						return instructionBinaryRepresentation;
					}
				}
			case "BNC":
				if(instructionParameter.contains("#")) {
					throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" indicates a direct adressing mode but this instruction does not support it");
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(73)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(74)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						//Is a code label, we reserve 2 memory addresses for it, as absolute addressing
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(74)); //Instruction binary representation
						instructionBinaryRepresentation.add(instructionParameter);
						instructionBinaryRepresentation.add(instructionParameter);
						return instructionBinaryRepresentation;
					}
				}
			case "BNS":
				if(instructionParameter.contains("#")) {
					throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" indicates a direct adressing mode but this instruction does not support it");
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(75)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(76)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						//Is a code label, we reserve 2 memory addresses for it, as absolute addressing
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(76)); //Instruction binary representation
						instructionBinaryRepresentation.add(instructionParameter);
						instructionBinaryRepresentation.add(instructionParameter);
						return instructionBinaryRepresentation;
					}
				}
			case "BOC":
				if(instructionParameter.contains("#")) {
					throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" indicates a direct adressing mode but this instruction does not support it");
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(77)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(78)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						//Is a code label, we reserve 2 memory addresses for it, as absolute addressing
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(78)); //Instruction binary representation
						instructionBinaryRepresentation.add(instructionParameter);
						instructionBinaryRepresentation.add(instructionParameter);
						return instructionBinaryRepresentation;
					}
				}

			case "BOS":
				if(instructionParameter.contains("#")) {
					throw new AssemblyException("The parameter of the instruction: "+instructionMnemonic+" "+instructionParameter+" indicates a direct adressing mode but this instruction does not support it");
				}else {
					//Adressing
					if(parameterNumberIsRepresentableWithDataBusLength(instructionParameter)) {
						//Zero page
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(79)); //Instruction binary representation
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter)));
						return instructionBinaryRepresentation;
					}else if (parameterNumberIsRepresentableWithAddressBusLength(instructionParameter)){
						//Absolute
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(80)); //Instruction binary representation
						String [] parameterBinaryStrings = convertNumberToTwoBinaryStringOfDataBusSize(convertParameterNumberValueToDecimal(instructionParameter));
						instructionBinaryRepresentation.add(parameterBinaryStrings[0]);
						instructionBinaryRepresentation.add(parameterBinaryStrings[1]);
						return instructionBinaryRepresentation;
					}else {
						//Is a code label, we reserve 2 memory addresses for it, as absolute addressing
						LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
						instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(80)); //Instruction binary representation
						instructionBinaryRepresentation.add(instructionParameter);
						instructionBinaryRepresentation.add(instructionParameter);
						return instructionBinaryRepresentation;
					}
				}
			case "RETI":
				if(instructionParameter.isEmpty()) {
					LinkedList<String> instructionBinaryRepresentation = new LinkedList<String>();
					instructionBinaryRepresentation.add(convertNumberToBinaryStringOfDataBusSize(81)); //Instruction binary representation
					return instructionBinaryRepresentation;
				}else {
					throw new AssemblyException("The instruction: "+instructionMnemonic+" cannot accept parameters, and the parameter: "+instructionParameter+" was found");
				}
			default:
				throw new AssemblyException("Invalid opcode: "+instructionMnemonic);
		}
	}
	
	
	/**
	 * Validates if the given number is representable with the number of bytes of the data bus
	 * @param number
	 * @return true if is representable and valid, false if not
	 */
	private boolean parameterNumberIsRepresentableWithDataBusLength(String number) {
		double maximumRepresentableNumber = Math.pow(2,dataBusLength)-1;
		if(number.matches("\\$([0-9a-fA-F])+")){
			//Hex number
			number = number.replace("$","");
			if(Integer.parseInt(number, 16)<=maximumRepresentableNumber) {
				return true;
			}
		}else if (number.matches("¬([0-1])+")) {
			 //Binary number
			number = number.replace("¬","");
			if(Integer.parseInt(number, 2)<=maximumRepresentableNumber) {
				return true;
			}
		}else if (number.matches("%([0-9])+")) {
			//Decimal number
			number = number.replace("%","");
			if(Integer.parseInt(number)<=maximumRepresentableNumber) {
				return true;
			}
			
		}
		return false;
	}
	
	/**
	 * Validates if the given number is representable with the number of bytes of the address bus
	 * @param number
	 * @return true if is representable and valid, false if not
	 */
	private boolean parameterNumberIsRepresentableWithAddressBusLength(String number) {
		double maximumRepresentableNumber = Math.pow(2,addressBusLength)-1;
		if(number.matches("\\$([0-9a-fA-F])+")){
			//Hex number
			number = number.replace("$","");
			if(Integer.parseInt(number, 16)<=maximumRepresentableNumber) {
				return true;
			}
		}else if (number.matches("¬([0-1])+")) {
			 //Binary number
			number = number.replace("¬","");
			if(Integer.parseInt(number, 2)<=maximumRepresentableNumber) {
				return true;
			}
		}else if (number.matches("%([0-9])+")) {
			//Decimal number
			number = number.replace("%","");
			if(Integer.parseInt(number)<=maximumRepresentableNumber) {
				return true;
			}
			
		}
		return false;
	}
	
	private int convertParameterNumberValueToDecimal(String number) {
		if(number.matches("\\$([0-9a-fA-F])+")){
			//Hex number
			number = number.replace("$","");
			return Integer.parseInt(number, 16);
		}else if (number.matches("¬([0-1])+")) {
			 //Binary number
			number = number.replace("¬","");
			return Integer.parseInt(number, 2);
		}else if (number.matches("%([0-9])+")) {
			//Decimal number
			number = number.replace("%","");
			return Integer.parseInt(number);
		}else {
			return 0;
		}
		
	}
	
	private String convertNumberToBinaryStringOfDataBusSize(int number) {
		String numberBinaryString = Integer.toBinaryString(number);
		while(numberBinaryString.length()<dataBusLength) {
			numberBinaryString="0"+numberBinaryString;
		}
		return numberBinaryString;
	}
	
	private String[] convertNumberToTwoBinaryStringOfDataBusSize(int number) {
		String numberBinaryString = Integer.toBinaryString(number);
		//11010101 01010111
		String lowPart = reverseString(numberBinaryString.substring(0,dataBusLength));//Low part
		String highPart = reverseString(numberBinaryString.substring(dataBusLength,numberBinaryString.length()));//High part
		while(highPart.length()<dataBusLength) {
			highPart="0"+highPart;
		}
		return new String[] {lowPart,highPart};//Little endian
	}
	
	private String reverseString(String string) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(string);
		return stringBuilder.reverse().toString();
	}
}
