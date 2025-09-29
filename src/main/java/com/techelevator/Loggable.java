package com.techelevator;

import java.io.File;
import java.math.BigDecimal;

public interface Loggable {

    public void generateLog(BigDecimal balance, String purchaseChoice, String actionOrItemName, BigDecimal amountInsert, BigDecimal itemPrice);
}
