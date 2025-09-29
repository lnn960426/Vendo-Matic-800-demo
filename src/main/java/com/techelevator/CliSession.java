package com.techelevator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CliSession {
    // which screen we are in: "main" or "purchase"
    public String screen = "main";

    // inside purchase menu
    public boolean feeding = false;      // waiting for money
    public boolean awaitingSlot = false; // waiting for slot

    // after showing items or report, wait any key to go back
    public boolean waitingAnyKey = false;

    // last selected slot (still keep it)
    public String purchaseChoice = "";

    // simple "sales count" like old map (demo only)
    public Map<String, Integer> sales = new HashMap<>();

    // --- cart: allow many items ---
    public List<String> cartNames = new ArrayList<>();
    public List<BigDecimal> cartPrices = new ArrayList<>();
    public BigDecimal cartTotal = BigDecimal.ZERO;

    // the last item for "currently in cart" hint
    public String lastItemName = "";
    public BigDecimal lastItemPrice = BigDecimal.ZERO;

    public void addToCart(String name, BigDecimal price) {
        cartNames.add(name);
        cartPrices.add(price);
        if (price != null) {
            cartTotal = cartTotal.add(price);
        }
        lastItemName = name;
        lastItemPrice = price == null ? BigDecimal.ZERO : price;
    }

    public void clearCart() {
        cartNames.clear();
        cartPrices.clear();
        cartTotal = BigDecimal.ZERO;
        lastItemName = "";
        lastItemPrice = BigDecimal.ZERO;
    }
}
