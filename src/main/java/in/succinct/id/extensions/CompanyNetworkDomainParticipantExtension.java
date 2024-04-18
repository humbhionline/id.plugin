package in.succinct.id.extensions;

import com.venky.swf.plugins.collab.extensions.participation.CompanySpecificParticipantExtension;
import in.succinct.id.db.model.onboarding.company.CompanyNetworkDomain;

public class CompanyNetworkDomainParticipantExtension extends CompanySpecificParticipantExtension<CompanyNetworkDomain> {
    static {
        registerExtension(new CompanyNetworkDomainParticipantExtension());
    }
}
