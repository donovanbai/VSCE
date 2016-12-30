package client;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Asset {
    private String name;
    private String type;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal totalVal;
    private BigDecimal gain;

    public Asset(String name, String type, BigDecimal price, BigDecimal quantity, BigDecimal orig) {
        this.name = name;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.totalVal = price.multiply((quantity));
        this.totalVal = this.totalVal.setScale(2, RoundingMode.UP); // round totalVal to 2 decimal places
        this.gain = totalVal.subtract(orig);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) {
        this.price = price;
        totalVal = price.multiply(quantity);
        totalVal = totalVal.setScale(2, RoundingMode.UP);
    }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
        totalVal = price.multiply(quantity);
        totalVal = totalVal.setScale(2, RoundingMode.UP);
    }

    public BigDecimal getTotalVal() { return totalVal; }

    public BigDecimal getGain() { return gain; }
    public void setGain(BigDecimal gain) { this.gain = gain; }
}
