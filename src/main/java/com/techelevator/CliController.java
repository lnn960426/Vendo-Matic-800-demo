package com.techelevator;

import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/cli")
public class CliController {

    // keep one instance of these simple classes
    private final InventoryFileProvider fileProvider;
    private final Inventory inventory = new Inventory();
    private final BalanceTransaction wallet = new BalanceTransaction();

    public CliController(InventoryFileProvider fileProvider) {
        this.fileProvider = fileProvider;

        // make sure we have stock=5 for each slot once
        for (var it : fileProvider.readAllItems()) {
            if (!inventory.inventoryCount.containsKey(it.slot)) {
                inventory.inventoryCount.put(it.slot, 5);
            }
        }
    }

    // ====== POST /cli  ======
    @PostMapping
    public String handle(@RequestBody String body, HttpSession http) {
        String line = (body == null) ? "" : body.trim();
        CliSession s = getSession(http);

        // any input returns to main menu
        if (s.waitingAnyKey) {
            s.waitingAnyKey = false;
            s.screen = "main";
            s.feeding = false;
            s.awaitingSlot = false;
            return mainMenu();
        }

        // allow "menu" to force show main menu
        if ("menu".equalsIgnoreCase(line) || "help".equalsIgnoreCase(line)) {
            s.screen = "main";
            s.feeding = false;
            s.awaitingSlot = false;
            return mainMenu();
        }

        if ("main".equals(s.screen)) {
            return handleMain(line, s);
        } else {
            return handlePurchase(line, s);
        }
    }

    // ====== GET /cli/report  (download report.txt) ======
    @GetMapping("/report")
    public ResponseEntity<Resource> downloadReport() {
        File file = new File("report.txt"); // SalesReport writes this file name
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource res = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"report.txt\"")
                .body(res);
    }

    // ====== Menus ======
    private String mainMenu() {
        return "\nWelcome to Vendo-Matic 800!\n\n"
                + "(1) Display Vending Machine Items\n"
                + "(2) Purchase\n"
                + "(3) Exit\n"
                + "(4) Print out sales report\n\n"
                + "What would you like to do?";
    }

    // purchase menu with optional "currently in cart"
    private String purchaseMenu(CliSession s) {
        String text = "\n(1) Feed money\n"
                + "(2) Select product\n"
                + "(3) Finish Transaction";
        if (s.purchaseChoice != null && !s.purchaseChoice.isEmpty()) {
            File f = fileProvider.getInventoryFile();
            String name = inventory.getItemName(f, s.purchaseChoice);
            BigDecimal price = inventory.getItemPrice(f, s.purchaseChoice);
            text += "\ncurrently in cart: " + name + " $" + price.toPlainString();
        }
        return text;
    }

    // ====== Main menu flow ======
    private String handleMain(String line, CliSession s) {
        switch (line) {
            case "1": {
                // show items exactly like Inventory.displayItems()
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

                        // format: slot + " " + name + " " + price + " " + stock
                        sb.append(slot).append(" ").append(name).append(" ")
                                .append(price.toPlainString()).append(" ").append(stock).append("\n");
                    }
                } catch (Exception e) {
                    return "Error: File not found " + e.getMessage();
                }

                // your request: "type ANYKEY ..."
                sb.append("type ANYKEY to return to the main menu");
                s.waitingAnyKey = true;
                return sb.toString();
            }
            case "2": {
                s.screen = "purchase";
                s.feeding = false;
                s.awaitingSlot = false;
                return purchaseMenu(s);
            }
            case "3": {
                // we cannot exit the server; just print same line
                return "The machine is done. Thank you.";
            }
            case "4": {
                // build sales report (same logic as your Application)
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

                // print lines to screen, also write file via SalesReport
                StringBuilder sb = new StringBuilder();
                for (String r : salesReport) {
                    sb.append(r).append("\n");
                }
                todaySales.printSalesReport(salesReport, totalSales);

                sb.append("TOTAL SALES $").append(totalSales.toPlainString()).append("\n");
                sb.append("Download sales report: /cli/report").append("\n\n");
                sb.append("press enter to restart the machine");

                // next input goes back to main menu
                s.waitingAnyKey = true;
                return sb.toString();
            }
            default:
                // any other input -> show menu again
                return mainMenu();
        }
    }

    // ====== Purchase menu flow ======
    private String handlePurchase(String line, CliSession s) {
        File f = fileProvider.getInventoryFile();

        // 1) feeding loop (after user chose "1")
        if (s.feeding) {
            if (line.equalsIgnoreCase("B")) {
                s.feeding = false;
                return purchaseMenu(s);
            }
            if (line.equals("1") || line.equals("2") || line.equals("5") || line.equals("10")) {
                BigDecimal amount = new BigDecimal(line);
                wallet.addMoney(amount);
                wallet.generateLog(wallet.getCurrentBalance(), "", "FEED MONEY", amount, BigDecimal.ZERO);

                // exactly same two lines as your console text
                return "Current money provided: $" + wallet.getCurrentBalance().toPlainString() + "\n"
                        + "Insert money($1, $2, $5, or $10) OR type B to go back";
            }
            // invalid -> show the same prompt again
            return "Current money provided: $" + wallet.getCurrentBalance().toPlainString() + "\n"
                    + "Insert money($1, $2, $5, or $10) OR type B to go back";
        }

        // 2) awaiting slot number (after user chose "2")
        if (s.awaitingSlot) {
            s.purchaseChoice = (line == null ? "" : line.trim().toUpperCase());
            BigDecimal itemPrice = inventory.getInventory(f, s.purchaseChoice);

            // same validations and messages as your code
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

            // deduct stock
            int currentQty = inventory.inventoryCount.get(s.purchaseChoice);
            inventory.inventoryCount.put(s.purchaseChoice, currentQty - 1);

            // deduct balance + log
            wallet.deductBalance(itemPrice);
            String itemName = inventory.getItemName(f, s.purchaseChoice);
            inventory.generateLog(wallet.getCurrentBalance(), s.purchaseChoice, itemName, null, itemPrice);

            // go back to purchase menu, with "currently in cart"
            s.awaitingSlot = false;
            return purchaseMenu(s);
        }

        // 3) purchase menu options
        switch (line) {
            case "1": // enter feeding loop
                s.feeding = true;
                return "Current money provided: $" + wallet.getCurrentBalance().toPlainString() + "\n"
                        + "Insert money($1, $2, $5, or $10) OR type B to go back";

            case "2": // ask for slot number
                s.awaitingSlot = true;
                return "Please enter the slotNumber";

            case "3": { // finish transaction
                String itemName = inventory.getItemName(f, s.purchaseChoice);
                BigDecimal itemPrice = inventory.getInventory(f, s.purchaseChoice);

                StringBuilder sb = new StringBuilder();
                sb.append("Purchase successful!\n");
                sb.append("Enjoy your ").append(itemName).append(" XD!\n");

                // item message (same as ItemMessage output)
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

                // summary line (your new request)
                sb.append("You bought: ").append(itemName)
                        .append(" for $").append(itemPrice.toPlainString()).append("\n");

                // change lines (keep your exact wording/commas)
                BigDecimal remainingChange = wallet.getCurrentBalance();
                int cents = remainingChange.multiply(BigDecimal.valueOf(100)).intValue();
                int quarters = cents / 25; cents %= 25;
                int dimes = cents / 10; cents %= 10;
                int nickels = cents / 5;

                sb.append("Here is your change: $").append(remainingChange.toPlainString()).append("\n");
                sb.append("The change given as: ").append(quarters).append(" quarter(s),")
                        .append(dimes).append(" dime(s)").append(nickels).append(" nickle(s)").append("\n");

                // log + reset balance
                wallet.generateLog(wallet.getCurrentBalance(), s.purchaseChoice, "GIVE CHANGE", null, itemPrice);
                wallet.returnChange();

                // update sales count (demo only)
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
                // any other input -> show purchase menu again
                return purchaseMenu(s);
        }
    }

    // ====== session helper ======
    private CliSession getSession(HttpSession http) {
        CliSession s = (CliSession) http.getAttribute("CLI");
        if (s == null) {
            s = new CliSession();
            http.setAttribute("CLI", s);
        }
        return s;
    }
}}