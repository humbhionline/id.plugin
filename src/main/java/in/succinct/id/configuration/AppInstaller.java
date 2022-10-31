package in.succinct.id.configuration;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.configuration.Installer;
import com.venky.swf.db.Database;
import com.venky.swf.plugins.collab.db.model.config.Role;
import com.venky.swf.plugins.collab.db.model.participants.admin.Company;


import com.venky.swf.sql.Select;
import in.succinct.id.db.model.DefaultUserRoles;
import in.succinct.id.db.model.DocumentType;
import in.succinct.id.db.model.User;

import javax.print.Doc;
import java.util.List;

public class AppInstaller implements Installer {

    public void install() {
        installRoles();
        installDocumentTypes();

    }
    public void installRoles(){
        if (Database.getTable(Role.class).isEmpty()) {
            for (String allowedRole : DefaultUserRoles.ALLOWED_ROLES) {
                Role role = Database.getTable(Role.class).newRecord();
                role.setName(allowedRole);
                if (!ObjectUtil.equals(allowedRole,DefaultUserRoles.ALLOWED_ROLES[0])){
                    role.setStaff(true);
                }
                role.save();
            }
        }
        Role admin = com.venky.swf.plugins.security.db.model.Role.getRole(Role.class,"ADMIN");
        if (!admin.isStaff()){
            admin.setStaff(true);
            admin.save();
        }
    }

    public void installDocumentTypes(){
        if (Database.getTable(DocumentType.class).isEmpty()){
            for (String defaultDocumentType : DocumentType.DEFAULT_DOCUMENT_TYPES) {
                DocumentType documentType = Database.getTable(DocumentType.class).newRecord();
                documentType.setName(defaultDocumentType);
                documentType.save();
            }
        }
    }
}

