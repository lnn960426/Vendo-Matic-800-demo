package com.techelevator;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class InventoryFileProvider {

    private File tempCsvFile;

    public InventoryFileProvider() {
        copyCsvToTemp();
    }

    private void copyCsvToTemp() {
        try {
            ClassPathResource res = new ClassPathResource("vendingmachine.csv");
            InputStream is = res.getInputStream();
            File tmp = File.createTempFile("vendingmachine", ".csv");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                 FileWriter fw = new FileWriter(tmp, false)) {
                String line;
                while ((line = br.readLine()) != null) {
                    fw.write(line);
                    fw.write("\n");
                }
            }
            this.tempCsvFile = tmp;
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy vendingmachine.csv", e);
        }
    }

    public File getInventoryFile() {
        if (tempCsvFile == null || !tempCsvFile.exists()) {
            copyCsvToTemp();
        }
        return tempCsvFile;
    }

    // display for frontend
    public List<ItemView> readAllItems() {
        List<ItemView> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(getInventoryFile(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                String slot = parts[0];
                String name = parts[1];
                BigDecimal price = new BigDecimal(parts[2]);
                list.add(new ItemView(slot, name, price, 0)); // qty 后面由 service 填
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read items", e);
        }
        return list;
    }

    // DTO
    public static class ItemView {
        public String slot;
        public String name;
        public BigDecimal price;
        public int quantity;

        public ItemView(String slot, String name, BigDecimal price, int quantity) {
            this.slot = slot;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }
    }
}