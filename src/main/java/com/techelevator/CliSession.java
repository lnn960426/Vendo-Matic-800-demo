package com.techelevator;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple per-session state.
 * Keep where user is and last selected slot.
 */
public class CliSession {
    // "main" or "purchase"
    public String screen = "main";

    // sub-states inside purchase menu
    public boolean feeding = false;      // waiting for 1/2/5/10 or B
    public boolean awaitingSlot = false; // waiting for slot number (e.g. A1)

    // after showing items, wait any key to go back to main
    public boolean waitingAnyKey = false;

    // last selected slot (e.g. A1)
    public String purchaseChoice = "";

    // sales count by slot (demo only)
    public Map<String, Integer> sales = new HashMap<>();
}
