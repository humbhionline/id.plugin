package in.succinct.id.controller;

import com.venky.swf.controller.ModelController;
import com.venky.swf.controller.annotations.SingleRecordAction;
import com.venky.swf.db.Database;
import com.venky.swf.path.Path;
import com.venky.swf.views.ForwardedView;
import com.venky.swf.views.View;
import in.succinct.id.db.model.onboarding.company.ClaimRequest;
import in.succinct.id.db.model.onboarding.company.Company;

public class CompaniesController extends ModelController<Company> {
    public CompaniesController(Path path) {
        super(path);
    }

    @SingleRecordAction(  icon = "fa-plug" , tooltip = "Claim ownership")
    public View claim(long id){
        Company company = Database.getTable(Company.class).get(id);
        ClaimRequest request = company.claim();
        return new ForwardedView(getPath(),"/claim_requests/verifyDomain/" + request.getId());
    }
    
}
