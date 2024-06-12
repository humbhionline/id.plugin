package in.succinct.id.controller;

import com.venky.swf.db.Database;
import com.venky.swf.path.Path;
import com.venky.swf.views.View;
import in.succinct.id.db.model.onboarding.company.ApplicationPublicKey;

public class ApplicationPublicKeysController extends com.venky.swf.plugins.collab.controller.ApplicationPublicKeysController {
    public ApplicationPublicKeysController(Path path) {
        super(path);
    }

    @Override
    public View verify(long id) {
        ApplicationPublicKey key = Database.getTable(ApplicationPublicKey.class).get(id);
        key.verify(false);
        return show(key);
    }
}
