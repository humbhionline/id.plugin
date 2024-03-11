package in.succinct.id.controller;

import com.venky.swf.path.Path;
import in.succinct.id.db.model.onboarding.user.SubmittedUserDocument;

public class SubmittedUserDocumentsController extends VerifiableDocumentsController<SubmittedUserDocument> {
    public SubmittedUserDocumentsController(Path path) {
        super(path);
    }

}
