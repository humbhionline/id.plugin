package in.succinct.id.extensions;

import com.venky.core.util.ObjectUtil;
import com.venky.swf.plugins.collab.db.model.config.City;
import com.venky.swf.plugins.collab.db.model.config.Country;
import com.venky.swf.plugins.collab.db.model.config.PinCode;
import com.venky.swf.plugins.collab.db.model.config.State;
import in.succinct.id.db.model.onboarding.user.User;
import in.succinct.id.util.AadharEKyc;
import in.succinct.id.util.AadharEKyc.AadharData;
import in.succinct.plugins.kyc.db.model.VerifiableDocument;
import in.succinct.plugins.kyc.db.model.submissions.SubmittedDocument;
import in.succinct.plugins.kyc.extensions.VerifiableDocumentExtension;

import java.sql.Date;

public class BeforeValidateUserDocument extends VerifiableDocumentExtension<SubmittedDocument> {
    static {
        registerExtension(new BeforeValidateUserDocument());
    }

    @Override
    public void beforeValidate(SubmittedDocument document) {
        if (!ObjectUtil.equals("User",document.getDocumentedModelName())){
            return;
        }
        super.beforeValidate(document);
        User user = document.extractDocumentedModel();

        if (document.getDocument().getDocumentName().equals("AADHAR")){
            if (!document.getVerificationStatus().equals(VerifiableDocument.APPROVED) && document.getFileContentSize()  > 0 && document.getFileContentName().endsWith(".zip")){
                try {
                    AadharData data = AadharEKyc.getInstance().parseZip(document.getFile(),document.getPassword());
                    if (data != null){
                        if (!ObjectUtil.isVoid(user.getPhoneNumber()) ){
                            user.setPhoneNumberVerified(data.isValidPhone(user.getPhoneNumber()));
                        }

                        if (!ObjectUtil.isVoid(user.getEmail())){
                            user.setEmailVerified(data.isValidEmail(user.getEmail()));
                        }

                        user.setLongName(data.get(AadharEKyc.AadharData.NAME));
                        user.setDateOfBirth(new Date(data.getDateOfBirth().getTime()));
                        user.setDateOfBirthVerified(true);
                        user.setAddressLine1(data.get(AadharEKyc.AadharData.HOUSE));
                        user.setAddressLine2(data.get(AadharEKyc.AadharData.STREET));
                        user.setAddressLine3(data.get(AadharEKyc.AadharData.LOCALITY));
                        user.setAddressLine4(data.get(AadharEKyc.AadharData.POST_OFFICE));
                        user.setCountryId(Country.findByName("India").getId());
                        State state = State.findByCountryAndName(user.getCountryId(), data.get(AadharEKyc.AadharData.STATE));
                        if (state != null) {
                            user.setStateId(state.getId());
                        }
                        City city = City.findByStateAndName(user.getStateId(), data.get(AadharEKyc.AadharData.DISTRICT));
                        if (city == null) {
                            city = City.findByStateAndName(user.getStateId(), data.get(AadharEKyc.AadharData.LOCALITY));
                        }
                        if (city != null) {
                            user.setCityId(city.getId());
                        }


                        PinCode pinCode = PinCode.find(data.get(AadharEKyc.AadharData.PIN_CODE));
                        if (pinCode != null) {
                            user.setPinCodeId(pinCode.getId());
                        }

                        user.setAddressVerified(true);
                        user.setTxnProperty("being.verified",true);
                        user.save();
                        document.setVerificationStatus(VerifiableDocument.APPROVED);
                        document.setTxnProperty("being.verified",true);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                    //Config.instance().getLogger(getClass().getName()).log(Level.WARNING,"Failed Aadhar Validation", e);
                }
            }
        }

    }
}
