package in.succinct.id.db.model.onboarding.company;

import com.venky.swf.db.Database;
import com.venky.swf.db.table.ModelImpl;

public class CompanyImpl extends ModelImpl<Company> {
    public CompanyImpl() {
    }

    public CompanyImpl(Company proxy) {
        super(proxy);
    }

    public ClaimRequest claim(){
        ClaimRequest claimRequest = Database.getTable(ClaimRequest.class).newRecord();
        claimRequest.setCompanyId(getProxy().getId());
        claimRequest.setCreatorUserId(Database.getInstance().getCurrentUser().getId());
        claimRequest = Database.getTable(ClaimRequest.class).getRefreshed(claimRequest);
        claimRequest.save();
        return claimRequest;
    }
}
