package com.techelevator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SalesReport {

    private List<String>newSales = new ArrayList<>();

    public SalesReport() {

    }

    public SalesReport(List newSales){
        this.newSales = newSales;
    }

    public List<String> getNewSales() {
        return newSales;
    }

    public void printSalesReport(List<String> sales, BigDecimal totalSales) {
        //Determine and format date/time
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String formattedDate = dateTime.format(formatDate);
        //Map<String, Integer> reportSales = sales;

        try {
            String reportName = "report.txt";
            File reportFile = new File(reportName);
            reportFile.createNewFile();


            try (FileWriter reportPrinter = new FileWriter(reportFile)) {
                for (String line : sales) {
                    reportPrinter.write(line + "\n");

                }
                reportPrinter.write("TOTAL SALES $" + totalSales + "\n");

            } catch (IOException e) {
                System.out.println("Error on writing sales report: " + e.getMessage());
            }

        } catch (IOException e) {
            System.out.println("Error on writing sales report: " + e.getMessage());
        }
    }
}
