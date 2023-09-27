package labvantage.custom.alcon.actions;

import sapphire.SapphireException;
import sapphire.action.AddToDoListEntry;
import sapphire.action.BaseAction;
import sapphire.action.GenerateReport;
import sapphire.xml.PropertyList;
/**
 * $Author: BAGCHAN1 $
 * $Date: 2022-04-20 01:39:24 +0530 (Wed, 20 Apr 2022) $
 * $Revision: 48 $
 */

/**
 * $Revision: 48 $
 * This class is used for Employee Report
 * <p>
 * $Author: BAGCHAN1 $
 * $Date: 2022-04-20 01:39:24 +0530 (Wed, 20 Apr 2022) $
 * $Revision: 48 $
 */

public class EmployeeReportAction extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 48 $";
    public static final String ID = "EmployeeReportAction";
    public static final String VERSIONID = "1";
    private static final String PROP_REPORT_ID = "reportid";
    private static final String PROP_REPORT_VERSIONID = "reportversionid";
    private static final String PROP_REPORT_PARAM1 = "STARTDATE";
    private static final String PROP_REPORT_PARAM2 = "ENDDATE";
    private static final String PROP_REPORT_PARAM3 = "EMPLOYEENUMBER";
    private static final String PROP_TO_ADDRESS = "emailtolist";
    private static final String PROP_EMAIL_SUBJECT = "emailsubject";
    private static final String PROP_EMAIL_MESSAGE = "emailmessage";
    private static final String PROP_DESTINATION = "destination";
    private static final String PROP_FILE_TYPE = "filetype";
    private static final String PROP_FILE_NAME = "filename";

    @Override
    public void processAction(PropertyList properties) throws SapphireException {
        logger.info("++++++++++ Start Processing Action: " + ID + " , Version: " + VERSIONID + " ++++++++++");
        // Checking Mandatory Fields
        checkMandatoryFields(properties, PROP_REPORT_ID, PROP_REPORT_VERSIONID, PROP_REPORT_PARAM1, PROP_REPORT_PARAM2, PROP_REPORT_PARAM3);
        // Reading Action Properties
        String reportID = properties.getProperty(PROP_REPORT_ID, "");
        String reportVersionID = properties.getProperty(PROP_REPORT_VERSIONID, "");
        String reportParam1 = properties.getProperty(PROP_REPORT_PARAM1, "").substring(0, 8).trim();
        String reportParam2 = properties.getProperty(PROP_REPORT_PARAM2, "").substring(0, 8).trim();
        String reportParam3 = properties.getProperty(PROP_REPORT_PARAM3, "");
        String reportDestination = properties.getProperty(PROP_DESTINATION, "");
        String reportFileType = properties.getProperty(PROP_FILE_TYPE, "");
        String reportFileName = properties.getProperty(PROP_FILE_NAME, "");
        String reportEmailFrom = getConnectionProcessor().getConfigProperty("com.labvantage.sapphire.server.emailfromaddress");
        String emailToList = properties.getProperty(PROP_TO_ADDRESS, "");
        String emailSubject = properties.getProperty(PROP_EMAIL_SUBJECT, "");
        String emailMessage = properties.getProperty(PROP_EMAIL_MESSAGE, "");

        // Calling method for generating Report.
        generateReport(reportID, reportVersionID, reportParam1, reportParam2, reportParam3, reportDestination, reportFileType, reportFileName, reportEmailFrom, emailToList, emailSubject, emailMessage);
        logger.info("++++++++++ End of Processing Action: " + ID + " , Version: " + VERSIONID + " ++++++++++");
    }

    /**
     * Adding Generate Report Action into ToDoList.
     *
     * @param reportId
     * @param reportVersion
     * @param reportParam1
     * @param reportParam2
     * @param reportParam3
     * @param reportDest
     * @param fileType
     * @param fileName
     * @param emailFrom
     * @param emailTo
     * @param subject
     * @param body
     * @throws SapphireException
     */
    private void generateReport(String reportId, String reportVersion, String reportParam1, String reportParam2, String reportParam3, String reportDest, String fileType, String fileName, String emailFrom, String emailTo, String subject, String body) throws SapphireException {
        logger.info("++++++++++ Processing Action: " + ID + " , inside  generateReport ++++++++++");
        PropertyList plGeneratereport = new PropertyList();
        plGeneratereport.setProperty(AddToDoListEntry.PROPERTY_ACTIONID, GenerateReport.ID);
        plGeneratereport.setProperty(AddToDoListEntry.PROPERTY_ACTIONVERSIONID, GenerateReport.VERSIONID);
        plGeneratereport.setProperty(GenerateReport.PROPERTY_REPORTID, reportId);
        plGeneratereport.setProperty(GenerateReport.PROPERTY_REPORTVERSIONID, reportVersion);
        plGeneratereport.setProperty(PROP_REPORT_PARAM1, reportParam1);
        //plGeneratereport.setProperty(PROP_REPORT_PARAM1, "11/1/20");
        plGeneratereport.setProperty(PROP_REPORT_PARAM2, reportParam2);
        //plGeneratereport.setProperty(PROP_REPORT_PARAM2, "11/15/20");
        plGeneratereport.setProperty(PROP_REPORT_PARAM3, reportParam3);
        //plGeneratereport.setProperty(GenerateReport.PROPERTY_PARAM3, "10273");
        //plGeneratereport.setProperty(GenerateReport.PROPERTY_DESTINATION, reportDest);
        //plGeneratereport.setProperty(GenerateReport.PROPERTY_DESTINATION, "file");
        plGeneratereport.setProperty(GenerateReport.PROPERTY_DESTINATION, "email");
        //plGeneratereport.setProperty(GenerateReport.PROPERTY_FILETYPE, fileType);
        //plGeneratereport.setProperty(GenerateReport.PROPERTY_FILENAME, "D:\\apps\\labvantage_1\\labvantagehome81\\generatedreport\\" + fileName + ".pdf");
        plGeneratereport.setProperty(GenerateReport.PROPERTY_EMAILFROM, emailFrom);
        plGeneratereport.setProperty(GenerateReport.PROPERTY_EMAILTOLIST, emailTo);
        plGeneratereport.setProperty(GenerateReport.PROPERTY_EMAILSUBJECT, subject);
        plGeneratereport.setProperty(GenerateReport.PROPERTY_EMAILMESSAGE, body);
        //plGeneratereport.setProperty(GenerateReport.PROPERTY_DEBUGLOG, "Y");
        // Adding TodoList
        getActionProcessor().processAction(AddToDoListEntry.ID, AddToDoListEntry.VERSIONID, plGeneratereport, true);
        // getActionProcessor().processAction(GenerateReport.ID, GenerateReport.VERSIONID, plGeneratereport,true);

    }

    /**
     * Check List of Mandatory Fields
     *
     * @param allFileds
     * @param mandateFields
     * @throws SapphireException
     */
    private void checkMandatoryFields(PropertyList allFileds, String... mandateFields) throws SapphireException {
        logger.info("++++++++++ Processing Action: " + ID + " , inside  checkMandatoryFields ++++++++++");
        String propValue = "";
        for (int propCount = 0; propCount < mandateFields.length; propCount++) {
            propValue = allFileds.getProperty(mandateFields[propCount]);
            if (null == propValue || ("").equalsIgnoreCase(propValue)) {
                throw new SapphireException(mandateFields[propCount] + " can't be Blank or Null.");
            }
        }
    }
}
