package in.succinct.id.db.model.onboarding.company;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.model.CONFIGURATION;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;

@CONFIGURATION
@MENU("Beckn")
public interface NetworkUsage extends Model {
    @UNIQUE_KEY
    public String getName();
    public void setName(String name);

    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    public boolean isSupplyListed();
    public void setSupplyListed(boolean supplyListed);

    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    public boolean isDemandListed();
    public void setDemandListed(boolean demandListed);

    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    public boolean isFuturesListed();
    public void setFuturesListed(boolean futuresListed);

    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    public boolean isOwnedListing();
    public void setOwnedListing(boolean ownedListing);
}
