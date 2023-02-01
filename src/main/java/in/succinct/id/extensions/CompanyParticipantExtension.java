package in.succinct.id.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ParticipantExtension;
import in.succinct.id.db.model.Company;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompanyParticipantExtension extends ParticipantExtension<Company> {
    static {
        registerExtension(new CompanyParticipantExtension());
    }
    @Override
    protected List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user, Company partial , String fieldName) {
        if (ObjectUtil.equals(fieldName,"ADMIN_ID")){
            return Collections.singletonList(user.getId());
        }
        return new ArrayList<>();
    }
}
