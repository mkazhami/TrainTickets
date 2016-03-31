package com.mikhailkazhamiaka.viatickets;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by mikhailkazhamiaka on 2016-02-21.
 */
public class Ticket {

    private String pdfFile;
    private String qrFile;

    private String origin;
    private String destination;
    private String date;
    private String time;
    private String trainNumber;
    private String car;
    private String seat;

    public Ticket () {

    }

    public String getPdfFile() { return this.pdfFile; }
    public String getQrFile() { return this.qrFile; }
    public String getOrigin() { return this.origin; }
    public String getDestination() { return this.destination; }
    public String getDate() { return this.date; }
    public String getTime() { return this.time; }
    public String getTrainNumber() { return this.trainNumber; }
    public String getCar() { return this.car; }
    public String getSeat() { return this.seat; }

    public void setPdfFile(String file) { this.pdfFile = file; }
    public void setQrFile(String file) { this.qrFile = file; }
    public void setOrigin(String origin) { this.origin = origin; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setDate(String date) { this.date = date; }
    public void setTime(String time) { this.time = time; }
    public void setTrainNumber(String trainNumber) { this.trainNumber = trainNumber; }
    public void setCar(String car) { this.car = car; }
    public void setSeat(String seat) { this.seat = seat; }

    public String getFormattedText() {
        return "(" + date + ") " + origin + " - " + destination + "   " + time + "\nTrain #: " + trainNumber + "  Car: " + car + "  Seat: " + seat;
    }

    public String getDatetimeFormat() {
        Date date = null;
        try {
            String dateStrBad = this.getDate().replace(".", "").replace(",", "") + " " + getTime();
            String dateStr = "";
            // pdfBox gave back text with weird space characters, so replace them with normal space character
            // TODO: replace this hacky method
            for (byte b : dateStrBad.getBytes()) {
                if (b == -62) b = 32;
                if (b == -96) continue;
                dateStr += (char) b;
            }
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd yyyy HH:mm");
                date = sdf.parse(dateStr);
            } catch(ParseException e) {
                SimpleDateFormat sdf = new SimpleDateFormat("E MMM d yyyy HH:mm");
                date = sdf.parse(dateStr);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String dateStr = df.format(date);
        return dateStr;
    }
}
