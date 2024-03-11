package in.succinct.id.db.model.onboarding.company;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;
import com.venky.swf.db.model.application.Application;
import in.succinct.id.db.model.City;
import in.succinct.id.db.model.Country;

public interface ApplicationContext extends Model {
    @IS_NULLABLE(false)
    @UNIQUE_KEY
    public Long getApplicationId();
    public void setApplicationId(Long id);
    public Application getApplication();

    @UNIQUE_KEY(allowMultipleRecordsWithNull = false)
    @IS_NULLABLE
    public Long getCityId();
    public void setCityId(Long id);
    public City getCity();

    @UNIQUE_KEY(allowMultipleRecordsWithNull = false)
    @IS_NULLABLE
    public Long getCountryId();
    public void setCountryId(Long id);
    public Country getCountry();


}
