package in.succinct.id.db.model.onboarding.company;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.model.Model;
import com.venky.swf.plugins.collab.db.model.CompanySpecific;

public interface CompanyNetworkDomain extends Model, CompanySpecific {
    @IS_NULLABLE(value = false)
    @UNIQUE_KEY
    public Long getNetworkDomainId();
    public void setNetworkDomainId(Long id);
    public NetworkDomain getNetworkDomain();


}
