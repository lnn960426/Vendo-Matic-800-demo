package com.techelevator;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BalanceTransaction implements Loggable{

    private BigDecimal currentBalance = BigDecimal.ZERO;

    private BigDecimal remainingChange = BigDecimal.ZERO;


    //Getter
    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRemainingChange() {
        return remainingChange;
    }

    public void setCurrentBalance(BigDecimal balance){
        this.currentBalance = balance;
    }



    //adding the money to the machine
    public void addMoney(BigDecimal amount){
       currentBalance = currentBalance.add(amount);
    }

    public void deductBalance(BigDecimal itemPrice){
        System.out.println("price is: " + itemPrice + ", press 3 to finish transaction");
        currentBalance = currentBalance.subtract(itemPrice);

    }

    // feed money method
    public void insertMoneyLoop(String purchaseChoice, BigDecimal itemPrice, String itemName){

        //insert money (user input)
        Scanner input = new Scanner(System.in);
        boolean inserting = true;

        while(inserting){

            System.out.println("Current money provided: $" + getCurrentBalance());
            System.out.println("Insert money($1, $2, $5, or $10) OR type B to go back");

            String moneyInsert = input.nextLine();

            //go back to main menu
            if(moneyInsert.equalsIgnoreCase("B")){
                inserting = false;
            } else if(moneyInsert.equals("1") || moneyInsert.equals("2") || moneyInsert.equals("5") || moneyInsert.equals("10")){

                BigDecimal amountInsert = new BigDecimal(moneyInsert);
                addMoney(amountInsert);
                generateLog(getCurrentBalance(), purchaseChoice, "FEED MONEY", amountInsert, itemPrice);
            } else{
                System.out.println("Invalid amount. Please insert $1, $2, $5, or $10");
            }

        }

        }

    //returning change method
    public void returnChange(){
        remainingChange = currentBalance;

        //converts dollars to cents so we can work in integer
        int cents = remainingChange.multiply(BigDecimal.valueOf(100)).intValue();

        //convert cent to quarter, get as many quarter as possible and use the leftover cent for next step
        int quarters = cents / 25;
        cents %=25;

        //convert remainder to dimes, and use the leftover cent for next step
        int dimes = cents / 10;
        cents %=10;

        //convert the rest to nickles and if there still remainder then it will stay as it is(cent)
        int nickels = cents / 5;

        System.out.println("Here is your change: $" + remainingChange);
        System.out.println("The change given as: " + quarters + " quarter(s)," + dimes + " dime(s)" + nickels + " nickle(s)");

        //reset the balances once the user get the change
        currentBalance = BigDecimal.ZERO;
        remainingChange = BigDecimal.ZERO;

    }

    //implements Loggable interface
@Override
    public void generateLog(BigDecimal balance, String purchaseChoice, String actionOrItemName, BigDecimal amountInsert, BigDecimal itemPrice){
            //Determine and format date/time
            LocalDateTime dateTime = LocalDateTime.now();
            DateTimeFormatter formatDate = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss a");
            String formattedDate = dateTime.format(formatDate);
            try {
                File logFile = new File("log.txt");

                //do not overwrite log file if one already exists
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }
                if (actionOrItemName.contains("GIVE")){
                    try (FileWriter logPrinter = new FileWriter(logFile, true)) {
                        logPrinter.append(formattedDate);
                        logPrinter.append(" " + actionOrItemName);
                        logPrinter.append(" $" + balance);
                        logPrinter.append(" $0.00\n");

                    } catch (FileNotFoundException e) {
                        System.out.println("File does not exist");
                    }
                }else if (actionOrItemName.contains("FEED")){
                    try (FileWriter logPrinter = new FileWriter(logFile, true)) {
                        logPrinter.append(formattedDate);
                        logPrinter.append(" " + actionOrItemName);
                        logPrinter.append(" $" + amountInsert + ".00");
                        logPrinter.append(" $" + balance + ".00\n");

                    } catch (FileNotFoundException e) {
                        System.out.println("File does not exist");
                    }
                }
            } catch (IOException e) {
                System.out.println("File failed to create");
            }
    }
}

