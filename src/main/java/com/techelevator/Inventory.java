package com.techelevator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Inventory implements Loggable{
    List<String> fileInput = new ArrayList<>();
    //private final Scanner userInput = new Scanner(System.in);
    public Map<String, Integer> inventoryCount = new HashMap<>();

    Integer count = 5;
    //boolean itemInStock;


    public Inventory() {

        }


    public boolean itemExists(String slotNumber){
        return inventoryCount.containsKey(slotNumber);

    }

    public boolean itemSoldOut(String slotNumber){
        return inventoryCount.get(slotNumber) == 0;
    }

    // Option 1: menu display
    public void displayItems(File inventoryFile) {


        String line;

        try (Scanner rawInput = new Scanner(inventoryFile)) {
            while (rawInput.hasNextLine()) {
                line = rawInput.nextLine();
                fileInput.add(line);

                String[] parts = line.split("\\|");

                String slotNumber = parts[0];
                String itemName = parts[1];
                BigDecimal price = new BigDecimal(parts[2]);
                //inventoryCount.put(slotNumber, count);

                //only restock each time the application run, so the count will went down when make purchase
                if(!inventoryCount.containsKey(slotNumber)){
                    inventoryCount.put(slotNumber,count);
                }

                // Stock status
                int qty = inventoryCount.get(slotNumber);
                String stockStatus;

                if(qty == 0) {
                    stockStatus = "SOLD OUT";
                } else {
                    stockStatus = "qty: " + qty;
                }

                System.out.println(slotNumber + " " + itemName + " " + price + " " + stockStatus);

            }
        }catch (FileNotFoundException e) {
            System.out.println("Error: File not found " + e.getMessage());
        }
    }


    //option 2: Purchase item
    public BigDecimal getInventory(File inventoryFile, String userChoice) {
        String line;

        try (Scanner rawInput = new Scanner(inventoryFile)) {
            while (rawInput.hasNextLine()) {
                line = rawInput.nextLine();
                fileInput.add(line);

                String[] parts = line.split("\\|");

                String slotNumber = parts[0];
                String itemName = parts[1];
                BigDecimal price = new BigDecimal(parts[2]);

           if(!inventoryCount.containsKey(slotNumber)){
                    inventoryCount.put(slotNumber,count);
                }


                if (userChoice.equals(slotNumber)) {
                   // inventoryCount.put(slotNumber,inventoryCount.get(slotNumber)-1);
                    return price;
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error: File not found " + e.getMessage());

        }        //System.out.println(fileInput);
        return BigDecimal.ZERO;
    }


        //purchase item name
        public String getItemName(File inventoryFile, String slotNumber) {
            try (Scanner rawInput = new Scanner(inventoryFile)) {
                while (rawInput.hasNextLine()) {

                    String[] parts = rawInput.nextLine().split("\\|");
                    if(parts[0].equalsIgnoreCase(slotNumber)){
                        return parts[1];
                    }
                }
            } catch (FileNotFoundException e) {
                System.out.println("Error: File not found " + e.getMessage());

            }
            return "Unknown item";
        }

    //for report Qty
        public BigDecimal getItemPrice(File inventoryFile, String slotNumber) {
            try (Scanner rawInput = new Scanner(inventoryFile)) {
                while (rawInput.hasNextLine()) {

                String[] parts = rawInput.nextLine().split("\\|");
                if(parts[0].equalsIgnoreCase(slotNumber)){
                    return new BigDecimal(parts[2]);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error: File not found " + e.getMessage());

        }
        return BigDecimal.ZERO;
    }



    //message display
        public void itemMessageDisplay( String userChoice) {

                    //item message
                    ItemMessage candyMessage = new Candy();
                    if(userChoice.contains("A")){
                        System.out.println(candyMessage.itemDispenseMessage());
                    } else if (userChoice.contains("B")) {
                        ItemMessage chipMessage = new Chips();
                        System.out.println(chipMessage.itemDispenseMessage());
                    } else if (userChoice.contains("C")) {
                        ItemMessage drinkMessage = new Drink();
                        System.out.println(drinkMessage.itemDispenseMessage());
                    } else if (userChoice.contains("D")) {
                        ItemMessage gumMessage = new Gum();
                        System.out.println(gumMessage.itemDispenseMessage());
                    }
                }

    @Override
    public void generateLog(BigDecimal balance, String purchaseChoice, String actionOrItemName, BigDecimal amountInsert, BigDecimal itemPrice){
        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter formatDate = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss a");
        String formattedDate = dateTime.format(formatDate);
        try {
            String line;
            List<String> fileInput = new ArrayList<>();
            File logFile = new File("log.txt");
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            try (FileWriter logPrinter = new FileWriter(logFile, true)) {
                logPrinter.append(formattedDate);
                logPrinter.append(" " + actionOrItemName);
                logPrinter.append(" " + purchaseChoice);
                logPrinter.append(" $" + itemPrice);
                logPrinter.append(" $" + balance + "\n");
                //fileInput.add(purchaseChoice);

                //SalesReport testing = new SalesReport(fileInput);

            } catch (FileNotFoundException e) {
                System.out.println("File does not exist");
            }
        } catch (IOException e) {
            System.out.println("File failed to create");
        }
    }
}






