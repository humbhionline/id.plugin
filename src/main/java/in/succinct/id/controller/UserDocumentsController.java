package in.succinct.id.controller;

import com.venky.swf.path.Path;
import in.succinct.id.db.model.UserDocument;

public class UserDocumentsController extends VerifiableDocumentsController<UserDocument> {
    public UserDocumentsController(Path path) {
        super(path);
    }

}
