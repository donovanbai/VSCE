package client;

public class Asset {
    private String name;
    private String type;
    private double price;
    private int quantity;
    private double totalVal;
    
    public Asset() {
        name = "";
        type = "";
        price = 0;
        quantity = 0;
        totalVal = 0;
    }
    public Asset(String name, String type, double price, int quantity) {
        this.name = name;
        this.type = type;
        this.price = price;
        this.quantity = quantity;
        this.totalVal = price * quantity;
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

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalVal() {
        return totalVal;
    }
}
