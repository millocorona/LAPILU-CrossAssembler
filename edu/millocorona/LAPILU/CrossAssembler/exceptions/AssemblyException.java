package edu.millocorona.LAPILU.CrossAssembler.exceptions;

public class AssemblyException extends Exception{

	private static final long serialVersionUID = 3296672970417325524L;
	private String message;
	
	public AssemblyException (String message) {
		this.message = message;
	}
	
	@Override
	public String toString() {
		return message;
	}
	
}
