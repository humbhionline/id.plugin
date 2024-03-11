package in.succinct.id.db.model.onboarding.user;

import com.venky.swf.db.annotations.column.IS_NULLABLE;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.model.Model;
import in.succinct.id.db.model.onboarding.VerifiableDocument;


public interface SubmittedUserDocument extends Model, VerifiableDocument {

    @PARTICIPANT
    public Long getUserId();
    public void setUserId(Long id);
    public User getUser();

    @IS_NULLABLE(false)
    public Long getDocumentTypeId();
    public void setDocumentTypeId(Long id);
    public UserDocument getDocumentType();

}
