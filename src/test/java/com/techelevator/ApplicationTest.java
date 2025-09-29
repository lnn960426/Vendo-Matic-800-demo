package com.techelevator;

import org.junit.jupiter.api.*;

import java.math.BigDecimal;


public class ApplicationTest{
@Test
    public void currentBalanceTest(){

        //arrange
        BalanceTransaction testingBalance = new BalanceTransaction();
        testingBalance.setCurrentBalance(new BigDecimal(10.00));

        BigDecimal itemPrice = new BigDecimal(3.05);
        String itemName = "Potato Crisps";
        String purchaseChoice = "a1";
        BigDecimal expected = new BigDecimal(6.95);

        //act
        testingBalance.deductBalance(itemPrice);
        BigDecimal actual = testingBalance.getCurrentBalance();

        //assert
        Assertions.assertEquals(expected, actual);

    }
    @Test
    public void addMoneyTest(){
        //arrange
        BalanceTransaction testingBalance = new BalanceTransaction();
        testingBalance.setCurrentBalance(new BigDecimal(10.00));
        BigDecimal expected = new BigDecimal(15.00);

        //act
        testingBalance.addMoney(new BigDecimal(5.00));
        BigDecimal actual = testingBalance.getCurrentBalance();
        //assert
        Assertions.assertEquals(expected, actual);
    }
}
