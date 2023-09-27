package labvantage.custom.alcon.actions;

import sapphire.SapphireException;
import sapphire.action.BaseAction;
import sapphire.action.DeleteWorksheetItemSDI;
import sapphire.action.DeleteWorksheetSDI;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.util.StringUtil;
import sapphire.xml.PropertyList;
import java.util.HashMap;

/**
 * This action is used to Unlinks samples from the Worksheet.
 * $Author: kumarvi4 $
 * $Date: 2023-07-13 17:49:23 +0530 (Thu, 13 Jul 2023) $
 * $Revision: 538 $
 */
public class UnLinkSampleToWorkSheet extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 538 $";
    public static final String ACTION_ID = "UnLinkSampleToWorkSheet";
    private static final String _SEMICOLON_SEPERATOR = ";";
    private static final String _QUOTES_COMMA_QUOTES_SEPERATOR = "','";

    /**
     * Unlink samples from the worksheet
     *
     * @param properties
     * @throws SapphireException
     */
    @Override
    public void processAction(PropertyList properties) throws SapphireException {
        String worksheetid = properties.getProperty("worksheetid");
        String worksheetversionid = properties.getProperty("worksheetversionid");
        String sdcid = properties.getProperty("sdcid");
        String keyid1 = properties.getProperty("keyid1");
        String keyid2 = properties.getProperty("keyid2");
        String keyid3 = properties.getProperty("keyid3");
        String controllist = properties.getProperty("controllist"); //semicolon seperated value
        controllist = "".equals(controllist) ? "DataEntryControl;ReagentsControl;AttachmentControl" : controllist;

        // Add samples to worksheet DeleteWorksheetSDI
        if ("".equals(worksheetid) || "".equals(worksheetversionid)) {
            String errMessage = "Worksheet Id/Version not found. Sample can't be linked.";
            throw new SapphireException(ErrorDetail.TYPE_VALIDATION, ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(errMessage));
        } else {
            // Execute AddWorksheetSDI Action
            deleteWorksheetSDI(worksheetid, worksheetversionid, sdcid, keyid1, "(null)", "(null)");
            // find worksheetitem for which the sample should be added
            HashMap<String, String> hmWSI = findWorksheetItems(worksheetid, worksheetversionid, controllist);
            String[] worksheetitemid = hmWSI.get("worksheetitemid").split(_SEMICOLON_SEPERATOR); // semicolon separated value
            String[] worksheetitemversionid = hmWSI.get("worksheetitemversionid").split(_SEMICOLON_SEPERATOR); // semicolon separated value
            // Execute AddWorksheetItemSDI Action
            for (int k = 0; k < worksheetitemid.length; k++) {
                deleteWorksheetItemSDI(worksheetid, worksheetversionid, worksheetitemid[k], worksheetitemversionid[k], sdcid, keyid1);
            }
            String sdiworkitemid = getSDIWorkItem(keyid1).getColumnValues("sdiworkitemid", _SEMICOLON_SEPERATOR);
            // Delete SDIWorkItem of Sample to worksheetsdi
            if (!"".equals(sdiworkitemid) && sdiworkitemid.split(_SEMICOLON_SEPERATOR).length > 0) {
                deleteSDIWorkItemToWorksheet(worksheetid, worksheetversionid, sdiworkitemid);
            }
        }
    }

    /**
     * Delete workitems to the worksheet
     *
     * @param worksheetid
     * @param worksheetversionid
     * @param sdiworkitemids
     * @throws SapphireException
     */
    private void deleteSDIWorkItemToWorksheet(String worksheetid, String worksheetversionid, String sdiworkitemids) throws SapphireException {
        deleteWorksheetSDI(worksheetid, worksheetversionid, "SDIWorkItem", sdiworkitemids, "(null)", "(null)");
    }

    /**
     * Get Workitem for the sample
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
     * Find worksheetitemid and version from the worksheetitem for given controllist
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
     * Delete samples from Worksheetitem
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
    private void deleteWorksheetItemSDI(String worksheetid, String worksheetversionid, String worksheetitemid, String worksheetitemversionid, String sdcid, String keyid1) throws SapphireException {
        HashMap<String, String> hmProps = new HashMap<>();
        hmProps.put("worksheetid", worksheetid);
        hmProps.put("worksheetversionid", worksheetversionid);
        hmProps.put("worksheetitemid", worksheetitemid);
        hmProps.put("worksheetitemversionid", worksheetitemversionid);
        hmProps.put("sdcid", sdcid);
        hmProps.put("keyid1", keyid1);
        try {
            getActionProcessor().processAction(DeleteWorksheetItemSDI.ID, DeleteWorksheetItemSDI.VERSIONID, hmProps);
        } catch (SapphireException se) {
            throw new SapphireException("General Error: ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(se.getMessage()));
        }
    }

    /**
     * Delete samples from worksheet
     *
     * @param worksheetid
     * @param worksheetversionid
     * @param sdcid
     * @param keyid1
     * @param keyid2
     * @param keyid3
     * @throws SapphireException
     */
    private void deleteWorksheetSDI(String worksheetid, String worksheetversionid, String sdcid, String keyid1, String keyid2, String keyid3) throws SapphireException {
        HashMap<String, String> hmProps = new HashMap<>();
        hmProps.put("worksheetid", worksheetid);
        hmProps.put("worksheetversionid", worksheetversionid);
        hmProps.put("sdcid", sdcid);
        hmProps.put("keyid1", keyid1);
        hmProps.put("keyid2", keyid2);
        hmProps.put("keyid3", keyid3);
        try {
            getActionProcessor().processAction(DeleteWorksheetSDI.ID, DeleteWorksheetSDI.VERSIONID, hmProps);
        } catch (SapphireException se) {
            throw new SapphireException("General Error: ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(se.getMessage()));
        }
    }
}
