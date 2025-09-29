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

    private final InventoryFileProvider fileProvider;
    private final Inventory inventory = new Inventory();
    private final BalanceTransaction wallet = new BalanceTransaction();

    public CliController(InventoryFileProvider fileProvider) {
        this.fileProvider = fileProvider;
        for (var it : fileProvider.readAllItems()) {
            inventory.inventoryCount.putIfAbsent(it.slot, 5);
        }
    }

    @PostMapping
    public String handle(@RequestBody String body, HttpSession http) {
        String line = body == null ? "" : body.trim();
        CliSession s = get(http);

        if (s.waitingAnyKey) {
            s.waitingAnyKey = false;
            s.screen = "main";
            s.feeding = false;
            s.awaitingSlot = false;
            return mainMenu();
        }
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

    @GetMapping("/report")
    public ResponseEntity<Resource> downloadReport() {
        File file = new File("report.txt");
        if (!file.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"report.txt\"")
                .body(new FileSystemResource(file));
    }

    private String mainMenu() {
        return "\nWelcome to Vendo-Matic 800!\n\n"
                + "(1) Display Vending Machine Items\n"
                + "(2) Purchase\n"
                + "(3) Exit\n"
                + "(4) Print out sales report\n\n"
                + "What would you like to do?";
    }

    private String purchaseMenu(CliSession s) {
        String text = "\n(1) Feed money\n"
                + "(2) Select product\n"
                + "(3) Finish Transaction";
        if (s.lastItemName != null && !s.lastItemName.isEmpty()) {
            text += "\ncurrently in cart: " + s.lastItemName + " $" + s.lastItemPrice.toPlainString();
        }
        return text;
    }

    private String handleMain(String line, CliSession s) {
        switch (line) {
            case "1": {
                StringBuilder sb = new StringBuilder();
                File f = fileProvider.getInventoryFile();
                try (java.util.Scanner raw = new java.util.Scanner(f)) {
                    while (raw.hasNextLine()) {
                        String l = raw.nextLine();
                        String[] parts = l.split("\\|");
                        String slot = parts[0];
                        String name = parts[1];
                        BigDecimal price = new BigDecimal(parts[2]);

                        inventory.inventoryCount.putIfAbsent(slot, 5);
                        int qty = inventory.inventoryCount.get(slot);
                        String stock = (qty == 0) ? "SOLD OUT" : "qty: " + qty;

                        sb.append(slot).append(" ").append(name).append(" ")
                                .append(price.toPlainString()).append(" ").append(stock).append("\n");
                    }
                } catch (Exception e) {
                    return "Error: File not found " + e.getMessage();
                }
                sb.append("type ANYKEY to return to the main menu");
                s.waitingAnyKey = true;
                return sb.toString();
            }
            case "2": {
                s.screen = "purchase";
                s.feeding = false;
                s.awaitingSlot = false;
                // show menu + immediate prompt (so user sees both without scrolling)
                return purchaseMenu(s) + "\nPlease enter the slotNumber";
            }
            case "3": {
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

                StringBuilder sb = new StringBuilder();
                for (String r : salesReport) sb.append(r).append("\n");
                todaySales.printSalesReport(salesReport, totalSales);

                sb.append("TOTAL SALES $").append(totalSales.toPlainString()).append("\n");
                sb.append("Download sales report....").append("\n\n");
                sb.append("press enter to restart the machine");

                s.waitingAnyKey = true;
                return sb.toString();
            }
            default:
                return mainMenu();
        }
    }

    private String handlePurchase(String line, CliSession s) {
        File f = fileProvider.getInventoryFile();

        if (s.feeding) {
            if (line.equalsIgnoreCase("B")) {
                s.feeding = false;
                return purchaseMenu(s);
            }
            if (line.equals("1") || line.equals("2") || line.equals("5") || line.equals("10")) {
                BigDecimal amount = new BigDecimal(line);
                wallet.addMoney(amount);
                wallet.generateLog(wallet.getCurrentBalance(), "", "FEED MONEY", amount, BigDecimal.ZERO);
                return "Current money provided: $" + wallet.getCurrentBalance().toPlainString() + "\n"
                        + "Insert money($1, $2, $5, or $10) OR type B to go back";
            }
            return "Current money provided: $" + wallet.getCurrentBalance().toPlainString() + "\n"
                    + "Insert money($1, $2, $5, or $10) OR type B to go back";
        }

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

            // deduct stock and balance (like original)
            int currentQty = inventory.inventoryCount.get(s.purchaseChoice);
            inventory.inventoryCount.put(s.purchaseChoice, currentQty - 1);

            wallet.deductBalance(itemPrice);

            String itemName = inventory.getItemName(f, s.purchaseChoice);
            inventory.generateLog(wallet.getCurrentBalance(), s.purchaseChoice, itemName, null, itemPrice);

            // *** add to cart (prevent override previous) ***
            s.addToCart(itemName, itemPrice);

            // back to purchase menu with "currently in cart"
            s.awaitingSlot = false;
            return purchaseMenu(s);
        }

        switch (line) {
            case "1":
                s.feeding = true;
                return "Current money provided: $" + wallet.getCurrentBalance().toPlainString() + "\n"
                        + "Insert money($1, $2, $5, or $10) OR type B to go back";
            case "2":
                s.awaitingSlot = true;
                // show menu + prompt together (same idea as main->purchase)
                return purchaseMenu(s) + "\nPlease enter the slotNumber";
            case "3": {
                // build finish text using ALL items in cart
                StringBuilder sb = new StringBuilder();
                sb.append("Purchase successful!\n");
                if (!s.cartNames.isEmpty()) {
                    // use last slot's first letter to print snack message
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
                    // Enjoy line uses last item name (keeps old feeling)
                    String lastName = s.lastItemName == null ? "" : s.lastItemName;
                    sb.append("Enjoy your ").append(lastName).append(" XD!\n");
                    sb.append(msg).append("\n");

                    // list all items and total spent
                    sb.append("You bought:\n");
                    for (int i = 0; i < s.cartNames.size(); i++) {
                        sb.append(" - ").append(s.cartNames.get(i))
                                .append(" $").append(s.cartPrices.get(i).toPlainString()).append("\n");
                    }
                    sb.append("Total spent: $").append(s.cartTotal.toPlainString()).append("\n");
                } else {
                    sb.append("Enjoy your XD!\n");
                }

                // change lines (same wording)
                BigDecimal remainingChange = wallet.getCurrentBalance();
                int cents = remainingChange.multiply(BigDecimal.valueOf(100)).intValue();
                int quarters = cents / 25; cents %= 25;
                int dimes = cents / 10; cents %= 10;
                int nickels = cents / 5;

                sb.append("Here is your change: $").append(remainingChange.toPlainString()).append("\n");
                sb.append("The change given as: ").append(quarters).append(" quarter(s),")
                        .append(dimes).append(" dime(s)").append(nickels).append(" nickle(s)").append("\n");

                // log GIVE CHANGE and reset balance
                BigDecimal priceForLog = s.lastItemPrice; // just keep something for log
                wallet.generateLog(wallet.getCurrentBalance(), s.purchaseChoice, "GIVE CHANGE", null, priceForLog);
                wallet.returnChange();

                // sales counter (use last slot if present)
                if (s.purchaseChoice != null && !s.purchaseChoice.isEmpty()) {
                    s.sales.put(s.purchaseChoice, s.sales.getOrDefault(s.purchaseChoice, 0) + 1);
                }

                // clear cart after finishing
                s.clearCart();

                // back to main
                s.screen = "main";
                s.feeding = false;
                s.awaitingSlot = false;
                s.purchaseChoice = "";

                sb.append("\n").append(mainMenu());
                return sb.toString();
            }
            default:
                return purchaseMenu(s);
        }
    }

    private CliSession get(HttpSession http) {
        CliSession s = (CliSession) http.getAttribute("CLI");
        if (s == null) {
            s = new CliSession();
            http.setAttribute("CLI", s);
        }
        return s;
    }
}