package in.succinct.id.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import in.succinct.id.db.model.onboarding.company.Company;
import in.succinct.id.db.model.onboarding.docs.SubmittedDocument;

import java.util.Collections;
import java.util.List;

public class SubmittedDocumentParticipantExtension extends ParticipantExtension<SubmittedDocument> {
    static {
        registerExtension(new SubmittedDocumentParticipantExtension());
    }
    @Override
    public List<Long> getAllowedFieldValues(User user, SubmittedDocument partiallyFilledModel, String fieldName) {
        if (ObjectUtil.equals(fieldName,"DOCUMENT_MODEL_ID")){
            in.succinct.id.db.model.onboarding.user.User u = user.getRawRecord().getAsProxy(in.succinct.id.db.model.onboarding.user.User.class);
            if (ObjectUtil.equals(partiallyFilledModel.getDocumentedModelName(),User.class.getSimpleName())){
                if (u.isStaff()) {
                    return  null;
                }else {
                    return Collections.singletonList(u.getId());
                }
            }else if (ObjectUtil.equals(partiallyFilledModel.getDocumentedModelName(), Company.class.getSimpleName())){
                return u.getCompanyIds();
            }
        }
        return null;
    }
}
