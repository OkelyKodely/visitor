package org.o7planning.springmvcshoppingcart.model;
 
import org.o7planning.springmvcshoppingcart.entity.Product;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
 
public class ProductInfo {
    private String code;
    private String name;
    private double price;
 
    private boolean newProduct=false;
 
    // Upload file.
    private CommonsMultipartFile fileData;

    // Upload bulk file.
    private CommonsMultipartFile bulk_Data;

    // Product Description.
    private String description;
    
    public ProductInfo () {}
    
    public ProductInfo(Product product) {
        this.code = product.getCode();
        this.name = product.getName();
        this.price = product.getPrice();
        this.description = product.getDescription();
        
        this.bulk_Data = null;
    }
 
    // Không thay d?i Constructor này,
    // nó du?c s? d?ng trong Hibernate query.
    public ProductInfo(String code, String name, double price, String description, Object bulk_Data) {
        this.code = code;
        this.name = name;
        this.price = price;
        this.description = description;
        
        try {
        this.bulk_Data = (CommonsMultipartFile) bulk_Data;
        } catch(Exception e) {}
    }
 
    public String getCode() {
        return code;
    }
 
    public void setCode(String code) {
        this.code = code;
    }
 
    public String getName() {
        return name;
    }
 
    public void setName(String name) {
        this.name = name;
    }
 
    public double getPrice() {
        return price;
    }
 
    public void setPrice(double price) {
        this.price = price;
    }
 
    public String getDescription() {
        return description;
    }
 
    public void setDescription(String description) {
        this.description = description;
    }
 
    public CommonsMultipartFile getFileData() {
        return fileData;
    }
 
    public void setFileData(CommonsMultipartFile fileData) {
        this.fileData = fileData;
    }
 
    public CommonsMultipartFile getBulkData() {
        return this.bulk_Data;
    }
 
    public void setBulkData(CommonsMultipartFile fileData) {
        this.bulk_Data = fileData;
    }

    public boolean isNewProduct() {
        return newProduct;
    }
 
    public void setNewProduct(boolean newProduct) {
        this.newProduct = newProduct;
    }
 
}