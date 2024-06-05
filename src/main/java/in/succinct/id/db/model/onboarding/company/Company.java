package in.succinct.id.db.model.onboarding.company;

import com.venky.swf.db.annotations.column.COLUMN_DEF;
import com.venky.swf.db.annotations.column.COLUMN_SIZE;
import com.venky.swf.db.annotations.column.UNIQUE_KEY;
import com.venky.swf.db.annotations.column.defaulting.StandardDefault;
import com.venky.swf.db.annotations.column.pm.PARTICIPANT;
import com.venky.swf.db.annotations.column.ui.HIDDEN;
import com.venky.swf.db.annotations.column.ui.PROTECTION;
import com.venky.swf.db.annotations.column.ui.PROTECTION.Kind;
import com.venky.swf.db.annotations.model.MENU;
import in.succinct.plugins.kyc.db.model.DocumentedModel;

import java.util.List;

public interface Company extends com.venky.swf.plugins.collab.db.model.participants.admin.Company , DocumentedModel {

    public static final String[] DEFAULT_DOCUMENTS = new String[]{"Company Registration","Company Tax Identifier","Previous Year Tax filing"};

    @UNIQUE_KEY("SUBSCRIBER")
    public String getSubscriberId();
    public void setSubscriberId(String subscriberId);


    @HIDDEN
    public List<ClaimRequest> getClaimRequests();

    public List<CompanyNetworkDomain> getCompanyNetworkDomains(); //Domains/
    public List<CompanyNetworkUsage> getCompanyNetworkUsages(); // Roles


    public ClaimRequest claim();
}
