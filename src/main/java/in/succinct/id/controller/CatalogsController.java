package in.succinct.id.controller;

import com.venky.cache.Cache;
import com.venky.core.string.StringUtil;
import com.venky.core.util.MultiException;
import com.venky.core.util.ObjectUtil;
import com.venky.geo.GeoCoordinate;
import com.venky.swf.controller.VirtualModelController;
import com.venky.swf.controller.annotations.RequireLogin;
import com.venky.swf.db.Database;
import com.venky.swf.db.annotations.column.ui.mimes.MimeType;
import com.venky.swf.db.model.application.ApplicationUtil;
import com.venky.swf.db.model.io.xls.JsonAttributeSetter;
import com.venky.swf.db.model.io.xls.XLSModelReader;
import com.venky.swf.exceptions.AccessDeniedException;
import com.venky.swf.integration.api.Call;
import com.venky.swf.integration.api.HttpMethod;
import com.venky.swf.integration.api.InputFormat;
import com.venky.swf.path.Path;
import com.venky.swf.plugins.collab.db.model.participants.admin.Facility;
import com.venky.swf.plugins.collab.db.model.user.User;
import com.venky.swf.routing.Config;
import com.venky.swf.views.View;
import in.succinct.beckn.Address;
import in.succinct.beckn.BecknStrings;
import in.succinct.beckn.Categories;
import in.succinct.beckn.Category;
import in.succinct.beckn.Category.CategoryCode;
import in.succinct.beckn.Circle;
import in.succinct.beckn.City;
import in.succinct.beckn.Contact;
import in.succinct.beckn.Context;
import in.succinct.beckn.Country;
import in.succinct.beckn.Descriptor;
import in.succinct.beckn.Fulfillment;
import in.succinct.beckn.Fulfillment.RetailFulfillmentType;
import in.succinct.beckn.FulfillmentStop;
import in.succinct.beckn.Fulfillments;
import in.succinct.beckn.Image;
import in.succinct.beckn.Images;
import in.succinct.beckn.Item;
import in.succinct.beckn.Items;
import in.succinct.beckn.Location;
import in.succinct.beckn.Locations;
import in.succinct.beckn.Message;
import in.succinct.beckn.Payment;
import in.succinct.beckn.Payment.CollectedBy;
import in.succinct.beckn.PaymentType;
import in.succinct.beckn.Payments;
import in.succinct.beckn.Person;
import in.succinct.beckn.Price;
import in.succinct.beckn.Provider;
import in.succinct.beckn.Providers;
import in.succinct.beckn.Request;
import in.succinct.beckn.Scalar;
import in.succinct.beckn.Subscriber;
import in.succinct.beckn.Subscribers;
import in.succinct.id.core.db.model.onboarding.company.Application;
import in.succinct.id.core.db.model.onboarding.company.Company;
import in.succinct.id.db.model.Catalog;
import in.succinct.id.db.model.onboarding.catalog.DeliveryRule;
import in.succinct.id.db.model.onboarding.catalog.Product;
import in.succinct.id.db.model.onboarding.company.ApplicationPublicKey;
import in.succinct.id.util.LookupManager;
import in.succinct.onet.core.adaptor.NetworkAdaptor;
import in.succinct.onet.core.adaptor.NetworkAdaptorFactory;
import in.succinct.onet.core.api.BecknIdHelper;
import in.succinct.onet.core.api.BecknIdHelper.Entity;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.simple.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.logging.Level;

public class CatalogsController extends VirtualModelController<Catalog> {

    public CatalogsController(Path path) {
        super(path);
    }
    private static Address getAddress(String name, com.venky.swf.plugins.collab.db.model.participants.admin.Address facility) {
        Address address = new Address();
        address.setState(facility.getState().getName());
        address.setName(name);
        address.setPinCode(facility.getPinCode().getPinCode());
        address.setCity(facility.getCity().getCode());
        address.setCountry(facility.getCountry().getIsoCode());
        address.setDoor(facility.getAddressLine1());
        address.setBuilding(facility.getAddressLine2());
        address.setStreet(facility.getAddressLine3());
        address.setLocality(facility.getAddressLine4());

        return address;
    }

    public NetworkAdaptor getNetworkAdaptor(){
        return NetworkAdaptorFactory.getInstance().getAdaptor(Config.instance().getProperty("in.succinct.onet.name","beckn_open"));
    }
    @RequireLogin(value = false)
    public View activate(long facilityId) {
        return ingest(facilityId);
    }
    @RequireLogin(value = false)
    public View deactivate(long facilityId) {
        return ingest(facilityId);
    }
    @RequireLogin(value = false)
    public View ingest(long facilityId) {
        if (getReturnIntegrationAdaptor() == null){
            throw new RuntimeException("Only json api is supported.!");
        }
        NetworkAdaptor networkAdaptor = getNetworkAdaptor();
        Providers providers = networkAdaptor.getObjectCreator("").create(Providers.class);
        User user = getSessionUser();
        Facility facility = Database.getTable(Facility.class).get(facilityId);
        if (!facility.isAccessibleBy(user)){
            throw new AccessDeniedException();
        }

        Company company = facility.getCompany().getRawRecord().getAsProxy(Company.class);
        Subscriber subscriber = new Subscriber(){{
            setSubscriberId(company.getSubscriberId());
            setCity(facility.getCity().getCode());
            setCountry(facility.getCountry().getIsoCode());
        }};


        Provider provider = providers.getObjectCreator().create(Provider.class);
        provider.setId(company.getSubscriberId());
        provider.setTag("kyc","tax_id",company.getTaxIdentificationNumber());
        provider.setTag("kyc","registration_id",company.getRegistrationNumber());


        providers.add(provider);
        {
            Descriptor descriptor = provider.getObjectCreator().create(Descriptor.class);
            provider.setDescriptor(descriptor);
            if (!ObjectUtil.isVoid(company.getTaxIdentificationNumber())) {
                descriptor.setCode(company.getTaxIdentificationNumber());
            }
            descriptor.setName(company.getName());
            descriptor.setLongDesc(company.getName());
        }
        {
            Locations locations = provider.getObjectCreator().create(Locations.class);
            Location location = locations.getObjectCreator().create(Location.class);
            location.setGps(new GeoCoordinate(facility));
            location.setCountry(new Country(){{
                setName(facility.getCountry().getName());
                setCode(facility.getCountry().getIsoCode());
            }});
            location.setCity(new City(){{
                setName(facility.getCity().getName());
                setCode(facility.getCity().getCode());
            }});
            location.setId(BecknIdHelper.getBecknId(StringUtil.valueOf(facilityId),subscriber,Entity.provider_location));
            location.setAddress(CatalogsController.getAddress(facility.getName(),facility));
            location.setDescriptor(new Descriptor(){{
                setName(facility.getName());
            }});
            locations.add(location);
            provider.setLocations(locations);
        }
        {
            Payments payments = provider.getObjectCreator().create(Payments.class);
            Payment cod = provider.getObjectCreator().create(Payment.class);
            cod.setCollectedBy(CollectedBy.BPP);
            cod.setType(PaymentType.POST_FULFILLMENT);
            cod.setId("COD");
            payments.add(cod);
            Payment prepaid = provider.getObjectCreator().create(Payment.class);
            prepaid.setCollectedBy(CollectedBy.BPP);
            prepaid.setType(PaymentType.PRE_FULFILLMENT);
            prepaid.setId("PRE-PAID");
            payments.add(prepaid);
            provider.setPayments(payments);
        }
        {
            Fulfillments fulfillments = provider.getObjectCreator().create(Fulfillments.class);
            Fulfillment home_delivery = provider.getObjectCreator().create(Fulfillment.class);
            home_delivery.setId(RetailFulfillmentType.home_delivery.toString());
            home_delivery.setType(RetailFulfillmentType.home_delivery.toString());

            Fulfillment store_pickUp = provider.getObjectCreator().create(Fulfillment.class);
            store_pickUp.setId(RetailFulfillmentType.store_pickup.toString());
            store_pickUp.setType(RetailFulfillmentType.store_pickup.toString());

            fulfillments.add(home_delivery);fulfillments.add(store_pickUp);
            provider.setFulfillments(fulfillments);
        }


        List<Catalog> catalogs = getIntegrationAdaptor().readRequest(getPath());
        if (catalogs.isEmpty()){
            throw new RuntimeException("No Catalog uploaded");
        }else if (catalogs.size() > 1){
            throw new RuntimeException("Upload one catalog file at a time.");
        }
        BecknStrings locationIds = new BecknStrings(){{
            for (Location location : provider.getLocations()) {
                add(location.getId());
            }
        }};


        BecknStrings fulfillmentIds = new BecknStrings(){{
            for (Fulfillment fulfillment : provider.getFulfillments()){
                add(fulfillment.getId());
            }
        }};

        BecknStrings paymentIds = new BecknStrings(){{
            for (Payment payment : provider.getPayments()){
                add(payment.getId());
            }
        }};

        MultiException multiException = new MultiException();
        for (Catalog catalog : catalogs){
            InputStream inputStream = catalog.getFile();
            try {
                Workbook book = new XSSFWorkbook(inputStream);
                JSONObject root = provider.getInner();

                for (int i = 0 ; i < book.getNumberOfSheets(); i ++){
                    Sheet sheet = book.getSheetAt(i);
                    importSheet(sheet,provider); // In Network format
                }
                //provider.setInner(root);
                for (Item item : provider.getItems()) {
                    item.setLocationIds(locationIds);
                    item.setFulfillmentIds(fulfillmentIds);
                    item.setPaymentIds(paymentIds);
                }
                for (Fulfillment fulfillment : provider.getFulfillments()){
                    if (fulfillment.getContact() == null){
                        fulfillment.setContact(new Contact());
                    }
                    fulfillment.getContact().setEmail(facility.getEmail());
                    fulfillment.getContact().setPhone(facility.getPhoneNumber());
                    fulfillment.setProviderId(provider.getId());
                    fulfillment.setProviderName(provider.getDescriptor().getName());
                    fulfillment.setTracking(false);
                    if (fulfillment.getStart() != null){
                        fulfillment.getStart().setContact(fulfillment.getContact());
                        fulfillment.getStart().setPerson(new Person(){{
                            setName(user.getLongName());
                        }});
                    }

                }

                Request request = prepareCatalogSyncRequest(providers, subscriber,networkAdaptor);
                request.setPayload(request.getInner().toString());

                Subscribers gateways = LookupManager.getInstance().lookup(new Subscriber() {{
                    //setSubscriberId(networkAdaptor.getSearchProviderId());
                    setType(Subscriber.SUBSCRIBER_TYPE_BG);
                }}, null);

                Application self = ApplicationUtil.find(Config.instance().getHostName()).getRawRecord().getAsProxy(Application.class);
                Map<String, ApplicationPublicKey> latestKeys = LookupManager.getInstance().getLatestKeys(self.getRawRecord().getAsProxy(in.succinct.id.core.db.model.onboarding.company.Application.class));



                if (latestKeys != null) {
                    Map<String,String> headers = new HashMap<>() {{
                        put("content-type", MimeType.APPLICATION_JSON.toString());
                        put("Authorization", request.generateAuthorizationHeader(self.getAppId(), latestKeys.get(ApplicationPublicKey.PURPOSE_SIGNING).getKeyId()));
                    }};
                    for (Subscriber gwSubscriber : gateways) {
                        if (!ObjectUtil.equals(gwSubscriber.getStatus(),Subscriber.SUBSCRIBER_STATUS_SUBSCRIBED)){
                            continue;
                        }
                        //Propagate to all gateways. Bubbling code in bg was removed in favour of catalog gateways
                        //It is not in the commercial interests of bg to propagate to other bgs.
                        Call<InputStream> call  = new Call<InputStream>().url(gwSubscriber.getSubscriberUrl(), "on_search").headers(headers).inputFormat(InputFormat.INPUT_STREAM).method(HttpMethod.POST).input(new ByteArrayInputStream(request.toString().getBytes(StandardCharsets.UTF_8)));
                        if (!call.hasErrors()) {
                            Config.instance().getLogger(getClass().getName()).log(Level.INFO, "BroadCasted on_search to bg " + gwSubscriber.getSubscriberId() + ":\n"
                                                                                              + StringUtil.valueOf(call.getResponseStream()));
                        }
                    }
                }
            } catch (IOException e) {
                multiException.add(e);
            }

        }
        if (!multiException.isEmpty()) {
            throw multiException;
        }else {
            return getReturnIntegrationAdaptor().createStatusResponse(getPath(),null,"Catalog update queued!");
        }
    }
    public Request prepareCatalogSyncRequest(Providers providers,  Subscriber subscriber, NetworkAdaptor networkAdaptor){
        Request request = networkAdaptor.getObjectCreator(subscriber.getDomain()).create(Request.class);
        Context context = request.getObjectCreator().create(Context.class);
        request.setContext(context);
        request.setMessage(context.getObjectCreator().create(Message.class));
        request.getMessage().setCatalog(request.getObjectCreator().create(in.succinct.beckn.Catalog.class));
        request.getMessage().getCatalog().setProviders(providers);
        context.setBppId(subscriber.getSubscriberId());
        context.setBppUri(subscriber.getSubscriberUrl());
        context.setTransactionId(UUID.randomUUID().toString());
        context.setMessageId(UUID.randomUUID().toString());
        context.setDomain(subscriber.getDomain());// will go as null.
        context.setCountry(subscriber.getCountry());
        context.setCoreVersion(networkAdaptor.getCoreVersion());
        context.setTimestamp(new Date());
        context.setNetworkId(networkAdaptor.getId());
        context.setCity(subscriber.getCity());
        context.setTtl(60);
        context.setAction("on_search");

        for (in.succinct.beckn.Provider provider : providers){
            if (getPath().action().equals("ingest")) {
                provider.setTag("general_attributes", "catalog.indexer.reset", "Y");
            }else {
                provider.setTag("general_attributes", "catalog.indexer.reset", "N");
                provider.setTag("general_attributes","catalog.indexer.operation",getPath().action());
            }
        }

        return request;

    }

    public void importProducts(List<Product> products,Provider provider){
        NetworkAdaptor networkAdaptor = getNetworkAdaptor();
        Items items = provider.getItems();
        if (items == null){
            items = provider.getObjectCreator().create(Items.class);
            provider.setItems(items);
        }

        for (Product  product : products){
            Item item = networkAdaptor.getObjectCreator("").create(Item.class);
            item.setId(product.getProductId());
            item.setDescriptor(item.getObjectCreator().create(Descriptor.class));
            item.getDescriptor().setName(product.getProductName());
            item.getDescriptor().setShortDesc(product.getProductDescription());
            item.getDescriptor().setLongDesc(product.getProductDescription());

            Images images = item.getObjectCreator().create(Images.class);
            Image image = item.getObjectCreator().create(Image.class);
            image.setUrl(product.getImageUrl());
            images.add(image);
            item.getDescriptor().setImages(images);
            item.setPrice(item.getObjectCreator().create(Price.class));
            Price price = item.getPrice();
            price.setMaximumValue(product.getMaxRetailPrice());
            price.setListedValue(product.getMaxRetailPrice());
            price.setOfferedValue(product.getSellingPrice());
            price.setValue(product.getSellingPrice());
            price.setCurrency(com.venky.swf.plugins.collab.db.model.config.Country.findByName(provider.getLocations().get(0).getCountry().getCode()).getWorldCurrency().getCode());

            StringTokenizer tokenizer = new StringTokenizer(StringUtil.valueOf(product.getKeywords()),",");
            item.setCategoryIds(new BecknStrings());
            if (provider.getCategories() == null ){
                provider.setCategories(new Categories());
            }
            Categories categories = provider.getCategories();
            while (tokenizer.hasMoreTokens()){
                String token  = tokenizer.nextToken().trim();
                item.getCategoryIds().add(token);
                if (categories.get(token) == null){
                    Category category = provider.getObjectCreator().create(Category.class);
                    category.setId(token);
                    category.setDescriptor(provider.getObjectCreator().create(Descriptor.class));
                    Descriptor descriptor = category.getDescriptor();
                    descriptor.setName(token);
                    descriptor.setCode(token);
                    descriptor.setShortDesc(token);
                    descriptor.setLongDesc(token);
                    categories.add(category);
                }
            }
            item.setTag("DOMAIN","CATEGORY","BUY_MOVABLE_GOODS");
            items.add(item);
        }

    }
    public void importDeliveryRules(List<DeliveryRule> deliveryRules,Provider provider){
        Fulfillment fulfillment  = provider.getFulfillments().get(RetailFulfillmentType.home_delivery.toString());
        if (deliveryRules.isEmpty()){
            if (fulfillment != null) {
                provider.getFulfillments().remove(fulfillment);
            }
        }else if (deliveryRules.size() > 1){
            throw new RuntimeException("Cannot have multiple delivery rules");
        }else {
            DeliveryRule deliveryRule = deliveryRules.get(0);
            fulfillment.setTag("APPLICABILITY", "MIN_ORDER_VALUE",deliveryRule.getMinOrderValue());
            fulfillment.setTag("APPLICABILITY", "MAX_DISTANCE",deliveryRule.getMaxDistance());
            fulfillment.setTag("APPLICABILITY", "MAX_WEIGHT",deliveryRule.getMaxWeight());

            fulfillment.setTag("DELIVERY_CHARGES", "CHARGES_PER_KM",deliveryRule.getChargesPerKm());
            fulfillment.setTag("DELIVERY_CHARGES", "MIN_DISTANCE_CHARGED",deliveryRule.getMinDistanceCharged());

            fulfillment.setStart(new FulfillmentStop() {{
                setLocation(provider.getLocations().get(0));
                if (deliveryRule.getMaxDistance() > 0) {
                    getLocation().setCircle(new Circle());
                    getLocation().getCircle().setGps(getLocation().getGps());
                    getLocation().getCircle().setRadius(new Scalar() {{
                        setValue(Database.getJdbcTypeHelper("").getTypeRef(double.class).getTypeConverter().valueOf(deliveryRule.getMaxDistance()));
                        setUnit("Km");
                    }});
                }
            }});

        }


    }
    public void importSheet(Sheet sheet, final Provider root){
        String sheetName = sheet.getSheetName();
        if (sheetName.startsWith("_")){
            return;
        }

        if (sheetName.equals("Product")) {
            XLSModelReader<Product> xlsModelReader = getXLSModelReader(Product.class);
            List<Product> products = xlsModelReader.read(sheet);
            importProducts(products,root);
            return;
        }else if (sheetName.equals("DeliveryRule")){
            XLSModelReader<DeliveryRule> xlsModelReader = getXLSModelReader(DeliveryRule.class);
            List<DeliveryRule> rules = xlsModelReader.read(sheet);
            importDeliveryRules(rules,root);
            return;
        }

        Map<String, JsonAttributeSetter> setterMap = new Cache<>(0,0){
            @Override
            protected JsonAttributeSetter getValue(String s) {
                JsonAttributeSetter attributeSetter = new JsonAttributeSetter(s);
                attributeSetter.setJsonAware(root.getInner());
                return attributeSetter;
            }
        };

        Row heading = null;
        int rowCount = -1;
        for (Row row : sheet){
            if (heading == null){
                heading = row;
                continue;
            }
            rowCount ++;
            for (int i  = 0  ; i < heading.getLastCellNum() ; i ++){
                Cell headingCell = heading.getCell(i);
                Cell recordCell = row.getCell(i);
                String header = String.format("%s[%d].%s",StringUtil.pluralize(sheetName).toLowerCase(),rowCount,headingCell.getStringCellValue());
                setterMap.get(header).set(recordCell);
            }
        }
    }


}
