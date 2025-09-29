package com.techelevator;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Text-in / Text-out CLI gateway that mimics the old Application exactly.
 * We keep the same wording line by line.
 */
@RestController
@RequestMapping("/cli")
public class CliController {

    private final InventoryFileProvider fileProvider;
    private final Inventory inventory = new Inventory();
    private final BalanceTransaction wallet = new BalanceTransaction();

    public CliController(InventoryFileProvider fileProvider) {
        this.fileProvider = fileProvider;
        // init stock to 5 for each slot
        for (var it : fileProvider.readAllItems()) {
            inventory.inventoryCount.putIfAbsent(it.slot, 5);
        }
    }

    @PostMapping
    public String handle(@RequestBody String body, HttpSession http) {
        String line = body == null ? "" : body.trim();
        CliSession s = get(http);

        // after "press ANYKEY..." go back to main on any input
        if (s.waitingAnyKey) {
            s.waitingAnyKey = false;
            s.screen = "main";
            s.feeding = false;
            s.awaitingSlot = false;
            return mainMenu();
        }

        try {
            if (s.screen.equals("main")) {
                return handleMain(line, s);
            } else if (s.screen.equals("purchase")) {
                return handlePurchase(line, s);
            } else {
                s.screen = "main";
                return mainMenu();
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // ===== Menus (exact wording) =====

    private String mainMenu() {
        return "\nWelcome to Vendo-Matic 800!\n\n"
                + "(1) Display Vending Machine Items\n"
                + "(2) Purchase\n"
                + "(3) Exit\n"
                + "(4) Print out sales report\n\n"
                + "What would you like to do?";
    }

    private String purchaseMenu() {
        return "\n(1) Feed money\n"
                + "(2) Select product\n"
                + "(3) Finish Transaction";
    }

    // ===== Handlers =====

    private String handleMain(String line, CliSession s) {
        switch (line) {
            case "1": {
                // show items exactly like displayItems()
                StringBuilder sb = new StringBuilder();
                File f = fileProvider.getInventoryFile();
                try (java.util.Scanner raw = new java.util.Scanner(f)) {
                    while (raw.hasNextLine()) {
                        String l = raw.nextLine();
                        String[] parts = l.split("\\|");
                        String slot = parts[0];
                        String name = parts[1];
                        BigDecimal price = new BigDecimal(parts[2]);

                        if (!inventory.inventoryCount.containsKey(slot)) {
                            inventory.inventoryCount.put(slot, 5);
                        }
                        int qty = inventory.inventoryCount.get(slot);
                        String stock = (qty == 0) ? "SOLD OUT" : "qty: " + qty;

                        // exactly: slot + " " + itemName + " " + price + " " + stock
                        sb.append(slot).append(" ").append(name).append(" ")
                                .append(price.toPlainString()).append(" ")
                                .append(stock).append("\n");
                    }
                } catch (Exception e) {
                    return "Error: File not found " + e.getMessage();
                }
                sb.append("press ANYKEY to return to the main menu");
                s.waitingAnyKey = true; // any next input goes back to main
                return sb.toString();
            }
            case "2": {
                s.screen = "purchase";
                s.feeding = false;
                s.awaitingSlot = false;
                return purchaseMenu();
            }
            case "3": {
                // cannot exit server; print the same message
                return "The machine is done. Thank you.";
            }
            case "4": {
                int originalQTY = 5;
                BigDecimal totalSales = BigDecimal.ZERO;
                List<String> salesReport = new ArrayList<>();
                SalesReport todaySales = new SalesReport();
                String[] products = {"a","b","c","d"};
                File f = fileProvider.getInventoryFile();

                for (String product : products) {
                    for (int i = 1; i < 5; i++) {
                        String slot = (product + i).toUpperCase();
                        String name = inventory.getItemName(f, slot);
                        int remainingQty = inventory.inventoryCount.getOrDefault(slot, originalQTY);
                        int soldQTY = originalQTY - remainingQty;
                        salesReport.add(name + "|" + soldQTY);
                        if (soldQTY > 0) {
                            BigDecimal price = inventory.getItemPrice(f, slot);
                            if (price != null) {
                                totalSales = totalSales.add(price.multiply(BigDecimal.valueOf(soldQTY)));
                            }
                        }
                    }
                }
                // print report lines to screen (like your System.out loop)
                StringBuilder sb = new StringBuilder();
                for (String r : salesReport) sb.append(r).append("\n");
                // also write file like your SalesReport (side effect)
                todaySales.printSalesReport(salesReport, totalSales);
                sb.append("\nPress 3 to get the sales report");
                return sb.toString();
            }
            default:
                return mainMenu();
        }
    }

    private String handlePurchase(String line, CliSession s) {
        File f = fileProvider.getInventoryFile();

        // feeding loop
        if (s.feeding) {
            if (line.equalsIgnoreCase("B")) {
                s.feeding = false;
                return purchaseMenu();
            }
            if (line.equals("1") || line.equals("2") || line.equals("5") || line.equals("10")) {
                BigDecimal amount = new BigDecimal(line);
                wallet.addMoney(amount);
                wallet.generateLog(wallet.getCurrentBalance(), "", "FEED MONEY", amount, BigDecimal.ZERO);
                // exactly these two lines:
                return "Current money provided: $" + wallet.getCurrentBalance().toPlainString() + "\n"
                        + "Insert money($1, $2, $5, or $10) OR type B to go back";
            }
            // any other input: show the same prompt again
            return "Current money provided: $" + wallet.getCurrentBalance().toPlainString() + "\n"
                    + "Insert money($1, $2, $5, or $10) OR type B to go back";
        }

        // awaiting slot input
        if (s.awaitingSlot) {
            s.purchaseChoice = (line == null ? "" : line.trim().toUpperCase());
            BigDecimal itemPrice = inventory.getInventory(f, s.purchaseChoice);

            if (wallet.getCurrentBalance().compareTo(itemPrice) < 0) {
                s.awaitingSlot = false;
                return "Insufficient balance!" + " The due amount is:" + "[$" + itemPrice.toPlainString() + "]" + " Please insert more money";
            }
            if (!inventory.itemExists(s.purchaseChoice)) {
                s.awaitingSlot = false;
                return "This is a invalid slot number.";
            }
            if (inventory.itemSoldOut(s.purchaseChoice)) {
                s.awaitingSlot = false;
                return "Sorry, this item is SOLD OUT";
            }
            if (itemPrice == null) {
                s.awaitingSlot = false;
                return "Sorry, issue loading price";
            }

            // deduct 1 qty
            int currentQty = inventory.inventoryCount.get(s.purchaseChoice);
            inventory.inventoryCount.put(s.purchaseChoice, currentQty - 1);

            // deduct balance + log
            wallet.deductBalance(itemPrice);
            String itemName = inventory.getItemName(f, s.purchaseChoice);
            inventory.generateLog(wallet.getCurrentBalance(), s.purchaseChoice, itemName, null, itemPrice);

            // back to purchase menu (old code prints menu again on next loop)
            s.awaitingSlot = false;
            return purchaseMenu();
        }

        // purchase menu options
        switch (line) {
            case "1": { // Feed money -> enter feeding loop
                s.feeding = true;
                return "Current money provided: $" + wallet.getCurrentBalance().toPlainString() + "\n"
                        + "Insert money($1, $2, $5, or $10) OR type B to go back";
            }
            case "2": { // Select product -> ask for slot
                s.awaitingSlot = true;
                return "Please enter the slotNumber";
            }
            case "3": { // Finish Transaction -> print 4 lines + back to main
                String itemName = inventory.getItemName(f, s.purchaseChoice);

                StringBuilder sb = new StringBuilder();
                sb.append("Purchase successful!\n");
                sb.append("Enjoy your ").append(itemName).append(" XD!\n");
                // item message (exact same text as ItemMessage)
                String msg;
                if (s.purchaseChoice != null && !s.purchaseChoice.isEmpty()) {
                    char ch = Character.toUpperCase(s.purchaseChoice.charAt(0));
                    if (ch == 'A') msg = "Munch Munch Yum!";
                    else if (ch == 'B') msg = "Crunch Crunch, Yum!";
                    else if (ch == 'C') msg = "Glug Glug,Yum!";
                    else msg = "Chew Chew, Yum!";
                } else {
                    msg = "";
                }
                sb.append(msg).append("\n");

                // change preview (same lines/format as your returnChange())
                BigDecimal remainingChange = wallet.getCurrentBalance();
                int cents = remainingChange.multiply(BigDecimal.valueOf(100)).intValue();
                int quarters = cents / 25; cents %= 25;
                int dimes = cents / 10; cents %= 10;
                int nickels = cents / 5;

                sb.append("Here is your change: $").append(remainingChange.toPlainString()).append("\n");
                sb.append("The change given as: ").append(quarters).append(" quarter(s),")
                        .append(dimes).append(" dime(s)").append(nickels).append(" nickle(s)").append("\n");

                // log GIVE CHANGE and actually return change (reset to 0)
                BigDecimal itemPrice = inventory.getInventory(f, s.purchaseChoice);
                wallet.generateLog(wallet.getCurrentBalance(), s.purchaseChoice, "GIVE CHANGE", null, itemPrice);
                wallet.returnChange();

                // increase sales counter (like your map)
                if (s.purchaseChoice != null && !s.purchaseChoice.isEmpty()) {
                    s.sales.put(s.purchaseChoice, s.sales.getOrDefault(s.purchaseChoice, 0) + 1);
                }

                // back to main menu
                s.screen = "main";
                s.feeding = false;
                s.awaitingSlot = false;
                s.purchaseChoice = "";
                sb.append("\n").append(mainMenu());
                return sb.toString();
            }
            default:
                return purchaseMenu();
        }
    }

    // ===== Helpers =====

    private CliSession get(HttpSession http) {
        CliSession s = (CliSession) http.getAttribute("CLI");
        if (s == null) {
            s = new CliSession();
            http.setAttribute("CLI", s);
        }
        return s;
    }
}