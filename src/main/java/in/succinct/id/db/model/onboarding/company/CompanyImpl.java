package in.succinct.id.db.model.onboarding.company;

import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.IS_VIRTUAL;
import com.venky.swf.db.table.ModelImpl;
import in.succinct.plugins.kyc.db.model.DocumentedModelProxy;
import in.succinct.plugins.kyc.db.model.submissions.Document;
import in.succinct.plugins.kyc.db.model.submissions.SubmittedDocument;

import java.util.List;

public class CompanyImpl extends ModelImpl<Company> {
    public CompanyImpl() {
    }

    DocumentedModelProxy<Company> documentedModelProxy ;
    public CompanyImpl(Company proxy) {
        super(proxy);
        this.documentedModelProxy = new DocumentedModelProxy<>(proxy);
    }

    public ClaimRequest claim(){
        ClaimRequest claimRequest = Database.getTable(ClaimRequest.class).newRecord();
        claimRequest.setCompanyId(getProxy().getId());
        claimRequest.setCreatorUserId(Database.getInstance().getCurrentUser().getId());
        claimRequest = Database.getTable(ClaimRequest.class).getRefreshed(claimRequest);
        claimRequest.save();
        return claimRequest;
    }

    public List<SubmittedDocument> getSubmittedDocuments(){
        return documentedModelProxy.getSubmittedDocuments();
    }

}
