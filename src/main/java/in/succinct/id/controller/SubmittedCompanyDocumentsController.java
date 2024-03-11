package in.succinct.id.controller;

import com.venky.swf.path.Path;
import in.succinct.id.db.model.onboarding.company.SubmittedCompanyDocument;

public class SubmittedCompanyDocumentsController extends VerifiableDocumentsController<SubmittedCompanyDocument> {
    public SubmittedCompanyDocumentsController(Path path) {
        super(path);
    }

}
