package com.techelevator;
import org.junit.jupiter.api.*;
public class InventoryTest {
    @Test
    public void testItemSoldOut(){
        //arrange
        Inventory testingInventory = new Inventory();
        String slotNumber = "c3";

        testingInventory.inventoryCount.put(slotNumber, 0);

        //act
        boolean actual = testingInventory.itemSoldOut(slotNumber);

        //assert
        Assertions.assertTrue(actual);
    }
    @Test
    public void testItemExistsTrue(){
        //arrange
        Inventory testingInventory = new Inventory();
        String slotNumber = "c3";

        testingInventory.inventoryCount.put(slotNumber, 5);

        //act
        boolean actual = testingInventory.itemExists(slotNumber);
        //assert
        Assertions.assertTrue(actual);
    }
    @Test
    public void testItemExistsFalse(){
        //arrange
        Inventory testingInventory = new Inventory();
        String slotNumber = "z3";

        //act
        boolean actual = testingInventory.itemExists(slotNumber);
        //assert
        Assertions.assertFalse(actual);
    }


}
