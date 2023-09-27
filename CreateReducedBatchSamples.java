//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

/**
 * @ Original Author: Labvantage
 * $Author: BAGCHAN1 $
 * $Date: 2022-02-08 00:16:36 -0500 (Tue, 08 Feb 2022) $
 * $Revision: 13 $
 */

package labvantage.custom.alcon.actions;

import labvantage.custom.alcon.util.AlconUtil;
import sapphire.SapphireException;
import sapphire.action.BaseAction;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.util.StringUtil;
import sapphire.xml.PropertyList;

import java.util.ArrayList;
import java.util.HashMap;

/***************************************************************************
 * $Revision: 13 $
 * Description: Create Reduced Batch Samples
 *
 * @author
 * @version 1
 *
 *******************************************************************************/
public class CreateReducedBatchSamples extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 13 $";
    public static final String ID = "CreateReducedBatchSamples";
    public static final String VERSIONID = "1";
    public static String PROPERTY_INPUT_BATCH_ID = "batchid";
    public static String PROPERTY_INPUT_BATCH_STATUS = "batchstatus";
    public static String __LEVEL_REDUCED = "Reduced";

    /**
     * @param properties
     * @throws SapphireException
     */
    public void processAction(PropertyList properties) throws SapphireException {
        String strBatchIDs = properties.getProperty("keyid1", "");
        String strBatchStatus = properties.getProperty("batchstatus", "");
        String strAuditReason = properties.getProperty("auditreason", "");
        String strSAPFlag = properties.getProperty("issapflag", "");
        String tempSAPCurrentUser = getConnectionProcessor().getConnectionInfo(getConnectionid()).getSysuserId();

        String strCurrentUser = "Y".equalsIgnoreCase(strSAPFlag) ? (tempSAPCurrentUser.equalsIgnoreCase("(system)") ? "SAP_INTERFACE" : tempSAPCurrentUser) : "";

        String strRSet = this.getDAMProcessor().createRSet("Batch", strBatchIDs, "", "");
        String strQuery = "select s_batch.s_batchid,s_batch.batchstatus from s_batch, rsetitems where s_batch.levelid is not null " +
                " and  rsetitems.sdcid='Batch' and rsetitems.keyid1=s_batch.s_batchid and  rsetitems.keyid2='(null)'  and  rsetitems.keyid3='(null)' " +
                " and rsetitems.rsetid = ? ";

        DataSet dsReducedBatchInformation = this.getQueryProcessor().getPreparedSqlDataSet(strQuery, new Object[]{strRSet});
        if (!"".equals(strRSet)) {
            this.getDAMProcessor().clearRSet(strRSet);
        }

        if (null == dsReducedBatchInformation) {
            throw new SapphireException("Unable to execute query for dataset dsReducedBatchInformation ..!\n Please contact System Administrator..!");
        }

        if (dsReducedBatchInformation.size() == 0) {
            this.receiveBatch(strBatchIDs, strBatchStatus, strAuditReason, strCurrentUser);
            return;
        }

        //------------------------------------------------------------------------------------------------------------------------//

        String[] strBatches = StringUtil.split(strBatchIDs, ";");
        String[] strBatchStatusARr = StringUtil.split(strBatchStatus, ";");

        for (int i = 0; i < strBatches.length; ++i) {
            HashMap hmFilter = new HashMap();
            hmFilter.put("s_batchid", strBatches[i]);
            if (dsReducedBatchInformation.findRow(hmFilter) == -1) {
                this.receiveBatch(strBatches[i], strBatchStatusARr[i], strAuditReason, strCurrentUser);
            }
        }

        //------------------------------------------------------------------------------------------------------------------------//

        String strQueryReduced = "select sb.productid,sb.productversionid,sdetail.countrule,sb.s_batchid,sstage.s_processstageid,sitem.itemsdcid,sitem.itemkeyid1,sitem.itemkeyid2 " +
                " ,sdetail.u_quantity,sdetail.u_quantityunit,sdetail.u_containertype,sdetail.defaultdepartmentid " +
                " ,sdetail.u_storagecondition,splan.s_samplingplanid,splan.s_samplingplanversionid,sdetail.levelid,sdetail.sourcelabel,sdetail.templatekeyid1 " +
                " from s_samplingplan splan  " +
                " inner join s_processstage sstage on splan.s_samplingplanid=sstage.s_samplingplanid and splan.s_samplingplanversionid=sstage.s_samplingplanversionid   " +
                " inner join s_spdetail sdetail on sdetail.s_samplingplanid=sstage.s_samplingplanid and sdetail.s_samplingplanversionid=sstage.s_samplingplanversionid and sstage.s_processstageid = sdetail.processstageid    " +
                " inner join s_spdetailitem sdetailitem on sdetailitem.s_samplingplanid=sdetail.s_samplingplanid and sdetailitem.s_samplingplanversionid=sdetail.s_samplingplanversionid and sdetailitem.s_samplingplandetailno=sdetail.s_samplingplandetailno" +
                " inner join s_spitem sitem on sitem.s_samplingplanid=sstage.s_samplingplanid and sitem.s_samplingplanversionid=sstage.s_samplingplanversionid and sitem.s_samplingplanitemno=sdetailitem.s_samplingplanitemno " +
                " inner join s_prodvariant svariant on svariant.samplingplanid=splan.s_samplingplanid " +
                " and (case " +
                "   when nvl(svariant.samplingplanversionid,'-1')='-1' " +
                "       then (select max(s_samplingplanversionid) from s_samplingplan where s_samplingplan.s_samplingplanid=splan.s_samplingplanid) " +
                "   when  nvl(svariant.samplingplanversionid,'-1')!='-1' " +
                "       then nvl(svariant.samplingplanversionid,'-1') end)=splan.s_samplingplanversionid " +
                " inner join s_batch sb on sb.prodvariantid=svariant.s_prodvariantid     " +
                " where  " +
                " sdetail.levelid='" + __LEVEL_REDUCED + "' and sb.s_batchid in ('" + dsReducedBatchInformation.getColumnValues("s_batchid", "','") + "') ";

        DataSet dsBatchStageInformation = this.getQueryProcessor().getPreparedSqlDataSet(strQueryReduced, new Object[0]);

        if (null == dsBatchStageInformation) {
            throw new SapphireException("Unable to execute Query for dataset dsBatchStageInformation ..!\n Please contact System Administrator..!");
        }

        if (dsBatchStageInformation.getRowCount() > 0) {
            dsBatchStageInformation.sort("s_batchid,s_processstageid");
            ArrayList arGroupArrayList = dsBatchStageInformation.getGroupedDataSets("s_batchid,s_processstageid");

            for (int i = 0; i < arGroupArrayList.size(); ++i) {
                DataSet dsGroup = (DataSet) arGroupArrayList.get(i);
                PropertyList plAddSDIBatchStage = new PropertyList();
                plAddSDIBatchStage.setProperty("sdcid", "LV_BatchStage");
                plAddSDIBatchStage.setProperty("processstageid", dsGroup.getValue(0, "s_processstageid"));
                plAddSDIBatchStage.setProperty("processstageinstance", "1");
                plAddSDIBatchStage.setProperty("label", dsGroup.getValue(0, "sourcelabel"));
                plAddSDIBatchStage.setProperty("copies", "1");
                plAddSDIBatchStage.setProperty("batchstagestatus", "Initial");
                plAddSDIBatchStage.setProperty("batchid", dsGroup.getValue(0, "s_batchid"));

                try {
                    //--- Action processing is handled in a special way  - original code was from Labvantage vendor.
                    //-- Devops has no clue about the reason why the transaction was marked true.
                    //-- For SAP-LIMS interface transaction has been marked as false, for other processes (adhoc batch creation) from GUI,  kept open as true.

                    boolean actionTransaction = true;
//                    TODO FOR RAW MATERIAL IMPLEMENTATION, OPEN THIS LINE, COMMENT OUT PREVIOUS LINE
//                    boolean actionTransaction = false; //Changed for RM SAP Interface

                    if ("Y".equalsIgnoreCase(strSAPFlag)) {
                        actionTransaction = false;
                    }
                    this.getActionProcessor().processAction("AddSDI", "1", plAddSDIBatchStage, actionTransaction);

                } catch (Exception e) {
                    throw new SapphireException("General_Error", "FAILURE", this.getTranslationProcessor().translate("Failed to call AddSDI for BatchStage. Please contact Administrator. " + e.getMessage()));
                }

                int inCount = Integer.valueOf(dsGroup.getValue(0, "countrule"));
                PropertyList plAddSample = new PropertyList();
                plAddSample.setProperty("sdcid", "Sample");
                plAddSample.setProperty("batchid", StringUtil.repeat(dsGroup.getValue(0, "s_batchid"), inCount, ";"));
                plAddSample.setProperty("batchstageid", StringUtil.repeat(plAddSDIBatchStage.getProperty("newkeyid1"), inCount, ";"));
                plAddSample.setProperty("copies", String.valueOf(inCount));
                plAddSample.setProperty("processstageinstance", StringUtil.repeat("1", inCount, ";"));
                plAddSample.setProperty("productid", StringUtil.repeat(dsGroup.getValue(0, "productid", ""), inCount, ";"));
                plAddSample.setProperty("productversionid", StringUtil.repeat(dsGroup.getValue(0, "productversionid", ""), inCount, ";"));
                plAddSample.setProperty("qcsampletype", StringUtil.repeat("Unknown", inCount, ";"));
                plAddSample.setProperty("u_otherdept", StringUtil.repeat(dsGroup.getValue(0, "defaultdepartmentid"), Integer.valueOf(inCount), ";"));
                plAddSample.setProperty("u_quantity", StringUtil.repeat(dsGroup.getValue(0, "u_quantity"), Integer.valueOf(inCount), ";"));
                plAddSample.setProperty("u_quantityunit", StringUtil.repeat(dsGroup.getValue(0, "u_quantityunit"), Integer.valueOf(inCount), ";"));
                plAddSample.setProperty("u_containertype", StringUtil.repeat(dsGroup.getValue(0, "u_containertype"), Integer.valueOf(inCount), ";"));
                plAddSample.setProperty("u_storagecondition", StringUtil.repeat(dsGroup.getValue(0, "u_storagecondition"), Integer.valueOf(inCount), ";"));
                plAddSample.setProperty("sourcespid", StringUtil.repeat(dsGroup.getValue(0, "s_samplingplanid"), Integer.valueOf(inCount), ";"));
                plAddSample.setProperty("sourcespversionid", StringUtil.repeat(dsGroup.getValue(0, "s_samplingplanversionid"), Integer.valueOf(inCount), ";"));
                plAddSample.setProperty("sourcesplevelid", StringUtil.repeat(dsGroup.getValue(0, "levelid"), Integer.valueOf(inCount), ";"));
                plAddSample.setProperty("sourcespsourcelabel", StringUtil.repeat(dsGroup.getValue(0, "sourcelabel"), Integer.valueOf(inCount), ";"));
                plAddSample.setProperty("securitydepartment", StringUtil.repeat(dsGroup.getValue(0, "defaultdepartmentid"), Integer.valueOf(inCount), ";"));

                try {
                    this.getActionProcessor().processAction("AddSDI", "1", plAddSample);
                } catch (SapphireException e) {
                    throw new SapphireException("General_Error", ErrorDetail.TYPE_FAILURE
                            , this.getTranslationProcessor().translate("Failed to add Sample to the batch. Please contact Administrator."));
                }

                //------------ Samples created --------------------//

                //------ Add Workitems to Sample ---//
                HashMap hmTestMethodFilter = new HashMap();
                hmTestMethodFilter.put("itemsdcid", "WorkItem");
                HashMap hmSpecFilter = new HashMap();
                hmSpecFilter.put("itemsdcid", "SpecSDC");
                String strReturnedMessage = "";
                DataSet dsTestMethod = dsGroup.getFilteredDataSet(hmTestMethodFilter);
                String[] strArrSampleID = StringUtil.split(plAddSample.getProperty("newkeyid1"), ";");
                String strSampleIDs = "";

                for (int j = 0; j < strArrSampleID.length; ++j) {
                    if (strSampleIDs.equalsIgnoreCase("")) {
                        strSampleIDs = StringUtil.repeat(strArrSampleID[j], dsTestMethod.getRowCount(), ";");
                    } else {
                        strSampleIDs = strSampleIDs + ";" + StringUtil.repeat(strArrSampleID[j], dsTestMethod.getRowCount(), ";");
                    }
                }

                if (dsTestMethod.size() > 0) {
                    PropertyList plAddSDIWorkItemSample = new PropertyList();
                    plAddSDIWorkItemSample.setProperty("sdcid", "Sample");
                    plAddSDIWorkItemSample.setProperty("keyid1", strSampleIDs);
                    plAddSDIWorkItemSample.setProperty("workitemid", StringUtil.repeat(dsTestMethod.getColumnValues("itemkeyid1", ";"), strArrSampleID.length, ";"));
                    plAddSDIWorkItemSample.setProperty("workitemversionid", StringUtil.repeat(dsTestMethod.getColumnValues("itemkeyid2", ";"), strArrSampleID.length, ";"));
                    plAddSDIWorkItemSample.setProperty("applyworkitem", "Y");
                    plAddSDIWorkItemSample.setProperty("propsmatch", "Y");

                    try {
                        getActionProcessor().processAction("AddSDIWorkItem", "1", plAddSDIWorkItemSample);
                    } catch (SapphireException e) {
                        throw new SapphireException("General_Error", ErrorDetail.TYPE_FAILURE
                                , this.getTranslationProcessor().translate("Failed to add Tests for samples. Please contact Administrator." + e.getMessage()));
                    }
                }

                //------ Add Specs to Sample ---//
                DataSet dsSpecMethod = dsGroup.getFilteredDataSet(hmSpecFilter);
                strSampleIDs = "";

                for (int j = 0; j < strArrSampleID.length; ++j) {
                    if (strSampleIDs.equalsIgnoreCase("")) {
                        strSampleIDs = StringUtil.repeat(strArrSampleID[j], dsSpecMethod.getRowCount(), ";");
                    } else {
                        strSampleIDs = strSampleIDs + ";" + StringUtil.repeat(strArrSampleID[j], dsSpecMethod.getRowCount(), ";");
                    }
                }

                if (dsSpecMethod.size() > 0) {
                    PropertyList plAddSDISpec = new PropertyList();
                    plAddSDISpec.setProperty("sdcid", "Sample");
                    plAddSDISpec.setProperty("keyid1", strSampleIDs);
                    plAddSDISpec.setProperty("specid", dsSpecMethod.getColumnValues("itemkeyid1", ";"));
                    plAddSDISpec.setProperty("specversionid", dsSpecMethod.getColumnValues("itemkeyid2", ";"));

                    try {
                        this.getActionProcessor().processAction("AddSDISpec", "1", plAddSDISpec);
                    } catch (SapphireException e) {
                        throw new SapphireException("General_Error", ErrorDetail.TYPE_FAILURE
                                , this.getTranslationProcessor().translate("Failed to add Specs for samples. Please contact Administrator." + e.getMessage()));
                    }
                }
            }

            PropertyList plEditBatchStatus = new PropertyList();
            plEditBatchStatus.setProperty("sdcid", "Batch");
            plEditBatchStatus.setProperty("keyid1", AlconUtil.getUniqueList(dsBatchStageInformation.getColumnValues("s_batchid", ";"), ";", true));
            plEditBatchStatus.setProperty("batchstatus", "Active");
            plEditBatchStatus.setProperty("__sdcruleignore", "Y");

            try {
                this.getActionProcessor().processAction("EditSDI", "1", plEditBatchStatus);
            } catch (Exception e) {
                throw new SapphireException("General_Error", "FAILURE", this.getTranslationProcessor().translate("Failed to call EditSDI.Please contact Administrator. " + e.getMessage()));
            }
        }
    }


    /**
     * @param strBatchID
     * @param strBatchStatus
     * @param strAuditReason
     * @throws SapphireException
     */
    private void receiveBatch(String strBatchID, String strBatchStatus, String strAuditReason, String currentUser) throws SapphireException {
        PropertyList plEditSDI = new PropertyList();
        plEditSDI.setProperty("sdcid", "Batch");
        plEditSDI.setProperty("keyid1", strBatchID);
        plEditSDI.setProperty("operation", "Receive");
        plEditSDI.setProperty("batchstatus", strBatchStatus);
        plEditSDI.setProperty("auditreason", strAuditReason);
        if (!"".equals(currentUser)) {
            plEditSDI.setProperty("receivedby", currentUser);
        }

        try {
            this.getActionProcessor().processAction("EditSDI", "1", plEditSDI);
        } catch (SapphireException var5) {
            throw new SapphireException("General_Error", "FAILURE", this.getTranslationProcessor().translate("Failed to call EditSDI.Please contact Administrator."));
        }
    }

}
