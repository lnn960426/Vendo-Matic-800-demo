package com.techelevator;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;

public class Application {
	public static void main(String[] args) {
		Application app = new Application();
		app.run();
	}


	static File inventoryFile = new File("static/vendingmachine.csv");

	public void run() {

		Scanner keyboardInput = new Scanner(System.in);
		String userChoice;
		String purchaseChoice = "";

		//keep track of sales
		Map<String, Integer> sales = new HashMap<>();


		//inventory class
		Inventory newStuff = new Inventory();

		//balance transaction class
		BalanceTransaction management = new BalanceTransaction();

		// Main menu
		while (true) {
			System.out.println("\nWelcome to Vendo-Matic 800!");

			System.out.println("(1) Display Vending Machine Items");
			System.out.println("(2) Purchase");
			System.out.println("(3) Exit");
			System.out.println("(4) Print out sales report");

			System.out.println("What would you like to do?");
			userChoice = keyboardInput.nextLine();

			switch (userChoice) {
				case ("1"):
					newStuff.displayItems(inventoryFile);
					System.out.println("press ANYKEY to return to the main menu");
					keyboardInput.nextLine();
					break;

				// Purchase Menu
				case ("2"):
					boolean purchaseMenu = true;
					while (purchaseMenu) {
						System.out.println("\n(1) Feed money");
						System.out.println("(2) Select product");
						System.out.println("(3) Finish Transaction");
						String userInput = keyboardInput.nextLine();

						//Feed Money
						switch (userInput) {
							case ("1"):
								BigDecimal itemPrice = newStuff.getInventory(inventoryFile, purchaseChoice);
								String itemName = newStuff.getItemName(inventoryFile, purchaseChoice);
								management.insertMoneyLoop(purchaseChoice, itemPrice, itemName);
								break;

							//Select product
							case ("2"):
								System.out.println("Please enter the slotNumber");
								purchaseChoice = keyboardInput.nextLine().toUpperCase();

								//load inventory
								itemPrice = newStuff.getInventory(inventoryFile, purchaseChoice);

								// check if the balance is enough to purchase
								if (management.getCurrentBalance().compareTo(itemPrice) < 0) {
									System.out.println("Insufficient balance!" + " The due amount is:" + "[$" + itemPrice + "]" + " Please insert more money");
									break;
								}

								//check invalid slot number condition
								if (!newStuff.itemExists(purchaseChoice)) {
									System.out.println("This is a invalid slot number.");
									break;
								}

								// check sold out condition
								if (newStuff.itemSoldOut(purchaseChoice)) {
									System.out.println("Sorry, this item is SOLD OUT");
									break;
								}

								//check null
								if (itemPrice == null) {
									System.out.println("Sorry, issue loading price");
									break;
								}

								//qty - 1 if the payment went successful
								int currentQty = newStuff.inventoryCount.get(purchaseChoice);
								newStuff.inventoryCount.put(purchaseChoice, currentQty - 1);

								// if all check passed, deduct the current balance method from balance class
								management.deductBalance(itemPrice);

								//log purchase
								itemName = newStuff.getItemName(inventoryFile, purchaseChoice);
								newStuff.generateLog(management.getCurrentBalance(), purchaseChoice, itemName, null, itemPrice);
								break;

							case ("3"):
								//Purchase success message
								itemName = newStuff.getItemName(inventoryFile, purchaseChoice);

								System.out.println("Purchase successful!");
								System.out.println("Enjoy your " + itemName + " XD!");

								//Item message
								newStuff.itemMessageDisplay(purchaseChoice);

								// log and return change
								itemPrice = newStuff.getInventory(inventoryFile, purchaseChoice);
								management.generateLog(management.getCurrentBalance(), purchaseChoice, "GIVE CHANGE", null, itemPrice);
								management.returnChange();

								//keep track of sale
								if (!sales.containsKey(purchaseChoice)) {
									sales.put(purchaseChoice, 1);
								} else {
									sales.put(purchaseChoice, sales.get(purchaseChoice) + 1);
								}

								purchaseMenu = false;

								break;
						}
					}

					break;

				case ("3"):
					System.out.println("The machine is done. Thank you.");
					System.exit(0);
					break;

				case ("4"):

					int originalQTY = 5;
					BigDecimal totalSales = BigDecimal.ZERO;
					List<String> salesReport = new ArrayList<>();
					SalesReport todaySales = new SalesReport();
					String[] products = {"a","b","c","d"};


					for (String product : products){
						for (int i = 1; i < 5; i++) {
							String slot = (product + i).toUpperCase();

							String name = newStuff.getItemName(inventoryFile,slot);

							int remainingQty = newStuff.inventoryCount.getOrDefault(slot, originalQTY);

							int soldQTY = originalQTY - remainingQty;


							salesReport.add(name + "|" + soldQTY);

							if (soldQTY > 0) {
								BigDecimal price = newStuff.getItemPrice(inventoryFile, slot);
								if (price != null) {
									totalSales = totalSales.add(price.multiply(BigDecimal.valueOf(soldQTY)));

								}
							}
						}

						}
					for (String saleLine: salesReport){
						System.out.println(saleLine);
					}
					todaySales.printSalesReport(salesReport,totalSales);

					System.out.println("\nPress 3 to get the sales report");
					break;

					}
			}
		}
	}











