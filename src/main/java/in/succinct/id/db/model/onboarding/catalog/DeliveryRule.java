package in.succinct.id.db.model.onboarding.catalog;

import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.model.Model;

@IS_VIRTUAL
public interface DeliveryRule extends Model {

    public double getMaxDistance();
    public void setMaxDistance(double maxDistance);
    
    public double getMinOrderValue();
    public void setMinOrderValue(double minOrderValue);

    public double getMaxWeight();
    public void setMaxWeight(double maxWeight);

    public double getMinDistanceCharged();
    public void setMinDistanceCharged(double minDistanceCharged);

    public double getChargesPerKm();
    public void setChargesPerKm(double chargesPerKm);

    
}
