package labvantage.custom.alcon.actions;


import sapphire.SapphireException;
import sapphire.action.AddToDoListEntry;
import sapphire.action.BaseAction;
import sapphire.action.SendMail;
import sapphire.util.DataSet;
import sapphire.xml.PropertyList;

/**
 * $Author: BAGCHAN1 $
 * $Date: 2022-02-08 00:16:36 -0500 (Tue, 08 Feb 2022) $
 * $Revision: 13 $
 */

/**
 * $Revision: 13 $
 * This class is used for POM failed emailing.
 * <p>
 * $Author: BAGCHAN1 $
 * $Date: 2022-02-08 00:16:36 -0500 (Tue, 08 Feb 2022) $
 * $Revision: 13 $
 */

public class POMFailedEmailNotification extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 13 $";

    public static final String ID = "POMFailedEmailNotification";
    public static final String VERSIONID = "1";

    private static String EMAIL_ADDRESS = "email_to";
    private static String EMAIL_FROM = "email_from";
    private static String EMAIL_SUBJECT = "email_subject";
    private static String EMAIL_MESSAGE = "email_body";

    @Override
    public void processAction(PropertyList properties) throws SapphireException {
        logger.info("------------- Processing Action:" + ID + ", Version:" + VERSIONID + " --------------");
        String retTrans[] = new String[2];
        // Checking Mandatory Fields
        checkMandatoryFields(properties, EMAIL_ADDRESS, EMAIL_SUBJECT, EMAIL_MESSAGE);
        //Reading Action Properties
        String toAddress = properties.getProperty(EMAIL_ADDRESS);
        String fromAddress = properties.getProperty(EMAIL_FROM);
        String subject = properties.getProperty(EMAIL_SUBJECT);
        String body = properties.getProperty(EMAIL_MESSAGE);

        //Getting Default From Address from System Admin --> System Configuration --> Miscellenious Option
        // If From Field is Left Blank. Special Handling
        if ("".equalsIgnoreCase(fromAddress)) {
            fromAddress = getConnectionProcessor().getConfigProperty("com.labvantage.sapphire.server.emailfromaddress");
        }
        // Adding default Body Content to mail Content.
        StringBuilder mailContent = new StringBuilder().append(body);
        // Getting Failed Transaction Ids (If Any)
        retTrans = checkFailedPOMTrans();
        if (!"N".equalsIgnoreCase(retTrans[0])) {
            // Splitting ';' separated POM Ids.
            String[] strPOMIds = retTrans[1].split(";");
            String[] creationDate = retTrans[2].split(";");
            // Looping through POM Ids.
            for (int pomId = 0; pomId < strPOMIds.length; pomId++) {
                // If POM Id is not blank then Adding POM Ids to Mail Body.
                if (!"".equalsIgnoreCase(strPOMIds[pomId])) {
                    mailContent.append("\r\n").append("-").append(strPOMIds[pomId]).append(" Created On: ").append(creationDate[pomId]).append(" US/Central");
                }
            }
            // Calling SendMail
            sendMail(toAddress, fromAddress, subject, mailContent.toString());
        }

        logger.info("------------- End Processing Action:" + ID + ", Version:" + VERSIONID + " --------------");
    }

    private void checkMandatoryFields(PropertyList inputProps, String... colNames) throws SapphireException {
        logger.info("Processing " + ID + ". (Action) : Inside checkMandatoryFields (method)");
        String colValue = "";
        StringBuilder errMsg = new StringBuilder().append(" Error:::");
        // Looping through Property List and checking mandatory SendMail columns are Null or Blank
        for (int colNo = 0; colNo < colNames.length; colNo++) {
            colValue = inputProps.getProperty(colNames[colNo]);
            if (null == colValue || "".equalsIgnoreCase(colValue)) {
                errMsg.append(" Blank or Null value found.").append(" For field:").append(colNames[colNo]).append("\n");
                logger.error(errMsg.toString());
                throw new SapphireException(errMsg.toString());
            }
        }
    }

    private void sendMail(String toAddress, String fromAddress, String subject, String body) {
        // Call OOB SendEMAIL here
        logger.info("Processing " + ID + ". (Action) : Inside sendEmail (method)");
        PropertyList plProps = new PropertyList();
        plProps.setProperty(AddToDoListEntry.PROPERTY_ACTIONID, SendMail.ID);
        plProps.setProperty(AddToDoListEntry.PROPERTY_ACTIONVERSIONID, SendMail.VERSIONID);
        plProps.setProperty(SendMail.PROPERTY_TO, toAddress);
        plProps.setProperty(SendMail.PROPERTY_FROM, fromAddress);
        plProps.setProperty(SendMail.PROPERTY_SUBJECT, subject);
        plProps.setProperty(SendMail.PROPERTY_MESSAGE, body);

        try {
            logger.info("Processing Action ." + ID + " : AddToDoListEntry (Action) " + plProps.toJSONString());
            getActionProcessor().processAction(AddToDoListEntry.ID, AddToDoListEntry.VERSIONID, plProps, true);
            logger.info("Processed Action ." + ID + " : AddToDoListEntry (Action) " + plProps.toJSONString());
        } catch (SapphireException ex) {
            logger.error("Error while processing SendMail action" + "\n" + ex);
        }
    }

    private String[] checkFailedPOMTrans() throws SapphireException {
        logger.info("Processing " + ID + ". (Action) : Inside checkFailedPOMTrans (method)");
        String returnVal[] = new String[3];
        StringBuilder errMsg = new StringBuilder().append("");
        String sqlText = "select u_poms_interfaceid,TO_CHAR(createdt,'dd-Mon-yyyy hh:mi am') creationdate from u_poms_interface where poms_flag is null OR poms_flag='' OR poms_ax_flag='' OR poms_ax_flag is null order by createdt desc";
        // 1. Query Will go here and Store it in DataSet.
        DataSet dsPOMSData = getQueryProcessor().getSqlDataSet(sqlText);
        if (null == dsPOMSData) {
            // 2. Check DataSet is Null or Not then Throw Exception.
            throw new SapphireException(errMsg.append(" Error::: Null DataSet found while checking values for POM Transactions. ").toString());
        } else if (dsPOMSData.getRowCount() == 0) {
            // 3. Check DataSet for blank then No Action.
            returnVal[0] = "N";
            returnVal[1] = " No Record Found!!!";
            returnVal[2] = " No Record Found!!!";
        } else {
            // 4. Has Correct Value, Returning the POM Ids
            returnVal[0] = "Y";
            returnVal[1] = dsPOMSData.getColumnValues("u_poms_interfaceid", ";");
            returnVal[2] = dsPOMSData.getColumnValues("creationdate", ";");
        }

        return returnVal;
    }
}
