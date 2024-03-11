package in.succinct.id.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ModelOperationExtension;
import com.venky.swf.db.model.Model;
import com.venky.swf.exceptions.AccessDeniedException;
import in.succinct.id.db.model.onboarding.VerifiableDocument;

public class VerifiableDocumentExtension<M extends VerifiableDocument & Model> extends ModelOperationExtension<M> {
    @Override
    public void beforeValidate(M document) {
        if (!ObjectUtil.equals(true,document.getTxnProperty("being.verified")) && document.getRawRecord().isFieldDirty("VERIFICATION_STATUS") && !document.getVerificationStatus().equals(VerifiableDocument.PENDING)){
             throw new AccessDeniedException();
        }
    }
}
