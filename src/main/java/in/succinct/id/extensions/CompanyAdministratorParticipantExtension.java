package in.succinct.id.extensions;

import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.db.model.User;
import in.succinct.id.db.model.CompanyAdministrator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CompanyAdministratorParticipantExtension extends ParticipantExtension<CompanyAdministrator> {
    static {
        registerExtension(new CompanyParticipantExtension());
    }
    @Override
    protected List<Long> getAllowedFieldValues(User user, CompanyAdministrator partiallyFilledModel, String fieldName) {
        if (fieldName.equals("COMPANY_ID")){
            return user.getRawRecord().getAsProxy(in.succinct.id.db.model.User.class).getCompanyAdministrators().stream().map(ca->ca.getCompanyId()).collect(Collectors.toList());
        }else if (fieldName.equals("USER_ID")) {
            if (partiallyFilledModel.getCompanyId() > 0){
                return partiallyFilledModel.getCompany().getUserEmails().stream().map(ue->ue.getUserId()).collect(Collectors.toList());
            }else {
                return new ArrayList<>();
            }
        }
        return null;
    }
}
