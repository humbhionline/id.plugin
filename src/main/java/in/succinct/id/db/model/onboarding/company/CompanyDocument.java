package in.succinct.id.db.model.onboarding.company;

import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.model.MENU;
import com.venky.swf.db.model.Model;

import java.util.List;

@MENU("Documents")
public interface CompanyDocument extends Model {
    @UNIQUE_KEY
    public String getName();
    public void setName(String name);

    public boolean isRequiredForKyc();
    public void setRequiredForKyc(boolean requiredForKyc);


    @HIDDEN
    public List<SubmittedCompanyDocument> getSubmittedDocuments();
}
