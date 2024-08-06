package in.succinct.id.db.model.onboarding.catalog;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.model.Model;

@IS_VIRTUAL
public interface Product extends Model {

    public String getProductId();
    public void setProductId(String productId);

    public String getProductName();
    public void setProductName(String productName);

    public String getProductDescription();
    public void setProductDescription(String productDescription);

    public String getImageUrl();
    public void setImageUrl(String imageUrl);


    public double getSellingPrice();
    public void setSellingPrice(double sellingPrice);


    public double getMaxRetailPrice();
    public void setMaxRetailPrice(double maxRetailPrice);


    public String getKeywords();
    public void setKeywords(String keyWords);

}
