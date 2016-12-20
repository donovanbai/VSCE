package client;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Asset {
    private String name;
    private String type;
    private BigDecimal price;
    private int quantity;
    private BigDecimal totalVal;
    
    public Asset() {
        name = "";
        type = "";
        price = new BigDecimal("0");
        quantity = 0;
        totalVal = new BigDecimal("0");
    }
    public Asset(String name, String type, BigDecimal price, int quantity) {
        this.name = name;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.totalVal = price.multiply(new BigDecimal(quantity));
        this.totalVal = this.totalVal.setScale(2, RoundingMode.UP); // round totalVal to 2 decimal places
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        totalVal = price.multiply(new BigDecimal(quantity));
        totalVal = totalVal.setScale(2, RoundingMode.UP);
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        totalVal = price.multiply(new BigDecimal(quantity));
        totalVal = totalVal.setScale(2, RoundingMode.UP);
    }

    public BigDecimal getTotalVal() {
        return totalVal;
    }
}
