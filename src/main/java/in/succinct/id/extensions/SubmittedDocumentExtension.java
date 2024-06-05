package in.succinct.id.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ModelOperationExtension;
import in.succinct.id.db.model.onboarding.docs.SubmittedDocument;

public class SubmittedDocumentExtension extends ModelOperationExtension<SubmittedDocument> {
    static {
        registerExtension(new SubmittedDocumentExtension());
    }

    @Override
    protected void beforeValidate(SubmittedDocument instance) {
        if (ObjectUtil.equals("Company",instance.getDocumentedModelName())){
            instance.setCompanyId(instance.getDocumentedModelId());
            instance.setUserId(null);
        }else if (ObjectUtil.equals("User",instance.getDocumentedModelName())){
            instance.setUserId(instance.getDocumentedModelId());
            instance.setCompanyId(null);
        }
    }
}
