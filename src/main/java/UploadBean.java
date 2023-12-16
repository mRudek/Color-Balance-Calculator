

// import javax.faces.bean.ManagedBean;
// import javax.faces.bean.SessionScoped;

// import java.io.Serializable;

// @ManagedBean
// @SessionScoped
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.servlet.http.Part;

import java.io.Serializable;

@ManagedBean
@ViewScoped
public class UploadBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	//--variables
	private Part file;
	private String fileContent;
	public String getFileContent() {
		return fileContent;
	}
	public void setFileContent(String name) {
		this.fileContent = name;
	}

	private String colorResultString;
	public String getColorResultString() {
		return colorResultString;
	}
	public void setColorResultString(String content) {
		this.colorResultString = content;
	}

	private String errorString;
	public String geterrorString() {
		return errorString;
	}
	public void seterrorString(String content) {
		this.errorString = content;
	}

	private String textbox2;
	public String getValuesBox2() {
		return textbox2;
	}

	private String[] dnaBaseValues = {"G", "T", "C", "A"};

	//-------------
	//Methoden
	//-------------

	/**
	 * Upload Button startet Auswertungsvorgang nach Einlesen der Datei
	 * @throws IOException
	 */
	public void upload() throws IOException {
		Scanner s = new Scanner(file.getInputStream());
		fileContent = s.useDelimiter("\\A").next();
		s.close();
		// System.out.println(fileContent); //for debug
		this.evaluateDNA(fileContent);
	}

	/**
	 * Hauptmethode fuer Auswertung, die Untermethoden aufruft 
	 */
	public void evaluateDNA(String fileData){
		this.errorString = "";
		//1. bestimme die laengste Zeile
		int maxrows = 0;
		maxrows = this.getMaxOutputCol(fileData);
		//2. erstelle ergebnisarray mit laenge langester Zeile
		String[] output = new String[maxrows]; 
		//3. tausche Zeilen und Spalten aus Eingabe + Formatiere fuer Ergebnisverarbeitung
		output = this.formatDataInput(output, fileData);
		//4. Ergebnisverarbeitung
		int[] redLights = new int[maxrows];
		int[] greenLights = new int[maxrows];
		for(int i = 0; i < output.length; i++){
			for(int j = 0; j < output[i].length(); j++){
			//Auswertung max rows
				switch(output[i].substring(j,j+1)){
					case "G":
						break;
					case "T":
						greenLights[i]++;
						break;
					case "C":
						redLights[i]++;
						break;
					case "A":
						redLights[i]++;
						greenLights[i]++;
						break;
					default:
						String error = "\n there was a unknown error " + output[i].substring(j,j+1);
						this.errorString += error;
						System.out.println(error);
						break;
				};
			}
		}
		//5. Output in App
		this.colorResultString = "- red / green \n" ;
		int row = 1;
		for(int i = 0; i < maxrows; i++){
			this.colorResultString = this.colorResultString + row + "." + " " + redLights[i] + " / " + greenLights[i] + "\n";
			System.out.println(row + " output: " + output[i]);
			row++;
		}
	}

	/**
	 * Liefert die hoechste Anzahl an DNA-Base in einer Zeile in der ganzen Datei 
	 */
	public int getMaxOutputCol(String fileData){
		int maxrows = 0;
		int countedrows = 0;
		String markedValue = "";
		String sequenzProcess = "end";
		//max rows
		for(int i = 0; i < fileData.length() ; i++){
			markedValue = fileData.substring(i, i+1);
			// System.out.println(i +" Schritt: " + markedValue + " max fileLengh: " + fileData.length()  ); //for debug
			sequenzProcess = "\n ungueltigen parameter gefunden: Stelle in Datei " + i + " Parameter " + markedValue ;
			//Werte eingelesene Zeichen aus:
			if(Arrays.asList(dnaBaseValues).contains(markedValue)){
			sequenzProcess = "processValue";
			}
			if(markedValue.equals(" ")){
			sequenzProcess = "next";
			}
			if(markedValue.equals(".")){
			sequenzProcess = "endLineData";
			}
			if( markedValue.matches("-?\\d+(\\.\\d+)?") ){ //bei zahl
				sequenzProcess = "endLineData";
			}
			//new Line ignorieren
			if(markedValue.charAt(0) == 10){
				sequenzProcess = "next";
			}
			if(markedValue.charAt(0) == 13){
				sequenzProcess = "next";
			}
			//Auswertung max rows
			switch(sequenzProcess){
				case "next":
					// System.out.println(i + " read next"); //for debug
					break;
				case "endLineData":
					if(countedrows > maxrows){
						maxrows = countedrows;
					}
					countedrows = 0;
					// System.out.println("---------endLineData-------max cols: " + maxrows + " old count: " + countedrows); //for debug
					break;
				case "processValue":
					countedrows++;
					break;
				default:
					String error = sequenzProcess;
					this.errorString += error;
					System.out.println("error: " + error);
					break;
			};
			//Ende der Datei muss nochmals geprueft werden, da kein . vorhanden
			if(i == fileData.length()){ 
				if(countedrows > maxrows){
					maxrows = countedrows;
				}
			}
			if(i == fileData.length()-1 ){ 
				if(countedrows > maxrows){
					maxrows = countedrows;
				}
			}
		}
		return maxrows;
	}

	/**
	 * Formatiert String zur weiteren Verarbeitung:
	 * Zeilen und Spalten vertauschen + entfernen nicht benoetigter Zeichen
	 * Als Rueckgabe erhaelt man ein Array z. B. output[0] ist "GAC"
	 */		
	public String[] formatDataInput(String[] output, String fileData){
		String markedValue = "";
		String sequenzProcess = "end";
		int dataNo = 0;
		for(int i = 0; i < fileData.length() ; i++){
			markedValue = fileData.substring(i, i+1);
			// System.out.println(i +" Schritt: " + markedValue + " max fileLengh: " + fileData.length()  ); //for debug
			sequenzProcess = "\n ungueltigen parameter gefunden: Stelle in Datei " + i + " Parameter " + markedValue ;
			//Werte eingelesene Zeichen aus:
			if(Arrays.asList(dnaBaseValues).contains(markedValue)){
			sequenzProcess = "processValue";
			}
			if(markedValue.equals(" ")){
			sequenzProcess = "next";
			}
			if(markedValue.equals(".")){
			sequenzProcess = "endLineData";
			}
			if( markedValue.matches("-?\\d+(\\.\\d+)?") ){ //bei zahl
				sequenzProcess = "endLineData";
			}
			//new Line ignorieren
			if(markedValue.charAt(0) == 10){
				sequenzProcess = "next";
			}
			if(markedValue.charAt(0) == 13){
				sequenzProcess = "next";
			}
			//Auswertung max rows
			switch(sequenzProcess){
				case "next":
					// System.out.println(i + " read next"); //for debug
					break;
				case "endLineData":
					dataNo = 0;
					// System.out.println("---------endLineData-------max cols: " + maxrows + " old count: " + countedrows); //for debug
					break;
				case "processValue":
					if(Objects.isNull(output[dataNo])){
						output[dataNo] = markedValue;
					} else {
						output[dataNo] = output[dataNo] + markedValue;
					}
					dataNo++;
					break;
				default:
					String error = sequenzProcess;
					this.errorString += error;
					System.out.println("error: " + error);
					break;
			};
		}
		return output;
	}

	public void validate(FacesContext context, UIComponent component, Object value) {
		Part file = (Part) value;
		if (file.getSize() > 111111111) {
			throw new ValidatorException(new FacesMessage("File is too large"));
		}
		if (!file.getContentType().equals("text/plain")) 
			throw new ValidatorException(new FacesMessage("File is not a text file"));
	}
	
	public Part getFile() {
		return file;
	}

	public void setFile(Part file) {
		this.file = file;
	}

}