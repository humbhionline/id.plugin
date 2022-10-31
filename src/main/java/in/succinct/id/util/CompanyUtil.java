package in.succinct.id.util;

import com.venky.core.collections.SequenceSet;
import com.venky.core.util.ObjectUtil;
import com.venky.swf.db.Database;
import com.venky.swf.db.model.reflection.ModelReflector;
import com.venky.swf.path._IPath;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;
import com.venky.swf.plugins.security.db.model.Role;
import com.venky.swf.pm.DataSecurityFilter;
import com.venky.swf.routing.Config;
import com.venky.swf.sql.Expression;
import com.venky.swf.sql.Operator;
import com.venky.swf.sql.Select;
import in.succinct.id.db.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class CompanyUtil {
    public static Company getCompany(){
        Long id = getCompanyId();
        return Database.getTable(Company.class).get(id);
    }

    public static Long getCompanyId(){
        String domainName = getFQDomainName();
        List<Company> companies = new ArrayList<>();
        if (!ObjectUtil.isVoid(domainName)){
            companies = new Select().from(Company.class).
                    where(new Expression(ModelReflector.instance(Company.class).getPool(),
                            "DOMAIN_NAME", Operator.LK, "%"+getFQDomainName()+"%")).execute();
        }

        if (companies.isEmpty()){
            com.venky.swf.db.model.User sessionUser = Database.getInstance().getCurrentUser();

            if (sessionUser != null){
                User user = sessionUser.getRawRecord().getAsProxy(User.class);
                if (user.getCompanyId() != null){
                    companies.add(user.getCompany().getRawRecord().getAsProxy(Company.class));
                }
            }
        }
        if (companies.isEmpty()){
            companies = new Select().from(Company.class).execute(2);
            if (companies.size() > 1){
                companies.clear();
            }
        }
        SequenceSet<Long> companyIds = DataSecurityFilter.getIds(companies);
        Long companyId = null;
        if (!companyIds.isEmpty()){
            companyId = companyIds.get(0);
        }
        return companyId;
    }
    public static String getFQDomainName(){
        _IPath path = Database.getInstance().getContext(_IPath.class.getName());
        String domainName = null;
        if (path == null){
            domainName = Config.instance().getProperty("swf.host","");
        }else {
            domainName = path.getRequest().getServerName();
        }
        return getFQDomainName(domainName);
    }
    public static String getFQDomainName(String domainName) {
        List<String> domainParts = new ArrayList<>();
        StringTokenizer tok = new StringTokenizer(domainName,".");
        while (tok.hasMoreTokens()){
            domainParts.add(tok.nextToken());
        }
        while (domainParts.size() > 2){
            domainParts.remove(0);
        }
        StringBuilder fQdomainName = new StringBuilder();
        for (String part: domainParts){
            if(fQdomainName.length() > 0){
                fQdomainName.append(".");
            }
            fQdomainName.append(part);
        }
        return fQdomainName.toString();

    }
    
    public static List<User> getAdminUsers() {

        Role role = Role.getRole("ADMIN");
        if (role != null){
            return role.getUserRoles().stream().map(ur->ur.getUser().getRawRecord().getAsProxy(User.class)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }



}
