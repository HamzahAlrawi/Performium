package com.AnalyzeThis;


import org.testng.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Properties;

public class ReadWriteHelper {
    /**
     * @param fileName    CSV File name
     * @param linesToRead Numbers of lines to read.
     */
    public static String[][] readCSVFile(String fileName, int linesToRead) {
        //Possible future implementation: Make separators as input. However this looks better for reusability
        String line = "";
        String csvSplitBy = ",";
        String[] currentLine = null;
        String[][] finalResult = new String[linesToRead][1];
        try (BufferedReader br = new BufferedReader(new FileReader(System.getProperty( "user.dir" ) +
                "/src/main/resources/DataProvider/"+fileName+".csv"))) {
            int j = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                currentLine = line.split(csvSplitBy);
                for (int i = 0; i < currentLine.length; i++) {
                    finalResult[j][i] = currentLine[i];
                }
                j++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return finalResult;
    }

    public static void writeCSVFile(String filePath, String[] data) throws IOException {
        //Possible future implementation: Make separators as input. However this looks better for reusability
        String csvSplitBy = ",";
        BufferedWriter br = new BufferedWriter(new FileWriter(System.getProperty( "user.dir" ) +
                "/src/main/resources/DataProvider/"+filePath+".csv"));
        StringBuilder sb = new StringBuilder();
        int len = data.length;
        for (int i = 0 ; i < len; i++){
            sb.append(data[i]);
            sb.append("\n");
        }
        br.write(sb.toString());
        br.close();


    }
}