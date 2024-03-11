package in.succinct.id.extensions;

import com.venky.swf.db.extensions.BeforeModelValidateExtension;
import com.venky.swf.db.extensions.ModelOperationExtension;
import in.succinct.id.db.model.onboarding.company.CompanyDocument;
import in.succinct.id.extensions.SubmittedDocumentExtension.KycInspector;

public class CompanyDocumentExtension extends ModelOperationExtension<CompanyDocument> {
    static {
        registerExtension(new CompanyDocumentExtension());
    }
    @Override
    public void beforeValidate(CompanyDocument model) {
        if (model.getRawRecord().isFieldDirty("REQUIRED_FOR_KYC")) {
            KycInspector.submitInspection();
        }
    }

    @Override
    protected void beforeDestroy(CompanyDocument instance) {
        KycInspector.submitInspection();
    }


}
