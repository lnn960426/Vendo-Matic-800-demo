package com.techelevator;

import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class VendingService {

    private final InventoryFileProvider fileProvider;
    private final Inventory inventory;
    private final BalanceTransaction wallet;

    public VendingService(InventoryFileProvider fileProvider) {
        this.fileProvider = fileProvider;
        this.inventory = new Inventory();
        this.wallet = new BalanceTransaction();
        initCountsToFive();
    }

    private void initCountsToFive() {
        File f = fileProvider.getInventoryFile();
        List<InventoryFileProvider.ItemView> items = fileProvider.readAllItems();
        for (InventoryFileProvider.ItemView it : items) {
            if (!inventory.inventoryCount.containsKey(it.slot)) {
                inventory.inventoryCount.put(it.slot, 5);
            }
        }
    }

    public List<InventoryFileProvider.ItemView> listItems() {
        List<InventoryFileProvider.ItemView> items = fileProvider.readAllItems();
        for (InventoryFileProvider.ItemView it : items) {
            Integer q = inventory.inventoryCount.get(it.slot);
            it.quantity = (q == null ? 0 : q);
        }
        return items;
    }

    public BigDecimal getBalance() {
        return wallet.getCurrentBalance();
    }

    public BigDecimal feed(BigDecimal amount) {
        if (amount == null) return wallet.getCurrentBalance();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) return wallet.getCurrentBalance();
        wallet.addMoney(amount);
        wallet.generateLog(wallet.getCurrentBalance(), "", "FEED MONEY", amount, BigDecimal.ZERO);
        return wallet.getCurrentBalance();
    }

    public PurchaseResult select(String code) {
        if (code == null || code.trim().isEmpty()) {
            return PurchaseResult.error("Invalid slot");
        }
        code = code.toUpperCase();

        if (!inventory.itemExists(code)) {
            return PurchaseResult.error("Invalid slot");
        }
        if (inventory.itemSoldOut(code)) {
            return PurchaseResult.error("SOLD OUT");
        }

        File f = fileProvider.getInventoryFile();
        BigDecimal price = inventory.getInventory(f, code);
        if (price == null) {
            return PurchaseResult.error("Price not found");
        }
        if (wallet.getCurrentBalance().compareTo(price) < 0) {
            return PurchaseResult.error("Insufficient funds");
        }

        // deduct inventory
        Integer curr = inventory.inventoryCount.get(code);
        if (curr == null) curr = 0;
        if (curr <= 0) {
            return PurchaseResult.error("SOLD OUT");
        }
        inventory.inventoryCount.put(code, curr - 1);

        // deduct balance
        wallet.deductBalance(price);
        String itemName = inventory.getItemName(f, code);
        inventory.generateLog(wallet.getCurrentBalance(), code, itemName, null, price);

        // message
        String msg;
        if (code.startsWith("A")) {
            msg = new Candy().itemDispenseMessage();
        } else if (code.startsWith("B")) {
            msg = new Chips().itemDispenseMessage();
        } else if (code.startsWith("C")) {
            msg = new Drink().itemDispenseMessage();
        } else {
            msg = new Gum().itemDispenseMessage();
        }

        return PurchaseResult.ok(itemName, price, wallet.getCurrentBalance(), msg);
    }

    public Change finish() {
        BigDecimal before = wallet.getCurrentBalance();
        wallet.generateLog(before, "", "GIVE CHANGE", null, BigDecimal.ZERO);
        Change change = Change.from(before);
        wallet.returnChange();
        return change;
    }

    public void reset() {
        // reset
        for (Map.Entry<String, Integer> e : new ArrayList<>(inventory.inventoryCount.entrySet())) {
            inventory.inventoryCount.put(e.getKey(), 5);
        }
        wallet.setCurrentBalance(BigDecimal.ZERO);
    }

    // result
    public static class PurchaseResult {
        public boolean ok;
        public String error;
        public String itemName;
        public BigDecimal price;
        public BigDecimal balance;
        public String message;

        public static PurchaseResult ok(String itemName, BigDecimal price, BigDecimal balance, String message) {
            PurchaseResult r = new PurchaseResult();
            r.ok = true;
            r.itemName = itemName;
            r.price = price;
            r.balance = balance;
            r.message = message;
            return r;
        }

        public static PurchaseResult error(String err) {
            PurchaseResult r = new PurchaseResult();
            r.ok = false;
            r.error = err;
            return r;
        }
    }

    public static class Change {
        public int quarters;
        public int dimes;
        public int nickels;

        public static Change from(BigDecimal dollars) {
            Change c = new Change();
            if (dollars == null) dollars = BigDecimal.ZERO;
            int cents = dollars.multiply(new BigDecimal("100")).intValue();
            c.quarters = cents / 25;
            cents = cents % 25;
            c.dimes = cents / 10;
            cents = cents % 10;
            c.nickels = cents / 5;
            return c;
        }
    }
}
