package com.mikhailkazhamiaka.viatickets;

import android.content.Context;
import android.util.Log;


import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;


/**
 * Created by mikhailkazhamiaka on 2016-02-21.
 */
public class PDFParse {

    public static Ticket parsePDF(String pdfFile, Context context) {
        PDFParser parser = null;
        PDDocument pdDoc = null;
        COSDocument cosDoc = null;
        PDFTextStripper pdfStripper;

        String parsedText = "";
        File file = new File(context.getFilesDir() + "/" + pdfFile);
        assert(file.exists());
        try {
            RandomAccessBufferedFileInputStream ra = new RandomAccessBufferedFileInputStream(file);
            parser = new PDFParser(ra);
            parser.parse();
            cosDoc = parser.getDocument();
            pdfStripper = new PDFTextStripper();
            pdDoc = new PDDocument(cosDoc);
            parsedText = pdfStripper.getText(pdDoc);
            pdDoc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String origin = null;
        String destination = null;
        String date = null;
        String time = null;
        String trainNumber = null;
        String car = null;
        String seat = null;

        String splitText[] = parsedText.split("\n");
        String previousLine = null;
        boolean carInNextLine = false;
        boolean seatInNextLine = false;
        boolean trainNumberInNextLine = false;

        for (String line : splitText) {
            if (carInNextLine) {
                carInNextLine = false;
                car = line.trim();
                continue;
            } else if (seatInNextLine) {
                seatInNextLine = false;
                seat = line.trim();
                continue;
            } else if (trainNumberInNextLine) {
                trainNumberInNextLine = false;
                trainNumber = line.trim();
                continue;
            }

            if (line.contains("Date :")) {
                if (date == null) date = line.split(":")[1].trim();
                // can infer that the origin/destination was just before
                if (origin == null) origin = previousLine.trim();
                else destination = previousLine.trim();
            } else if (line.contains("Departure :") && time == null) {
                String timeSplit[] = line.split(":");
                time = (timeSplit[1] + ":" + timeSplit[2]).replace("AM", "").replace("PM", "");
            } else if (line.contains("Car")) {
                carInNextLine = true;
            } else if (line.contains("Seat")) {
                seatInNextLine = true;
            } else if (line.contains("Train") && line.contains("#")) {
                trainNumberInNextLine = true;
            }

            previousLine = line;
        }

        assert(origin != null && destination != null && date != null && time != null
                && trainNumber != null && car != null && seat != null);

        Ticket ticket = new Ticket();
        ticket.setOrigin(origin);
        ticket.setDestination(destination);
        ticket.setDate(date);
        ticket.setTime(time.trim());
        ticket.setTrainNumber(trainNumber);
        ticket.setCar(car);
        ticket.setSeat(seat);
        ticket.setPdfFile(pdfFile);

        return ticket;
    }


}
