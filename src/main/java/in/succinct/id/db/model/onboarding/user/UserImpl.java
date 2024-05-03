package in.succinct.id.db.model.onboarding.user;

import com.venky.swf.db.table.ModelImpl;
import in.succinct.id.db.model.onboarding.company.Company;
import in.succinct.plugins.kyc.db.model.DocumentedModelProxy;
import in.succinct.plugins.kyc.db.model.submissions.Document;
import in.succinct.plugins.kyc.db.model.submissions.SubmittedDocument;

import java.util.List;

public class UserImpl extends ModelImpl<User> {
    public UserImpl(User user){
        super(user);
        documentedModelProxy = new DocumentedModelProxy<>(user);
    }
    DocumentedModelProxy<User> documentedModelProxy ;

    public List<SubmittedDocument> getSubmittedDocuments(){
        return documentedModelProxy.getSubmittedDocuments();
    }

}
