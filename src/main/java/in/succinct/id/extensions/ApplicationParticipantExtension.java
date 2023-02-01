package in.succinct.id.extensions;

import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import com.venky.swf.plugins.collab.db.model.participants.Application;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApplicationParticipantExtension extends ParticipantExtension<Application> {
    static {
        registerExtension(new ApplicationParticipantExtension());
    }
    @Override
    protected List<Long> getAllowedFieldValues(User user, Application partiallyFilledModel, String fieldName) {

        if (fieldName.equals("ADMIN_ID")){
            return Collections.singletonList(user.getId());
        }
        return new ArrayList<>();
    }
}
