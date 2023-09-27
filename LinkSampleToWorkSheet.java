package labvantage.custom.alcon.actions;

import sapphire.SapphireException;
import sapphire.action.AddWorksheetSDI;
import sapphire.action.BaseAction;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.util.StringUtil;
import sapphire.xml.PropertyList;
import java.util.HashMap;

/**
 * This action is used to Link samples from the Worksheet.
 * $Author: kumarvi4 $
 * $Date: 2023-07-13 17:49:23 +0530 (Thu, 13 Jul 2023) $
 * $Revision: 538 $
 */
public class LinkSampleToWorkSheet extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 538 $";
    public static final String ACTION_ID = "LinkSampleToWorkSheet";
    private static final String _SEMICOLON_SEPERATOR = ";";
    private static final String _QUOTES_COMMA_QUOTES_SEPERATOR = "','";

    /**
     * Link Sample to the Worksheet
     *
     * @param properties PropertyList
     * @throws SapphireException
     */
    @Override
    public void processAction(PropertyList properties) throws SapphireException {

        String worksheetid = properties.getProperty("worksheetid");
        String worksheetversionid = properties.getProperty("worksheetversionid");
        String sdcid = properties.getProperty("sdcid");
        String keyid1 = properties.getProperty("keyid1"); // semicolon separated value
        String keyid2 = properties.getProperty("keyid2");  // semicolon separated value
        String keyid3 = properties.getProperty("keyid3");  // semicolon separated value
        String controllist = properties.getProperty("controllist"); //semicolon separated value
        controllist = "".equals(controllist) ? "DataEntryControl;ReagentsControl;AttachmentControl" : controllist;

        // Add samples to worksheet AddWorksheetSDI
        if ("".equals(worksheetid) && "".equals(worksheetversionid)) {
            String errMessage = "Worksheet Id not found. Sample can't be linked.";
            throw new SapphireException(ErrorDetail.TYPE_VALIDATION, ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(errMessage));
        } else {
            // Check if keyid1 (Sample Ids) have status other than Cancelled, Completed, Reviewed
            String validSample = getValidSampleStatus(keyid1);
            if ("".equals(validSample)) {
                throw new SapphireException(ErrorDetail.TYPE_VALIDATION, ErrorDetail.TYPE_FAILURE,
                        getTranslationProcessor().translate("Invalid Sample Status. Only Initial, InProgress and Received Samples can be linked to the WorkSheet."));
            } else {
                // Execute AddWorksheetSDI Action
                addWorksheetSDI(worksheetid, worksheetversionid, sdcid, keyid1, "(null)", "(null)");
                // find worksheetitem for which the sample should be added
                HashMap<String, String> hmWSI = findWorksheetItems(worksheetid, worksheetversionid, controllist);
                String[] worksheetitemid = hmWSI.get("worksheetitemid").split(_SEMICOLON_SEPERATOR); // semicolon separated value
                String[] worksheetitemversionid = hmWSI.get("worksheetitemversionid").split(_SEMICOLON_SEPERATOR); // semicolon separated value
                // Execute AddWorksheetItemSDI Action
                for (int k = 0; k < worksheetitemid.length; k++) {
                    addWorksheetItemSDI(worksheetid, worksheetversionid, worksheetitemid[k], worksheetitemversionid[k], sdcid, keyid1);
                }
                // Add Workitem of Sample to Worksheetsdi if not added
                // find workitem of the sample and check if it is available in worksheetsdi
                String workitemid = getSDIWorkItem(keyid1).getColumnValues("workitemid", _SEMICOLON_SEPERATOR);
                String workitemversionid = getSDIWorkItem(keyid1).getColumnValues("workitemversionid", _SEMICOLON_SEPERATOR);
                String sdiworkitemid = getSDIWorkItem(keyid1).getColumnValues("sdiworkitemid", _SEMICOLON_SEPERATOR);
                // get the workitem from the worksheet and if it availble don't add it
                String[] arrWorkitemid = workitemid.split(_SEMICOLON_SEPERATOR);
                String[] arrWorkitemVersionid = workitemversionid.split(_SEMICOLON_SEPERATOR);
                HashMap<String, String> hmFilter = new HashMap<>();
                String workitemIdToAdd = "";
                String workitemVersionToAdd = "";
                for (int i = 0; i < arrWorkitemid.length; i++) {
                    hmFilter.put("keyid1", arrWorkitemid[i]);
                    hmFilter.put("keyid2", arrWorkitemVersionid[i]);
                    DataSet dsWorkItemFromWorksheet = getWorkItemFromWorksheet(worksheetid, worksheetversionid);
                    DataSet dsFilter = dsWorkItemFromWorksheet.getFilteredDataSet(hmFilter);
                    if (dsFilter.getRowCount() == 0) { // not found add it
                        workitemIdToAdd += _SEMICOLON_SEPERATOR + dsWorkItemFromWorksheet.getColumnValues("keyid1", _SEMICOLON_SEPERATOR);
                        workitemVersionToAdd += _SEMICOLON_SEPERATOR + dsWorkItemFromWorksheet.getColumnValues("keyid2", _SEMICOLON_SEPERATOR);
                    }
                }
                workitemIdToAdd = !"".equals(workitemIdToAdd) ? workitemIdToAdd.substring(1) : workitemIdToAdd;
                workitemVersionToAdd = !"".equals(workitemVersionToAdd) ? workitemVersionToAdd.substring(1) : workitemVersionToAdd;
                //Add workitem to worksheetsdi
                if (!"".equals(workitemIdToAdd) && workitemIdToAdd.split(_SEMICOLON_SEPERATOR).length > 0) {
                    addWorkItemToWorksheet(worksheetid, worksheetversionid, workitemIdToAdd, workitemVersionToAdd);
                }
                // Add SDIWorkItem of Sample to worksheetsdi
                if (!"".equals(sdiworkitemid) && sdiworkitemid.split(_SEMICOLON_SEPERATOR).length > 0) {
                    addSDIWorkItemToWorksheet(worksheetid, worksheetversionid, sdiworkitemid);
                }
            }
        }
    }

    /**
     * Get Samples which are in status of Initial,InProgress,Received
     *
     * @param keyid1 String
     * @return validSampleIds String
     */
    private String getValidSampleStatus(String keyid1) {
        String[] arrKeyid1 = StringUtil.split(keyid1, _SEMICOLON_SEPERATOR);
        String validSampleIds = "";
        String sqlQuestionmarkParam = StringUtil.repeat("?", arrKeyid1.length, ",");
        String sqlSampleStatus = "select s_sampleid from s_sample where  samplestatus  is not null and samplestatus IN ('Initial','InProgress','Received')" +
                " and s_sampleid in (" + sqlQuestionmarkParam + ")";
        DataSet dsSample = getQueryProcessor().getPreparedSqlDataSet(sqlSampleStatus, arrKeyid1);
        if (dsSample != null && dsSample.getRowCount() > 0) {
            validSampleIds = dsSample.getColumnValues("s_sampleid", ";");
        }
        return validSampleIds;
    }

    /**
     * Add Workitem to the Worksheet
     *
     * @param worksheetid
     * @param worksheetversionid
     * @param workitemIdToAdd
     * @param workitemVersionToAdd
     * @throws SapphireException
     */
    private void addWorkItemToWorksheet(String worksheetid, String worksheetversionid, String workitemIdToAdd, String workitemVersionToAdd) throws SapphireException {
        addWorksheetSDI(worksheetid, worksheetversionid, "WorkItem", workitemIdToAdd, workitemVersionToAdd, "(null)");
    }

    /**
     * Add SDIWorkItem to the Worksheet
     *
     * @param worksheetid
     * @param worksheetversionid
     * @param sdiworkitemids
     * @throws SapphireException
     */
    private void addSDIWorkItemToWorksheet(String worksheetid, String worksheetversionid, String sdiworkitemids) throws SapphireException {
        addWorksheetSDI(worksheetid, worksheetversionid, "SDIWorkItem", sdiworkitemids, "(null)", "(null)");
    }

    /**
     * Get WorkItems for the given worksheetid and worksheetversionid
     *
     * @param worksheetid
     * @param worksheetversionid
     * @return
     */
    private DataSet getWorkItemFromWorksheet(String worksheetid, String worksheetversionid) {
        String sqlWS = "select keyid1,keyid2 from worksheetsdi where worksheetid=? and worksheetversionid=? and sdcid = 'WorkItem'";
        DataSet dsWS = getQueryProcessor().getPreparedSqlDataSet(sqlWS, new Object[]{worksheetid, worksheetversionid});
        return dsWS;
    }

    /**
     * Get SDIWorkItem for the given sample Ids
     *
     * @param keyid1
     * @return
     */
    private DataSet getSDIWorkItem(String keyid1) {
        String[] arrKeyid1 = StringUtil.split(keyid1, _SEMICOLON_SEPERATOR);
        String sqlQuestionmarkParam = StringUtil.repeat("?", arrKeyid1.length, ",");
        String sqlWI = " select sdiworkitemid, workitemid, workitemversionid from sdiworkitem where keyid1 IN (" + sqlQuestionmarkParam + ")";
        DataSet dsSDIWI = getQueryProcessor().getPreparedSqlDataSet(sqlWI, arrKeyid1);
        return dsSDIWI;
    }

    /**
     * Find WorksheetItems for worksheet/version and controllist
     *
     * @param worksheetid
     * @param worksheetversionid
     * @param controlList
     * @return
     * @throws SapphireException
     */
    private HashMap<String, String> findWorksheetItems(String worksheetid, String worksheetversionid, String controlList) throws SapphireException {
        DataSet dsWorkSheetItem;
        HashMap<String, String> hm = new HashMap<>();
        String sqlWorksheetItem = "select worksheetitemid , worksheetitemversionid " +
                "from worksheetitem where worksheetid=? and worksheetversionid = ? and propertytreeid in ('" + StringUtil.replaceAll(controlList, _SEMICOLON_SEPERATOR, _QUOTES_COMMA_QUOTES_SEPERATOR) + "')";
        dsWorkSheetItem = getQueryProcessor().getPreparedSqlDataSet(sqlWorksheetItem, new Object[]{worksheetid, worksheetversionid});
        if (dsWorkSheetItem == null) {
            throw new SapphireException("General Error: ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Failed to execute SQL Statement."));
        } else if (dsWorkSheetItem.getRowCount() > 0) {
            hm.put("worksheetitemid", dsWorkSheetItem.getColumnValues("worksheetitemid", _SEMICOLON_SEPERATOR));
            hm.put("worksheetitemversionid", dsWorkSheetItem.getColumnValues("worksheetitemversionid", _SEMICOLON_SEPERATOR));
        }
        return hm;
    }

    /**
     * Execute AddWorksheetItemSDI to add sample to worksheetitem
     *
     * @param worksheetid
     * @param worksheetversionid
     * @param worksheetitemid
     * @param worksheetitemversionid
     * @param sdcid
     * @param keyid1
     * @return
     * @throws SapphireException
     */
    private void addWorksheetItemSDI(String worksheetid, String worksheetversionid, String worksheetitemid, String worksheetitemversionid, String sdcid, String keyid1) throws SapphireException {
        HashMap<String, String> hmProps = new HashMap<>();
        hmProps.put("worksheetid", worksheetid);
        hmProps.put("worksheetversionid", worksheetversionid);
        hmProps.put("worksheetitemid", worksheetitemid);
        hmProps.put("worksheetitemversionid", worksheetitemversionid);
        hmProps.put("sdcid", sdcid);
        hmProps.put("keyid1", keyid1);
        try {
            getActionProcessor().processAction("AddWorksheetItemSDI", "1", hmProps);
        } catch (SapphireException se) {
            throw new SapphireException("General Error: ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(se.getMessage()));
        }
    }

    /**
     * Add sample to Worksheet
     *
     * @param worksheetid
     * @param worksheetversionid
     * @param sdcid
     * @param keyid1
     * @param keyid2
     * @param keyid3
     * @throws SapphireException
     */
    private void addWorksheetSDI(String worksheetid, String worksheetversionid, String sdcid, String keyid1, String keyid2, String keyid3) throws SapphireException {
        HashMap<String, String> hmProps = new HashMap<>();
        hmProps.put("worksheetid", worksheetid);
        hmProps.put("worksheetversionid", worksheetversionid);
        hmProps.put("sdcid", sdcid);
        hmProps.put("keyid1", keyid1);
        hmProps.put("keyid2", keyid2);
        hmProps.put("keyid3", keyid3);
        try {
            getActionProcessor().processAction(AddWorksheetSDI.ID, AddWorksheetSDI.VERSIONID, hmProps);
        } catch (SapphireException se) {
            throw new SapphireException("General Error: ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(se.getMessage()));
        }
    }
}