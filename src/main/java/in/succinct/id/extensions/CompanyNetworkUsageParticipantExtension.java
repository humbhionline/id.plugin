package in.succinct.id.extensions;

import com.venky.swf.plugins.collab.extensions.participation.CompanySpecificParticipantExtension;
import in.succinct.id.db.model.onboarding.company.CompanyNetworkDomain;
import in.succinct.id.db.model.onboarding.company.CompanyNetworkUsage;

public class CompanyNetworkUsageParticipantExtension extends CompanySpecificParticipantExtension<CompanyNetworkUsage> {
    static {
        registerExtension(new CompanyNetworkUsageParticipantExtension());
    }
}
