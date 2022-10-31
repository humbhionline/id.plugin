package in.succinct.id.extensions;

import com.venky.swf.db.Database;
import com.venky.swf.db.extensions.BeforeModelDestroyExtension;
import in.succinct.id.db.model.Company;
import in.succinct.id.db.model.CompanyAdministrator;

public class BeforeDestroyCompanyAdministrator extends BeforeModelDestroyExtension<CompanyAdministrator> {

    static {
        registerExtension(new BeforeDestroyCompanyAdministrator());
    }
    @Override
    public void beforeDestroy(CompanyAdministrator model) {
        Company company = Database.getTable(Company.class).lock(model.getCompanyId());

        if (Database.getTable(CompanyAdministrator.class).recordCount() <= 1 ){
            throw new RuntimeException("Company must have at least one administrator !");
        }
    }
}
