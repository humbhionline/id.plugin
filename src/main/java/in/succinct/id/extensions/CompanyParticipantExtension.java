package in.succinct.id.extensions;

import com.venky.core.collections.SequenceSet;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.extensions.ParticipantExtension;
import com.venky.swf.sql.Conjunction;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import in.succinct.id.db.model.onboarding.company.ClaimRequest;
import in.succinct.id.db.model.onboarding.company.Company;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CompanyParticipantExtension extends ParticipantExtension<Company> {
    static {
        registerExtension(new CompanyParticipantExtension());
    }
    @Override
    public List<Long> getAllowedFieldValues(com.venky.swf.db.model.User user, Company partial , String fieldName) {
        if (ObjectUtil.equals(fieldName,"SELF_COMPANY_ID")){
            SequenceSet<Long> ret = new SequenceSet<Long>();
            Select select = new Select().from(ClaimRequest.class);
            Expression where =  new Expression(select.getPool(), Conjunction.AND).
                    add(new Expression(select.getPool(),"CREATOR_ID", Operator.EQ,user.getId())).
                    add(new Expression(select.getPool(),"DOMAIN_VERIFIED", Operator.EQ,true));
            if (partial.getId() >0 ){
                where.add(new Expression(select.getPool(),"COMPANY_ID", Operator.EQ,partial.getId()));
            }

            select.where(where);
            List<ClaimRequest> requests = select.execute();
            ret.addAll(requests.stream().map(r->r.getCompanyId()).collect(Collectors.toList()));
            return ret;
        }
        return new ArrayList<>();
    }

}
