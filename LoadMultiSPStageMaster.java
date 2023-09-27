package labvantage.custom.alcon.actions;

import sapphire.SapphireException;
import sapphire.action.AddSDI;
import sapphire.action.BaseAction;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.xml.PropertyList;
import sapphire.xml.PropertyListCollection;
/**
 * @author Kaushik Ghosh
 * @version 1
 * $Author: BAGCHAN1 $
 * $Date: 2022-02-08 00:16:36 -0500 (Tue, 08 Feb 2022) $
 * $Revision: 13 $
 */

/********************************************************************************************
 * $Revision: 13 $
 * Description: This class is used to sync Multiple SP stage SDC with Policy.
 *******************************************************************************************/

public class LoadMultiSPStageMaster extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 13 $";
    public static final String ID = "LoadMultiSPStageMaster";
    public static final String VERSIONID = "1";

    private static final String __MULTIPLE_SP_STAGE_SDCID = "MultipleSPStage";
    private static final String __PROPS_POLICY = "MultipleSampleStagePolicy";
    private static final String __PROPS_POLICY_NODE = "Level";
    private static final String __PROPS_POLICY_PROPERTY_COLLECTION = "stagescollection";
    private static final String __PROPS_STAGE = "stage";
    private static final String __PROPS_MULTIPLE_STAGE_ID = "u_multiplespstageid";
    private static final String __PROPS_SOURCE_LABEL = "sourcelabel";
    private static final String __PROPS_LEVEL = "level";
    private static final String __PROPS_LEVEL_ID = "levelid";

    @Override
    public void processAction(PropertyList properties) throws SapphireException {
        logger.debug("=============== Start Processing Action: " + ID + ", Version:" + VERSIONID + "===============");
        syncMultiplSPStageSDCWithPolicy();
        logger.debug("=============== End Processing Action: " + ID + ", Version:" + VERSIONID + "===============");
    }

    /******************************
     * This method is used sync Multiple SP Stage details from Policy to SDC
     * @throws SapphireException
     ******************************/
    private void syncMultiplSPStageSDCWithPolicy() throws SapphireException {
        logger.info("Processing " + ID + ". (Action) : Inside syncMultiplSPStageSDCWithPolicy (method)");
        // 1. Getting Sampling Plan Stage data form Policy
        DataSet dsPropertTree = getDataSetFromPolicy();
        // 2. Getting new multiple SP stage details
        DataSet dsNewMultipleSPStageDetails = getNewMultipleSPStageDetailsDS(dsPropertTree);
        // 3. Adding new rows to Multiple SP Stage SDC
        if (dsNewMultipleSPStageDetails.getRowCount() > 0) {
            enterNewSPStageToSDC(dsNewMultipleSPStageDetails);
        }
    }

    /****************************
     * This method is used to add new rows from policy to SDC
     * @param dsNewMultipleSPStageDetails
     * @throws SapphireException
     ****************************/
    private void enterNewSPStageToSDC(DataSet dsNewMultipleSPStageDetails) throws SapphireException {
        logger.info("Processing " + ID + ". (Action) : Inside enterNewSPStageToSDC (method)");
        PropertyList plAddSDI = new PropertyList();
        plAddSDI.setProperty(AddSDI.PROPERTY_SDCID, __MULTIPLE_SP_STAGE_SDCID);
        plAddSDI.setProperty(AddSDI.PROPERTY_COPIES, String.valueOf(dsNewMultipleSPStageDetails.getRowCount()));
        plAddSDI.setProperty(__PROPS_MULTIPLE_STAGE_ID, dsNewMultipleSPStageDetails.getColumnValues(__PROPS_STAGE, ";"));
        plAddSDI.setProperty(__PROPS_LEVEL_ID, dsNewMultipleSPStageDetails.getColumnValues(__PROPS_LEVEL, ";"));
        plAddSDI.setProperty(__PROPS_SOURCE_LABEL, dsNewMultipleSPStageDetails.getColumnValues(__PROPS_SOURCE_LABEL, ";"));
        getActionProcessor().processAction(AddSDI.ID, AddSDI.VERSIONID, plAddSDI);
    }

    /***************************
     * This method is used to get data from Policy
     * @return
     * @throws SapphireException
     ***************************/
    private DataSet getDataSetFromPolicy() throws SapphireException {
        logger.info("Processing " + ID + ". (Action) : Inside getDataSetFromPolicy (method)");
        DataSet dsMultiplSPPolicy = new DataSet(connectionInfo);
        String strStage = "";
        String strLevel = "";
        String strSourceLabel = "";
        int newRow = 0;
        // Adding columns to new DataSet
        dsMultiplSPPolicy.addColumn(__PROPS_STAGE, DataSet.STRING);
        dsMultiplSPPolicy.addColumn(__PROPS_LEVEL, DataSet.STRING);
        dsMultiplSPPolicy.addColumn(__PROPS_SOURCE_LABEL, DataSet.STRING);
        // Getting policy node
        PropertyList plMultipleSPStagePolicy = getConfigurationProcessor().getPolicy(__PROPS_POLICY, __PROPS_POLICY_NODE);
        // Getting property collection
        PropertyListCollection plBatchStageCollection = plMultipleSPStagePolicy.getCollection(__PROPS_POLICY_PROPERTY_COLLECTION);
        // Looping through property collection
        for (int row = 0; row < plBatchStageCollection.size(); row++) {
            // Getting SP details
            strStage = plBatchStageCollection.getPropertyList(row).getProperty(__PROPS_STAGE);
            strLevel = plBatchStageCollection.getPropertyList(row).getProperty(__PROPS_LEVEL);
            strSourceLabel = plBatchStageCollection.getPropertyList(row).getProperty(__PROPS_SOURCE_LABEL);
            // Adding rows to new DataSet
            newRow = dsMultiplSPPolicy.addRow();
            // Adding records to new DataSet
            dsMultiplSPPolicy.setValue(newRow, __PROPS_STAGE, strStage);
            dsMultiplSPPolicy.setValue(newRow, __PROPS_LEVEL, strLevel);
            dsMultiplSPPolicy.setValue(newRow, __PROPS_SOURCE_LABEL, strSourceLabel);
        }

        return dsMultiplSPPolicy;
    }

    /**************************************
     * This method is used to get new rows to be inserted.
     * @param dsExistingMultipleSPStage
     * @return
     * @throws SapphireException
     **************************************/
    private DataSet getNewMultipleSPStageDetailsDS(DataSet dsExistingMultipleSPStage) throws SapphireException {
        logger.info("Processing " + ID + ". (Action) : Inside getNewMultipleSPStageDetailsDS (method)");
        DataSet dsNewMultipleSPStageDetails = new DataSet(connectionInfo);
        // Filter for DataSet
        PropertyList plFilter = new PropertyList();
        String strStage = "";
        String strLevel = "";
        String strSourceLabel = "";
        // Getting Stage details from SDC
        DataSet dsMultipleSPStageFromSDC = getMultipleSPStageDetailsFromSDC();
        // Check if there are some stage details in Multiple SP Stage SDC
        if (dsMultipleSPStageFromSDC.getRowCount() > 0) {
            // Looping through Policy Multiple SP Stage Details DataSet
            for (int row = 0; row < dsExistingMultipleSPStage.getRowCount(); row++) {
                // Fetching Stage details from Policy DataSet
                strStage = dsExistingMultipleSPStage.getValue(row, __PROPS_STAGE, "");
                strLevel = dsExistingMultipleSPStage.getValue(row, __PROPS_LEVEL, "");
                strSourceLabel = dsExistingMultipleSPStage.getValue(row, __PROPS_SOURCE_LABEL, "");
                // Creating filter
                plFilter.setProperty(__PROPS_MULTIPLE_STAGE_ID, strStage);
                plFilter.setProperty(__PROPS_LEVEL_ID, strLevel);
                plFilter.setProperty(__PROPS_SOURCE_LABEL, strSourceLabel);
                // Check if NOT found in SDC Multiple SP Stage details DataSet
                if (dsMultipleSPStageFromSDC.findRow(plFilter) == -1) {
                    // Copy the row in new DataSet
                    dsNewMultipleSPStageDetails.copyRow(dsExistingMultipleSPStage, row, 1);
                }
            }
        } else {
            // If no row found in Multiple Stage SDC the return all rows from policy
            dsNewMultipleSPStageDetails = dsExistingMultipleSPStage;
        }

        return dsNewMultipleSPStageDetails;

    }

    /******************************
     * This method is used to get all Multiple SP Stage details from SDC
     * @return
     * @throws SapphireException
     ******************************/
    private DataSet getMultipleSPStageDetailsFromSDC() throws SapphireException {
        logger.info("Processing " + ID + ". (Action) : Inside getMultipleSPStageDetailsFromSDC (method)");
        String sqlText = "SELECT " + __PROPS_MULTIPLE_STAGE_ID + ", " + __PROPS_LEVEL_ID + ", " + __PROPS_SOURCE_LABEL + " FROM u_multiplespstage";
        DataSet dsMultipleSPStage = getQueryProcessor().getSqlDataSet(sqlText);
        if (null == dsMultipleSPStage) {
            throw new SapphireException("General Error:", ErrorDetail.TYPE_FAILURE, " Aborting transaction. Null DataSet found while executing query sql: \n" + sqlText);
        }
        return dsMultipleSPStage;
    }
}
