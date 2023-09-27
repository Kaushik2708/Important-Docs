package labvantage.custom.alcon.actions;

import sapphire.SapphireException;
import sapphire.action.BaseAction;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.util.StringUtil;
import sapphire.xml.PropertyList;

import java.util.HashMap;
/**
 * $Author: BAGCHAN1 $
 * $Date: 2022-04-20 01:39:24 +0530 (Wed, 20 Apr 2022) $
 * $Revision: 48 $
 */

/********************************************************************************************************
 * $Revision: 48 $
 * Description: This action helps adding samples to multiple stages using SDC
 * and not Policy which it used to earlier.
 *
 *
 *******************************************************************************************************/

public class AddSamplesToMultipleStage extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 48 $";
    public static final String ID = "AddSamplesToMultipleStage";
    public static final String VERSIONID = "1";
    private static final String __PROPS_LABEL = "label";
    private static final String __PROPS_LEVELID = "levelid";
    private static final String __PROPS_SOURCELABEL = "sourcelabel";
    private static final String __PROPS_BATCHSTAGEID = "batchstageid";


    @Override
    public void processAction(PropertyList properties) throws SapphireException {

        logger.info("********************** Inside method processAction **************************");
        /***************************************************************
         * Here properties from getAttributeFromSDC received Label and s_batchstageid.
         *****************************************************************/
        DataSet dsMultipleStages = getAttributeFromSDC();
        String strLabel = properties.getProperty(__PROPS_LABEL);
        String strBatchStageID = properties.getProperty("s_batchstageid");
        /*************************************************************************
         * Function validationForLabel is called for checking up the stage status
         * i.e., if the Stage is defined in the polciy.
         *************************************************************************/
        DataSet dsModifiedMultipleStages = validationForLabel(strLabel, strBatchStageID, dsMultipleStages);
        dsMultipleStages.clear();
        dsMultipleStages = dsModifiedMultipleStages;
        String strBatch = properties.getProperty("s_batchid");
        int inSampleCopies = StringUtil.split(strBatchStageID, ";").length;
        /****************************************************************************************
         * Once Validation is done and the stage is found to be defined in the policy we proceed:-
         *****************************************************************************************/
        addSamplesToMultipleStage(strBatch, inSampleCopies, dsMultipleStages);

        logger.info("********************** End of method processAction **************************");
    }

    private void addSamplesToMultipleStage(String strBatch, int inSampleCopies, DataSet dsMultipleStages) throws SapphireException {
        logger.info("********************** Inside Method addSamplesToMultipleStage **************************");
        String strSamplePlanDetail = "SELECT sitem.itemsdcid, sstage.label, sitem.itemkeyid1, sitem.itemkeyid2, " +
                "sb.batchdesc, sb.productid, sb.productversionid, splan.s_samplingplanid, splan.s_samplingplanversionid, " +
                "sdetail.levelid, sdetail.sourcelabel, sdetail.templatekeyid1, sdetail.defaultdepartmentid FROM S_SAMPLINGPLAN SPLAN INNER JOIN S_PROCESSSTAGE SSTAGE ON SPLAN.S_SAMPLINGPLANID =SSTAGE.S_SAMPLINGPLANID " +
                "AND SPLAN.S_SAMPLINGPLANVERSIONID=SSTAGE.S_SAMPLINGPLANVERSIONID INNER JOIN S_SPDETAIL SDETAIL ON SDETAIL.S_SAMPLINGPLANID =SSTAGE.S_SAMPLINGPLANID AND SDETAIL.S_SAMPLINGPLANVERSIONID=SSTAGE.S_SAMPLINGPLANVERSIONID " +
                "AND SSTAGE.S_PROCESSSTAGEID = SDETAIL.PROCESSSTAGEID INNER JOIN S_SPDETAILITEM SDETAILITEM ON SDETAILITEM.S_SAMPLINGPLANID =SDETAIL.S_SAMPLINGPLANID AND SDETAILITEM.S_SAMPLINGPLANVERSIONID=SDETAIL.S_SAMPLINGPLANVERSIONID " +
                "AND SDETAILITEM.S_SAMPLINGPLANDETAILNO =SDETAIL.S_SAMPLINGPLANDETAILNO INNER JOIN S_SPITEM SITEM ON SITEM.S_SAMPLINGPLANID =SSTAGE.S_SAMPLINGPLANID AND SITEM.S_SAMPLINGPLANVERSIONID=SSTAGE.S_SAMPLINGPLANVERSIONID " +
                "AND SITEM.S_SAMPLINGPLANITEMNO =SDETAILITEM.S_SAMPLINGPLANITEMNO INNER JOIN S_BATCH SB ON SB.SAMPLINGPLANID =SPLAN.S_SAMPLINGPLANID AND SB.SAMPLINGPLANVERSIONID=SPLAN.S_SAMPLINGPLANVERSIONID WHERE SB.S_BATCHID= ? ";
        DataSet dsSamplePlanDetail = getQueryProcessor().getPreparedSqlDataSet(strSamplePlanDetail, new Object[]{strBatch});
        if (null == dsSamplePlanDetail) {
            throw new SapphireException("General Error::", ErrorDetail.TYPE_FAILURE, "Unable to Execute the Query for Sample Plan Detail");
        } else if (dsSamplePlanDetail.size() == 0) {
            throw new SapphireException("General Error::", ErrorDetail.TYPE_FAILURE, "Please set up the Master data in the Sampling Plan.");
        } else {
            // Sorting on itemsdcid in ascending order
            dsSamplePlanDetail.sort("sitem.itemsdcid");
            HashMap hmFilter = new HashMap();
            String strTemplateID = "";
            String strSecurityDepartment = "";

            for (int i = 0; i < dsMultipleStages.size(); ++i) {
                hmFilter.put(__PROPS_LABEL, dsMultipleStages.getValue(i, __PROPS_LABEL, ""));
                hmFilter.put(__PROPS_LEVELID, dsMultipleStages.getValue(i, __PROPS_LEVELID, ""));
                hmFilter.put(__PROPS_SOURCELABEL, dsMultipleStages.getValue(i, __PROPS_SOURCELABEL, ""));
                if (dsSamplePlanDetail.findRow(hmFilter) == -1) {
                    throw new SapphireException("General Error::", ErrorDetail.TYPE_FAILURE, "Unable to retrieve Sample Plan value for Stage : " + dsMultipleStages.getValue(i, __PROPS_LABEL, "") + " Level : " + dsMultipleStages.getValue(i, __PROPS_LEVELID, "") + " Source Label : " + dsMultipleStages.getValue(i, __PROPS_SOURCELABEL, ""));
                }

                if (strTemplateID.equalsIgnoreCase("")) {
                    strTemplateID = dsSamplePlanDetail.getValue(dsSamplePlanDetail.findRow(hmFilter), "templatekeyid1");
                    strSecurityDepartment = dsSamplePlanDetail.getValue(dsSamplePlanDetail.findRow(hmFilter), "defaultdepartmentid");
                } else {
                    strTemplateID = strTemplateID + ";" + dsSamplePlanDetail.getValue(dsSamplePlanDetail.findRow(hmFilter), "templatekeyid1");
                    strSecurityDepartment = strSecurityDepartment + ";" + dsSamplePlanDetail.getValue(dsSamplePlanDetail.findRow(hmFilter), "defaultdepartmentid");
                }
            }
            //*******  Adding sample here
            PropertyList plAddSDI = new PropertyList();
            plAddSDI.setProperty("batchid", StringUtil.repeat(strBatch, inSampleCopies, ";"));
            plAddSDI.setProperty(__PROPS_BATCHSTAGEID, dsMultipleStages.getColumnValues(__PROPS_BATCHSTAGEID, ";"));
            plAddSDI.setProperty("productversionid", StringUtil.repeat(dsSamplePlanDetail.getValue(0, "productversionid"), inSampleCopies, ";"));
            plAddSDI.setProperty("sourcespid", StringUtil.repeat(dsSamplePlanDetail.getValue(0, "s_samplingplanid"), inSampleCopies, ";"));
            plAddSDI.setProperty("sourcespversionid", StringUtil.repeat(dsSamplePlanDetail.getValue(0, "s_samplingplanversionid"), inSampleCopies, ";"));
            plAddSDI.setProperty("sdcid", "Sample");
            plAddSDI.setProperty("sampledesc", StringUtil.repeat(dsSamplePlanDetail.getValue(0, "batchdesc"), inSampleCopies, ";"));
            plAddSDI.setProperty("copies", String.valueOf(inSampleCopies));
            plAddSDI.setProperty("productid", StringUtil.repeat(dsSamplePlanDetail.getValue(0, "productid"), inSampleCopies, ";"));
            plAddSDI.setProperty("sourcespsourcelabel", dsMultipleStages.getColumnValues(__PROPS_SOURCELABEL, ";"));
            plAddSDI.setProperty("sourcesplevelid", dsMultipleStages.getColumnValues(__PROPS_LEVELID, ";"));
            plAddSDI.setProperty("templateid", strTemplateID);
            plAddSDI.setProperty("securitydepartment", strSecurityDepartment);
            String strNewSampleID = "";

            try {
                getActionProcessor().processAction("AddSDI", "1", plAddSDI);
                strNewSampleID = plAddSDI.getProperty("newkeyid1", "");
            } catch (SapphireException var30) {
                throw new SapphireException("General Error::", ErrorDetail.TYPE_FAILURE, "Fail to call  AddSDI" + var30.getMessage());
            }
            // Adding Spec details to Samples
            addSpecsToSamples(strNewSampleID, dsMultipleStages, dsSamplePlanDetail);
            // Adding Test details to Samples
            addWorkItmeToSamples(strNewSampleID, dsMultipleStages, dsSamplePlanDetail);
        }
        logger.info("********************** End of method addSamplesToMultipleStage **************************");
    }

    private void addSpecsToSamples(String strNewSampleID, DataSet dsMultipleStages, DataSet dsSamplePlanDetail) throws SapphireException {
        logger.info("********************** Inside method addSpecsToSamples **************************");
        HashMap hmFilter = new HashMap();
        String strSpecID = "";
        String strSpecVersionID = "";
        String strSampleForSpecs = "";
        String[] strNewKeyIDArr = StringUtil.split(strNewSampleID, ";");
        for (int i = 0; i < dsMultipleStages.size(); ++i) {
            hmFilter.put(__PROPS_LABEL, dsMultipleStages.getValue(i, __PROPS_LABEL, ""));
            hmFilter.put(__PROPS_LEVELID, dsMultipleStages.getValue(i, __PROPS_LEVELID, ""));
            hmFilter.put(__PROPS_SOURCELABEL, dsMultipleStages.getValue(i, __PROPS_SOURCELABEL, ""));
            hmFilter.put("itemsdcid", "SpecSDC");
            DataSet dsTemp = dsSamplePlanDetail.getFilteredDataSet(hmFilter);
            if (dsTemp.size() > 0) {
                if (strSampleForSpecs.equalsIgnoreCase("")) {
                    strSampleForSpecs = StringUtil.repeat(strNewKeyIDArr[i], dsTemp.size(), ";");
                    strSpecID = dsTemp.getColumnValues("itemkeyid1", ";");
                    strSpecVersionID = dsTemp.getColumnValues("itemkeyid2", ";");
                } else {
                    strSampleForSpecs = strSampleForSpecs + ";" + StringUtil.repeat(strNewKeyIDArr[i], dsTemp.size(), ";");
                    strSpecID = strSpecID + ";" + dsTemp.getColumnValues("itemkeyid1", ";");
                    strSpecVersionID = strSpecVersionID + ";" + dsTemp.getColumnValues("itemkeyid2", ";");
                }
            }
        }

        if (!strSampleForSpecs.equalsIgnoreCase("")) {
            String[] strSpecIDArr = StringUtil.split(strSpecID, ";");
            String[] strSpecVerArr = StringUtil.split(strSpecVersionID, ";");
            String[] strSampleForSpecArr = StringUtil.split(strSampleForSpecs, ";");

            for (int inSpec = 0; inSpec < strSpecIDArr.length; ++inSpec) {
                PropertyList plAddSDISpec = new PropertyList();
                plAddSDISpec.setProperty("sdcid", "Sample");
                plAddSDISpec.setProperty("specid", strSpecIDArr[inSpec]);
                plAddSDISpec.setProperty("specversionid", strSpecVerArr[inSpec]);
                plAddSDISpec.setProperty("keyid1", strSampleForSpecArr[inSpec]);

                try {
                    getActionProcessor().processAction("AddSDISpec", "1", plAddSDISpec);
                } catch (SapphireException var28) {
                    throw new SapphireException("General Error::", ErrorDetail.TYPE_FAILURE, "Fail to call  AddSDIWorkItem" + var28.getMessage());
                }
            }
        }
        logger.info("********************** End of method addSpecsToSamples **************************");
    }

    private void addWorkItmeToSamples(String strNewSampleID, DataSet dsMultipleStages, DataSet dsSamplePlanDetail) throws SapphireException {
        logger.info("********************** Inside method addWorkItmeToSamples **************************");
        String strWorkItemID = "";
        String strWorkItemVersionID = "";
        String strSampleForWorkItemID = "";
        String[] strNewKeyIDArr = StringUtil.split(strNewSampleID, ";");
        HashMap hmFilter = new HashMap();

        for (int i = 0; i < dsMultipleStages.size(); ++i) {
            hmFilter.put(__PROPS_LABEL, dsMultipleStages.getValue(i, __PROPS_LABEL, ""));
            hmFilter.put(__PROPS_LEVELID, dsMultipleStages.getValue(i, __PROPS_LEVELID, ""));
            hmFilter.put(__PROPS_SOURCELABEL, dsMultipleStages.getValue(i, __PROPS_SOURCELABEL, ""));
            hmFilter.put("itemsdcid", "WorkItem");
            DataSet dsTemp = dsSamplePlanDetail.getFilteredDataSet(hmFilter);
            if (dsTemp.size() > 0) {
                if (strSampleForWorkItemID.equalsIgnoreCase("")) {
                    strSampleForWorkItemID = StringUtil.repeat(strNewKeyIDArr[i], dsTemp.size(), ";");
                    strWorkItemID = dsTemp.getColumnValues("itemkeyid1", ";");
                    strWorkItemVersionID = dsTemp.getColumnValues("itemkeyid2", ";");
                } else {
                    strSampleForWorkItemID = strSampleForWorkItemID + ";" + StringUtil.repeat(strNewKeyIDArr[i], dsTemp.size(), ";");
                    strWorkItemID = strWorkItemID + ";" + dsTemp.getColumnValues("itemkeyid1", ";");
                    strWorkItemVersionID = strWorkItemVersionID + ";" + dsTemp.getColumnValues("itemkeyid2", ";");
                }
            }
        }

        if (!strSampleForWorkItemID.equalsIgnoreCase("")) {
            PropertyList plAddWorkItem = new PropertyList();
            plAddWorkItem.setProperty("sdcid", "Sample");
            plAddWorkItem.setProperty("workitemid", strWorkItemID);
            plAddWorkItem.setProperty("workitemversionid", strWorkItemVersionID);
            plAddWorkItem.setProperty("keyid1", strSampleForWorkItemID);
            plAddWorkItem.setProperty("applyworkitem", "Y");
            plAddWorkItem.setProperty("propsmatch", "Y");

            try {
                getActionProcessor().processAction("AddSDIWorkItem", "1", plAddWorkItem);
            } catch (SapphireException var29) {
                throw new SapphireException("General Error::", ErrorDetail.TYPE_FAILURE, "Fail to call  AddSDIWorkItem" + var29.getMessage());
            }
        }
        logger.info("********************** End of method addWorkItmeToSamples **************************");

    }


    /***************************************************************
     * This method is used to check whether stage is defined in the policy or not.
     * @return Returns Error message to check the validation process.
     *****************************************************************/
    private DataSet validationForLabel(String strLabel, String strBatchStageID, DataSet dsMultipleStages) throws SapphireException {
        logger.info("********************** Inside method validationForLabel **************************");
        DataSet dsModifiedMultipleStages = new DataSet(connectionInfo);
        dsModifiedMultipleStages.addColumn(__PROPS_LABEL, DataSet.STRING);
        dsModifiedMultipleStages.addColumn(__PROPS_LEVELID, DataSet.STRING);
        dsModifiedMultipleStages.addColumn(__PROPS_SOURCELABEL, DataSet.STRING);
        dsModifiedMultipleStages.addColumn(__PROPS_BATCHSTAGEID, DataSet.STRING);
        String[] strLabelArr = StringUtil.split(strLabel, ";");
        String[] strBatchStageArr = StringUtil.split(strBatchStageID, ";");
        String strTempStage = "";
        HashMap hmFilter = new HashMap();
        String errorData = "";
        Boolean flag = false;

        for (int i = 0; i < strLabelArr.length; ++i) {
            strTempStage = strLabelArr[i];
            hmFilter.put(__PROPS_LABEL, strTempStage);
            int inFindRow = dsMultipleStages.findRow(hmFilter);
            int inAddRow;
            DataSet dsTemp = dsMultipleStages.getFilteredDataSet(hmFilter);
            if (dsTemp.size() > 1) {
                throw new SapphireException("General Error::", ErrorDetail.TYPE_FAILURE, "More than one record found for the Stage :" + strTempStage + ", in the master setup.");
            }

            if (inFindRow != -1) {
                inAddRow = dsModifiedMultipleStages.addRow();
                dsModifiedMultipleStages.setValue(inAddRow, __PROPS_LABEL, dsMultipleStages.getValue(inFindRow, __PROPS_LABEL, ""));
                dsModifiedMultipleStages.setValue(inAddRow, __PROPS_LEVELID, dsMultipleStages.getValue(inFindRow, __PROPS_LEVELID, ""));
                dsModifiedMultipleStages.setValue(inAddRow, __PROPS_SOURCELABEL, dsMultipleStages.getValue(inFindRow, __PROPS_SOURCELABEL, ""));
                dsModifiedMultipleStages.setValue(inAddRow, __PROPS_BATCHSTAGEID, strBatchStageArr[i]);
            } else {
                flag = true;
                errorData = errorData + "," + strTempStage;
            }
        }
        if (flag == true) {
            errorData = errorData.substring(1);
            throw new SapphireException("General Error::", ErrorDetail.TYPE_FAILURE, "The following Stage detail(s) are missing in master setup -" + errorData + ".");
        }

        logger.info("********************** End of method validationForLabel **************************");
        return dsModifiedMultipleStages;
    }

    /***************************************************************
     * This method is used to get Batch Stage ids from Multiple Sample Stage SDC.
     * @return Returns DataSet of Batch Stage Ids.
     * @throws SapphireException OOB Sapphire Exceptions
     *****************************************************************/
    private DataSet getAttributeFromSDC() throws SapphireException {
        logger.info("********************** Inside method getAttributeFromSDC **************************");
        DataSet dsMultipleStages = new DataSet(connectionInfo);
        try {
            dsMultipleStages.addColumn(__PROPS_LABEL, DataSet.STRING);
            dsMultipleStages.addColumn(__PROPS_LEVELID, DataSet.STRING);
            dsMultipleStages.addColumn(__PROPS_SOURCELABEL, DataSet.STRING);
            int inRow;
            String sqlText = "select u_multiplespstageid, levelid, sourcelabel from u_multiplespstage";
            DataSet dsStageDetails = getQueryProcessor().getSqlDataSet(sqlText);
            if (dsStageDetails.getRowCount() > 0) {
                for (int i = 0; i < dsStageDetails.getRowCount(); ++i) {
                    inRow = dsMultipleStages.addRow();
                    dsMultipleStages.setValue(inRow, __PROPS_LABEL, dsStageDetails.getValue(i, "u_multiplespstageid", ""));
                    dsMultipleStages.setValue(inRow, __PROPS_LEVELID, dsStageDetails.getValue(i, __PROPS_LEVELID, ""));
                    dsMultipleStages.setValue(inRow, __PROPS_SOURCELABEL, dsStageDetails.getValue(i, __PROPS_SOURCELABEL, ""));
                }

            }

        } catch (Exception var5) {
            throw new SapphireException(var5.getMessage().toString());
        }
        logger.info("********************** Inside method getAttributeFromSDC **************************");
        return dsMultipleStages;
    }
}
