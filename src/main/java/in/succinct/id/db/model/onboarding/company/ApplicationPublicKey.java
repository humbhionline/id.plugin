package in.succinct.id.db.model.onboarding.company;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.ui.PROTECTION;

public interface ApplicationPublicKey extends com.venky.swf.plugins.collab.db.model.participants.ApplicationPublicKey {
    @COLUMN_DEF(StandardDefault.BOOLEAN_FALSE)
    @PROTECTION
    public boolean isVerified();
    public void setVerified(boolean verified);

    public void verify(boolean async);
}
