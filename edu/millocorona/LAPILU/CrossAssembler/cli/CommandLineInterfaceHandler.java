package edu.millocorona.LAPILU.CrossAssembler.cli;

import java.io.IOException;
import java.util.HashMap;

import edu.millocorona.LAPILU.CrossAssembler.LAPILUCrossAssembler;
import edu.millocorona.LAPILU.CrossAssembler.exceptions.AssemblyException;
import edu.millocorona.LAPILU.CrossAssembler.outputFiileFormats.BinOutputFileFormat;
import edu.millocorona.LAPILU.CrossAssembler.outputFiileFormats.CoeOutputFileFormat;
import edu.millocorona.LAPILU.CrossAssembler.outputFiileFormats.MifOutputFileFormat;
import edu.millocorona.LAPILU.CrossAssembler.outputFiileFormats.OutputFileFormat;


public class CommandLineInterfaceHandler {

	public static final float LAPILU_CROSS_ASSEMBLER_VERSION = 0.1f;
	
	public static void main(String[] args) {
		new CommandLineInterfaceHandler().interpretCommandLineArguments(args);
	}
	
	/**
	 * The purpose of this method is to interpret the CLI arguments and start the execution of the command given by the user
	 * @param arguments - the arguments specified by the user in the command line
	 */
	public void interpretCommandLineArguments(String [] arguments) {
		if(arguments.length>1) {
			if("help".equals(arguments[0])) {
				if(arguments.length == 1) {
					showHelp();
				}else {
					if("assemble".equals(arguments[1])) {
						showAssembleHelp();
					}else if("help".equals(arguments[1])) {
						showHelpHelp();
					}else {
						showNonExistentCommand(arguments[1]);
					}
				}
			}else if("assemble".equals(arguments[0])) {
				if(arguments.length>=11) {
					executeAssembleCommand(arguments);
				}else {
					showAssembleUsage();
				}
			}
		}else {
			showUsage();
		}
	}
	
	private void showUsage() {
		System.out.println();
		System.out.println("Usage:");
		System.out.println();
		System.out.println("	LPCA	<command>	[arguments]");
		System.out.println();
		System.out.println("For help type:");
		System.out.println();
		System.out.println("	LPCA	help");
		System.out.println();
	}
	
	private void showHelp() {
		System.out.println();
		System.out.println("LAPILU Cross-assembler Version "+LAPILU_CROSS_ASSEMBLER_VERSION);
		System.out.println();
		System.out.println("The porpouse of this program is to convert LAPILU Assembly code to LAPILU machine code");
		System.out.println();
		System.out.println("Usage:");
		System.out.println();
		System.out.println("	LPCA	<command>	[arguments]");
		System.out.println();
		System.out.println("The available commands are: ");
		System.out.println();
		System.out.println("	assemble	Assembles a source code file.");
		System.out.println("	help		Displays this help message.");
		System.out.println();
		System.out.println("Use \"LPCA help <command>\" for more information about a command.");
		System.out.println();
	}
	
	private void showAssembleUsage() {
		System.out.println("Usage:");
		System.out.println();
		System.out.println("	LPCA	assemble	[arguments]");
		System.out.println();
	}
	
	private void showAssembleHelp() {
		System.out.println();
		System.out.println("The porpouse of this command is to convert a file with LAPILU assembly code and convert it to LAPILU machine code");
		System.out.println();
		System.out.println("The available arguments are:");
		System.out.println();
		System.out.println("	Argument format							Optional?		Description");
		System.out.println();
		System.out.println("	-dbl [LAPILU_DATA_BUS_LENGTH]			No				Sets the DATA_BUS_LENGTH that your CPU config is using");
		System.out.println("	-abl [LAPILU_ADDRESS_BUS_LENGTH]		No				Sets the ADDRESS_BUS_LENGTH that your CPU config is using");
		System.out.println("	-if	 [INPUT_FILE_NAME].lpasm			No				Sets the input file name");
		System.out.println("	-of	 [OUTPUT_FILE_NAME]					No				Sets the output file name");
		System.out.println("	-off [OUTPUT_FORMAT]					No				Sets the output file format, the available formats are:");
		System.out.println("																bin (.bin) Simple binary file");
		System.out.println("																coe (.coe) Xilinx COE file");
		System.out.println("																mif (.mif) Memory initialization file");
		System.out.println();
		System.out.println("Example for a LAPILU CPU configured with an 8 bit data bus and a 16 bit address bus: ");
		System.out.println();
		System.out.println("LPCA assemble -dbl 8 -abl 16 -if my_assembly_file.lpasm -of my_output -off coe");
		System.out.println();
	}
	
	private void showHelpHelp() {
		System.out.println();
		System.out.println("You need help for the help? mmm... I dont know what to do, maybe call a Jedi?");
		System.out.println();
	}
	
	private void showNonExistentCommand(String nonExistentCommand) {
		System.out.println();
		System.out.println("The command: "+nonExistentCommand+" does not exist");
		System.out.println();
	}
	/**
	 * The purpouse of this method is to start the Assembly of the specified file 
	 * @param arguments
	 */
	private void executeAssembleCommand(String[] arguments) {
		//We need to parse the arguments, we put them in to a hash map, so the order doesn't matter
		HashMap<String,String> assembleCommandArguments = new HashMap<String,String>();
		for(int i = 1;i<arguments.length;i+=2) {
			assembleCommandArguments.put(arguments[i].trim(),arguments[i+1].trim());
		}
		LAPILUCrossAssembler lapiluCrossAssembler = new LAPILUCrossAssembler(Short.parseShort(assembleCommandArguments.get("-dbl")),Short.parseShort(assembleCommandArguments.get("-abl")),assembleCommandArguments.get("-if"));
		try {
			lapiluCrossAssembler.assembleFile();
			OutputFileFormat outputFileFormat = null;
			if("bin".equals(assembleCommandArguments.get("-off"))) {
				outputFileFormat = new BinOutputFileFormat();
			}else if("coe".equals(assembleCommandArguments.get("-off"))) {
				outputFileFormat = new CoeOutputFileFormat();
			}else if("mif".equals(assembleCommandArguments.get("-off"))) {
				outputFileFormat = new MifOutputFileFormat();
			}
			lapiluCrossAssembler.outputAssembledFile(assembleCommandArguments.get("-of"),outputFileFormat);
		} catch (AssemblyException e) {
			System.err.println("Assembly error: "+e.toString());
		} catch (IOException e) {
			System.err.println("Internal error: ");
			e.printStackTrace();
		}
	}
}
