package labvantage.custom.alcon.actions;

import com.labvantage.sapphire.DataSetUtil;
import com.labvantage.sapphire.DateTimeUtil;
import com.labvantage.sapphire.actions.workitem.WorkItemUtil;
import sapphire.SapphireException;
import sapphire.action.*;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.util.StringUtil;
import sapphire.xml.PropertyList;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;


/*****************************************************************************************************
 * $Author: kumarvi4 $
 * $Date: 2023-08-21 16:43:59 +0530 (Mon, 21 Aug 2023) $
 * $Revision: 547 $
 * Description: This class is used to create Sync Samples.It copies the results and data from the parent batch sample.
 *              It uses the specification set on the child batch sampling plan to evaluate the spec.
 *              It excludes the LES worksheet,Test Attributes, LES Text, Workflow to get created/copied from parent sample.
 ******************************************************************************************************/

public class CreateSyncSamples extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 547 $";
    public static final String ACTION_ID = "CreateSyncSamples";
    public static final String __SDC_SAMPLE = "Sample";
    public static final String __NEW_KEY_ID = "newkeyid1";

    /*********************************************************************************************************
     * Description: processAction is an OOB LabVantage method. This is the main method where execution starts.
     * @param properties
     * @throws SapphireException
     **********************************************************************************************************/

    @Override
    public void processAction(PropertyList properties) throws SapphireException {
        syncSample(properties);
    }

    /*************************************
     * Sync Samples from the parent batch.
     * @param properties
     * @throws SapphireException
     *************************************/
    public void syncSample(PropertyList properties) throws SapphireException {
        logger.debug("================== Processing CreateSyncSamples action. ==========================================");
        String arrParentSampleIds[] = properties.getProperty("parentsampleid", "").split(";");
        String arrParentLotSampleIds[] = properties.getProperty("u_parentlotsamplesid", "").split(";");
        String arrChildBatchIds[] = properties.getProperty("childbatchid", "").split(";");

        for (int i = 0; i < arrParentSampleIds.length; i++) {
            //*********** Copying SDI details from Parent Sample to Child ***************
            String newSampleId = copyParentSampleSDIDetails(arrParentSampleIds[i]);
            // ********** Fetching and adding parent workitem details to new Sample Id(s) excluding the Group TM *************
            addParentWorkItem(arrParentSampleIds[i], newSampleId);
            //********** copying parent Group workitem details to new Sample Id(s) ********
            copyParentGroupTM(arrParentSampleIds[i], newSampleId);
            // *********** Add Adhoc DataSet  from parent sample to child sample **********
            addAdhocDataSet(newSampleId, arrParentSampleIds[i]);
            //******* copy sdiworkitemitem from parent to child sample *******
            copySDIWII(arrParentSampleIds[i], newSampleId);
            //******* copy sdiworkitemrelation from parent batch sample to child batch sample.
            copySDIWIRelation(newSampleId);
            //*********** Adding replicates to DataSet ***************
            addReplicateDetails(newSampleId, arrParentSampleIds[i]);
            //*********** Getting stage details from Policy ************
            String strPolicyStageName = getStageFromPolicy();
            //*********** Finding the Batch Stage Id from the Current Batch ***************************
            DataSet dsBatchStage = findStageIdFromCurrentBatch(arrChildBatchIds[i], strPolicyStageName);
            //*********** Find Spec details from Sampling plan **************
            DataSet dsFindSpecs = findSpecFromSamplingPlan(arrChildBatchIds[i], strPolicyStageName);
            //*********** Find and remove existing Spec from new Sample *************
            findAndRemoveExistingSpec(newSampleId);
            //*********** Add new Spec from new Sample *************
            addNewSpec(newSampleId, dsFindSpecs);
            //*********** Declaring variables *************
            String sourcespsourcelabel = "";
            String sourcesplevelid = "";
            String sourcespid = "";
            String sourcespversionid = "";
            if (dsFindSpecs.getRowCount() > 0) {
                sourcespsourcelabel = dsFindSpecs.getValue(0, "sourcelabel");
                sourcesplevelid = dsFindSpecs.getValue(0, "levelid");
                sourcespid = dsFindSpecs.getValue(0, "s_samplingplanid");
                sourcespversionid = dsFindSpecs.getValue(0, "s_samplingplanversionid");
            }
            String batchstageid = dsBatchStage.getValue(0, "s_batchstageid");
            String productid = dsBatchStage.getValue(0, "productid");
            String productversionid = dsBatchStage.getValue(0, "productversionid");
            // ************ Removing Batch stage locking **************
            // ************ Reason: OOB limitation, if Batch Stage is already locked from Batch maintenance page, edit of linked sample(s) is not allowed.
            unlockBatchStage(arrChildBatchIds[i]);

            // ************ Updating Sample Information ************
            updateSampleDetails(newSampleId, batchstageid, arrChildBatchIds[i], productid, productversionid, sourcespsourcelabel, sourcesplevelid, sourcespid, sourcespversionid);
            // ************ Getting latest DataSet info ***********
            DataSet dsLatestDataItem = getLatestDataItem(newSampleId, arrParentSampleIds[i]);
            // ************ Updating Sample Information ************
            DataSet dsDataValue = newSampleDataEntry(newSampleId, dsLatestDataItem);
            // ************ Updating DataItem Information ************
            EditDataItem(newSampleId, dsDataValue);
            // ***** Redo Calculation
            redoCalculation(newSampleId);
            // ************ Update Parent Lot Sample Detail ************
            updateParentLotSample(newSampleId, arrParentLotSampleIds[i]);
            // **************** Getting Latest DataSet *****************
            DataSet dsLatestDS = getLatestDataSet(newSampleId, arrParentSampleIds[i]);
            //*********** Updating DataSet ************************
            updateDataSet(dsLatestDS);
            //*********** Releasing Batch Stage **************
            releaseBatchStage(batchstageid);
            // ********** Updating SDIWorkItem for new Sample based on existing Sample ***********
            updateSDIWorkItemBySample(newSampleId, arrParentSampleIds[i]);
            // ********** Releasing DataItem *************
            releaseDataItem(dsDataValue);
            // ********** Releaseing DataSet *************
            releaseDataSet(dsLatestDS);
            // ********** Updateing DataSet status **********
            updateDataSetStatus(dsLatestDS);
            // ********** Cancelling DataSet ************
            cancelDataSet(dsLatestDS);
        }
    }

    /**
     * invoke Redocalculation for child sample
     * @param newSampleId
     * @throws SapphireException
     */
    private void redoCalculation(String newSampleId) throws SapphireException {
        this.logger.debug(" Processing " + ACTION_ID + ". (Action) : Inside redoCalculation (methods)");
        try{
            HashMap<String,String> hmProps = new HashMap<>();
            hmProps.put("sdcid", __SDC_SAMPLE);
            hmProps.put("keyid1", newSampleId);
            this.getActionProcessor().processAction(RedoCalculations.ID,RedoCalculations.VERSIONID,hmProps);
        }catch(SapphireException se){
            throw new SapphireException("General Error : ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(se.getMessage()));
        }
    }

    /**
     * Copy sdiWorkitemRelation from parent to child batch sample.
     * @param newSampleId
     * @throws SapphireException
     */
    private void copySDIWIRelation(String newSampleId) throws SapphireException {
        this.logger.debug(" Processing " + ACTION_ID + ". (Action) : Inside copySDIWIAttrRelation (methods)");
        String rsetId = "";
        DataSet dsChildSampleWI = new DataSet();
        DataSet appliedSDIWI = new DataSet();
        DataSet sdiwiitem = new DataSet();
        try {
            rsetId = this.getDAMProcessor().createRSet(__SDC_SAMPLE, newSampleId, "(null)", "(null)");
            String sbSampleWI = "select sd.sdcid, sd.keyid1,sd.keyid2,sd.keyid3,sd.workitemid,sd.workitemversionid,sd.workiteminstance " +
                    "from sdiworkitem sd,rsetitems rs where sd.sdcid = rs.sdcid and sd.keyid1 = rs.keyid1 and sd.keyid2= rs.keyid2 and sd.keyid3 = rs.keyid3 and rs.rsetid=?";
            dsChildSampleWI = getQueryProcessor().getPreparedSqlDataSet(sbSampleWI, new Object[]{rsetId});
            // all the workitem assumed that are applied for the sample.
            appliedSDIWI = this.getQueryProcessor().getPreparedSqlDataSet("SELECT sw.* FROM sdiworkitem sw, rsetitems r WHERE sw.keyid1 = r.keyid1 AND sw.keyid2 = r.keyid2 AND sw.sdcid = r.sdcid AND r.rsetid = ?", new String[]{rsetId});
            // all the sdiworkitemitem records
            sdiwiitem = this.getQueryProcessor().getPreparedSqlDataSet("SELECT swii.* FROM sdiworkitemitem swii, rsetitems r WHERE swii.keyid1 = r.keyid1 AND swii.keyid2 = r.keyid2 AND swii.sdcid = r.sdcid AND r.rsetid = ?", new String[]{rsetId});

        } catch (SapphireException se) {
            throw new SapphireException("General Error : ", getTranslationProcessor().translate(se.getMessage()));
        } finally {
            if (!"".equalsIgnoreCase(rsetId)) {
                getDAMProcessor().clearRSet(rsetId);
            }
        }
        if (dsChildSampleWI == null) {
            throw new SapphireException("SQL Exception: ", getTranslationProcessor().translate("Failed to Execute SQL Statement in copySDIWIRel method in " + ACTION_ID));
        } else if (dsChildSampleWI.getRowCount() > 0) {
            DataSet dsInstances = new DataSet(); // sdiworkitemitem for ParameterList itemsdcid
            //DataSet sdiattributes = new DataSet(); //
            for (int k = 0; k < dsChildSampleWI.getRowCount(); k++) {
                String workitemid = dsChildSampleWI.getValue(k, "workitemid", "");
                String workitemversionid = dsChildSampleWI.getValue(k, "workitemversionid", "");
                String rsetWorkItems = "";
                try {
                    rsetWorkItems = this.getDAMProcessor().createRSet("WorkItem", workitemid, workitemversionid, "(null)");
                    //except les worksheet
                   // DataSet allWorkItemAttributes = this.getQueryProcessor().getPreparedSqlDataSet("SELECT s.* FROM sdiattribute s, rsetitems r WHERE s.keyid1 = r.keyid1 AND s.keyid2 = r.keyid2 AND s.sdcid = r.sdcid AND r.rsetid = ? AND s.sdcid not in ('LV_WorksheetItem','LV_Worksheet')", new String[]{rsetWorkItems});
                    DataSet allWorkItemReagents = this.getQueryProcessor().getPreparedSqlDataSet("SELECT wr.* FROM workitemreagenttype wr, rsetitems r WHERE wr.workitemid = r.keyid1 AND wr.workitemversionid = r.keyid2 AND r.sdcid = 'WorkItem' AND r.rsetid = ?", new String[]{rsetWorkItems});
                    DataSet allWorkItemInstruments = this.getQueryProcessor().getPreparedSqlDataSet("SELECT wi.* FROM workiteminstrument wi, rsetitems r WHERE wi.workitemid = r.keyid1 AND wi.workitemversionid = r.keyid2 AND r.sdcid = 'WorkItem' AND r.rsetid = ?", new String[]{rsetWorkItems});
                    HashMap<String, String> filter = new HashMap<>();
                    if (appliedSDIWI.getRowCount() > 0) {
                        if (sdiwiitem != null && sdiwiitem.getRowCount() > 0) {
                            filter.clear();
                            filter.put("itemsdcid", "ParamList");
                            DataSet plSDIWII = sdiwiitem.getFilteredDataSet(filter);

                            for (int p = 0; p < plSDIWII.getRowCount(); ++p) {
                                if (plSDIWII.getValue(p, "iteminstance").length() != 0) {
                                    dsInstances.copyRow(plSDIWII, p, 1);
                                }
                            }
                        }

                        if (allWorkItemInstruments.getRowCount() > 0 || allWorkItemReagents.getRowCount() > 0) {
                            WorkItemUtil.addWorkItemDataSetRelations(dsInstances, appliedSDIWI, this.database, this.getActionProcessor(), allWorkItemReagents, allWorkItemInstruments);
                        }
                    }
                } catch (SapphireException se) {
                    throw new SapphireException("General Error: ", getTranslationProcessor().translate(se.getMessage()));
                } finally {
                    if (!"".equalsIgnoreCase(rsetWorkItems)) {
                        this.getDAMProcessor().clearRSet(rsetWorkItems);
                    }
                }
            }
        }
    }

    /**********************************************************************
     * copy group TM from Parent Sample to Child Sample
     * @param arrParentSampleId
     * @param newSampleId
     * @throws SapphireException
     **********************************************************************/
    private void copyParentGroupTM(String arrParentSampleId, String newSampleId) throws SapphireException {
        this.logger.debug("Processing " + ACTION_ID + ". (Action) : Inside copyParentGroupTM (methods)");
        String cRsetId = "";
        String pRsetId = "";
        DataSet dsParentSampleGrpWI;
        DataSet dsChildSampleGrpWI;
        try {
            // Creating Rset using DAM Processor
            pRsetId = getDAMProcessor().createRSet(__SDC_SAMPLE, arrParentSampleId, "(null)", "(null)");
            cRsetId = getDAMProcessor().createRSet(__SDC_SAMPLE, newSampleId, "(null)", "(null)");
            // sql to find group workitems for a sample
            String sqlSampleWI = "select sd.sdcid,'" + newSampleId + "' keyid1,sd.keyid2,sd.keyid3,sd.workitemid,sd.workiteminstance,sd.workitemtypeflag,sd.groupid,sd.groupinstance,sd.appliedflag,sd.completeflag,sd.s_shippeddt,sd.s_shippableflag,sd.s_retestinstance,sd.s_retestedflag,sd.tracelogid,sd.duedt,sd.trackitemid,sd.scheduleplanid,sd.scheduleplanitemid,sd.s_assaytypeid,sd.s_sampletypeid,sd.documentid,sd.documentversionid,sd.activeflag,sd.s_assignedanalyst,sd.s_assigneddepartment,sd.workitemstatus,sd.duedtoffset,sd.workitemversionid," +
                    " sd.duedtoffsettimeunit,sd.duedtoverrideflag,sd.sourcesstudyid,sd.childsampleplanappliedflag,sd.embedchildsampleplanid,sd.embedchildsampleplanversionid, " +
                    " sd.completeddt,sd.applieddt,sd.starteddt,sd.cancelleddt,sd.completedby,sd.appliedby,sd.startedby,sd.cancelledby,sd.applyonaddflag, " +
                    " sd.u_changeduedate,sd.u_testingtime,sd.u_assigneddate,sd.u_reassigneddate,sd.reflexrule,sd.wapstatus,sd.testingdepartmentid,sd.workareadepartmentid,sd.plannedstartdt " +
                    " from sdiworkitem sd,rsetitems rs where sd.sdcid = rs.sdcid and sd.keyid1 = rs.keyid1 and sd.keyid2= rs.keyid2 and sd.keyid3 = rs.keyid3 and rs.rsetid=? and sd.groupid is not null";
            dsParentSampleGrpWI = getQueryProcessor().getPreparedSqlDataSet(sqlSampleWI, new Object[]{pRsetId});
            dsChildSampleGrpWI = getQueryProcessor().getPreparedSqlDataSet(sqlSampleWI, new Object[]{cRsetId});
        } catch (SapphireException ex) {
            throw new SapphireException("General Error : ", getTranslationProcessor().translate(ex.getMessage()));
        } finally {
            if (!"".equalsIgnoreCase(pRsetId)) {
                getDAMProcessor().clearRSet(pRsetId);
            }
            if (!"".equalsIgnoreCase(cRsetId)) {
                getDAMProcessor().clearRSet(cRsetId);
            }
        }
        if (dsChildSampleGrpWI == null || dsChildSampleGrpWI == null) {
            throw new SapphireException("SQL Exception: ", getTranslationProcessor().translate("Failed to Execute SQL Statement in copyParentGroupTM methid in " + ACTION_ID));
        } else if (dsParentSampleGrpWI.getRowCount() > 0) {
            this.logger.debug("Processing the sdiworkitem inserts: " + dsParentSampleGrpWI);
            try {
                dsParentSampleGrpWI = setParentSampleGrpWI(dsParentSampleGrpWI);
                // Foreign key check can be avoided as this data is getting copied from parent and it is assumed that value should be present in the system for other linked tables
                DataSetUtil.insert(this.database, dsParentSampleGrpWI, "sdiworkitem");
            } catch (SapphireException se) {
                throw new SapphireException("General Error:", getTranslationProcessor().translate("Failed to Insert data into sdiworkitem table from action " + ACTION_ID));
            }
        }
    }

    /**
     * Set columns value to the parentsample group WI for specified list of column
     *
     * @param dsParentSampleGrpWI
     * @return
     */
    private DataSet setParentSampleGrpWI(DataSet dsParentSampleGrpWI) {
        Calendar now = DateTimeUtil.getNowCalendar();
        dsParentSampleGrpWI.setDate(-1, "completeddt", now);
        dsParentSampleGrpWI.setString(-1, "completedby", this.connectionInfo.getSysuserId());
        dsParentSampleGrpWI.setDate(-1, "starteddt", now);
        dsParentSampleGrpWI.setString(-1, "startedby", this.connectionInfo.getSysuserId());
        dsParentSampleGrpWI.setDate(-1, "applieddt", "");
        dsParentSampleGrpWI.setString(-1, "appliedby", "");
        return dsParentSampleGrpWI;
    }

    /*********************************************************************************
     * This method is used to remeasure dataset.
     * @param newSampleId Child Sample Id.
     * @param parentSampleId Parent Sample Id
     * @throws SapphireException OOB Sapphire Exception
     **********************************************************************************/
    private void remeasureDataSet(String newSampleId, String parentSampleId) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside remeasureDataSet (method)");
        // ************* Getting all DataSet information from sdidata
        String sqlSDIData = "SELECT paramlistid,paramlistversionid,variantid,dataset,s_remeasuredflag, s_remeasureinstance, TO_CHAR(s_remeasureinstance) str_s_remeasureinstance" +
                " FROM sdidata where keyid1= ? AND sdcid = ? ";
        DataSet dsSDIData = getQueryProcessor().getPreparedSqlDataSet(sqlSDIData, new Object[]{parentSampleId, __SDC_SAMPLE});
        if (dsSDIData == null) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside remeasureDataSet - unable to execute sql sqlSDIData. "));
        }
        // ************** If any paramlist details found
        if (dsSDIData.size() > 0) {
            // *********** Finding if remeasured
            PropertyList plRemeasuredFilter = new PropertyList();
            plRemeasuredFilter.setProperty("s_remeasuredflag", "Y");
            DataSet dsRemeasuredDataSet = dsSDIData.getFilteredDataSet(plRemeasuredFilter);
            // ************** If any DataSet is remeasure
            if (dsRemeasuredDataSet.size() > 0) {
                // ************* Getting Result data of Child Sample Id.
                DataSet dsSDIDataItem = getResults(newSampleId, parentSampleId);
                DataSet dsRemeasuredInstance = new DataSet();
                // ************** Remeasuring datasets
                DataSet dsRemeasureSDIDataItem = new DataSet();
                // ************* Looping through each remeasued dataset
                for (int remCount = 0; remCount < dsRemeasuredDataSet.size(); remCount++) {
                    dsRemeasuredInstance.clear();
                    plRemeasuredFilter.clear();
                    // ************* Finding number of remeasured instance
                    plRemeasuredFilter.setProperty("paramlistid", dsRemeasuredDataSet.getValue(remCount, "paramlistid", ""));
                    plRemeasuredFilter.setProperty("paramlistversionid", dsRemeasuredDataSet.getValue(remCount, "paramlistversionid", ""));
                    plRemeasuredFilter.setProperty("variantid", dsRemeasuredDataSet.getValue(remCount, "variantid", ""));
                    plRemeasuredFilter.setProperty("str_s_remeasureinstance", dsRemeasuredDataSet.getValue(remCount, "dataset", ""));
                    dsRemeasuredInstance = dsSDIData.getFilteredDataSet(plRemeasuredFilter);

                    // ************ Getting param info against remeasured dataset
                    plRemeasuredFilter.clear();
                    plRemeasuredFilter.setProperty("paramlistid", dsRemeasuredDataSet.getValue(remCount, "paramlistid", ""));
                    plRemeasuredFilter.setProperty("paramlistversionid", dsRemeasuredDataSet.getValue(remCount, "paramlistversionid", ""));
                    plRemeasuredFilter.setProperty("variantid", dsRemeasuredDataSet.getValue(remCount, "variantid", ""));
                    plRemeasuredFilter.setProperty("str_dataset", dsRemeasuredDataSet.getValue(remCount, "dataset", ""));
                    // Getting SDIDataItem values
                    dsRemeasureSDIDataItem.clear();
                    dsRemeasureSDIDataItem = dsSDIDataItem.getFilteredDataSet(plRemeasuredFilter);

                    if (dsRemeasuredInstance.size() > 0 && dsRemeasureSDIDataItem.size() > 0) {
                        // *********** Looping through the number of remeasured instances
                        for (int remInCount = 0; remInCount < dsRemeasuredInstance.size(); remInCount++) {

                            plRemeasuredFilter.clear();
                            PropertyList plRemeasureDataSet = new PropertyList();
                            plRemeasureDataSet.setProperty("sdcid", __SDC_SAMPLE);
                            plRemeasureDataSet.setProperty("keyid1", newSampleId);
                            plRemeasureDataSet.setProperty("keyid2", "(null)");
                            plRemeasureDataSet.setProperty("keyid3", "(null)");
                            plRemeasureDataSet.setProperty("paramlistid", dsRemeasureSDIDataItem.getColumnValues("paramlistid", ";"));
                            //plRemeasureDataSet.setProperty("paramlistversionid", dsRemeasureSDIDataItem.getColumnValues("paramlistversionid", ";"));
                            plRemeasureDataSet.setProperty("paramlistversionid", StringUtil.repeat("".equalsIgnoreCase(getCurrentParamList(dsRemeasureSDIDataItem.getValue(0, "paramlistid", ";"), dsRemeasureSDIDataItem.getValue(0, "variantid", ";"))) ? dsRemeasureSDIDataItem.getValue(0, "paramlistversionid", ";") : getCurrentParamList(dsRemeasureSDIDataItem.getValue(0, "paramlistid", ";"), dsRemeasureSDIDataItem.getValue(0, "variantid", ";")), dsRemeasureSDIDataItem.getColumnValues("paramlistid", ";").split(";").length, ";"));
                            plRemeasureDataSet.setProperty("variantid", dsRemeasureSDIDataItem.getColumnValues("variantid", ";"));
                            plRemeasureDataSet.setProperty("dataset", dsRemeasureSDIDataItem.getColumnValues("dataset", ";"));
                            plRemeasureDataSet.setProperty("newdsstatus", "Initial");
                            plRemeasureDataSet.setProperty("auditreason ", "SyncCOASample");
                            try {
                                getActionProcessor().processAction("RemeasureDataSet", "1", plRemeasureDataSet);
                            } catch (SapphireException ex) {
                                throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside remeasureDataSet - unable to Remeasure DataSet. "));
                            }
                        }
                    }
                }
            }
        }
    }

    /*************************************************************************************
     * Insert sdiworkitemitem from parent sample to child sample by ensuring that workitem exist in sdiworkitem table for child sample.
     * It is required to maintain the relationship b/w sdiworkitem and sdiworkitemitem table
     * @param arrParentSampleId
     * @param newSampleId
     * @throws SapphireException
     ************************************************************************************/
    private void copySDIWII(String arrParentSampleId, String newSampleId) throws SapphireException {
        this.logger.debug("Processing " + ACTION_ID + ". (Action) : Inside copySDIWII (method)");
        String cRsetId = "";
        String pRsetId = "";
        DataSet dsParentSampleWII;
        DataSet dsChildSampleWII;
        try {
            pRsetId = getDAMProcessor().createRSet(__SDC_SAMPLE, arrParentSampleId, "(null)", "(null)");
            cRsetId = getDAMProcessor().createRSet(__SDC_SAMPLE, newSampleId, "(null)", "(null)");
            String sqlSampleWII = "select swii.sdcid,'" + newSampleId + "' keyid1,swii.keyid2, swii.keyid3, swii.workitemid, swii.workiteminstance,swii.workitemitemid, swii.itemsdcid, swii.itemkeyid1, swii.itemkeyid2, swii.itemkeyid3," +
                    " swii.iteminstance, swii.mandatoryflag, swii.completeflag, swii.workitemitemrule, swii.forcenewflag" +
                    " from sdiworkitemitem swii , rsetitems rs WHERE swii.sdcid = rs.sdcid AND swii.keyid1 = rs.keyid1 and swii.keyid2 = rs.keyid2 and swii.keyid3 = rs.keyid3 and rs.rsetid = ?";
            dsParentSampleWII = getQueryProcessor().getPreparedSqlDataSet(sqlSampleWII, new Object[]{pRsetId});
            dsChildSampleWII = getQueryProcessor().getPreparedSqlDataSet(sqlSampleWII, new Object[]{cRsetId});
        } catch (SapphireException se) {
            throw new SapphireException("General Error :", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(se.getMessage()));
        } finally {
            if (!"".equalsIgnoreCase(pRsetId)) {
                getDAMProcessor().clearRSet(pRsetId);
            }
            if (!"".equalsIgnoreCase(cRsetId)) {
                getDAMProcessor().clearRSet(cRsetId);
            }
        }
        if (dsParentSampleWII == null || dsChildSampleWII == null) {
            throw new SapphireException("General Error:", getTranslationProcessor().translate("Failed to Execute SQL from copySDIWII method in " + ACTION_ID));
        } else if (dsParentSampleWII.getRowCount() > 0) {
            this.logger.debug("Processing the sdiworkitemitem inserts: \n" + dsParentSampleWII);
            if (dsChildSampleWII.getRowCount() == 0) {
                try {
                    //check if workitem and workitemversion exist in sdiworkitem table before inserting into sdiworkitemitem table
                    validateWorkItemItem(dsParentSampleWII, newSampleId);
                    DataSetUtil.insert(this.database, dsParentSampleWII, "sdiworkitemitem");
                } catch (SapphireException se) {
                    throw new SapphireException("General Error:", getTranslationProcessor().translate("Failed to Insert data into sdiworkitemitem table in " + ACTION_ID));
                }
            } else {
                // check if parentSample WII already have values present in the child sample WII, if so don't add it again.
                DataSet dsFilteredParentWII = parentWIIToBeAdded(dsParentSampleWII, dsChildSampleWII);
                try {
                    //check if workitem and workitemversion exist in sdiworkitem table before inserting into sdiworkitemitem table
                    validateWorkItemItem(dsFilteredParentWII, newSampleId);
                    DataSetUtil.insert(this.database, dsFilteredParentWII, "sdiworkitemitem");
                } catch (SapphireException se) {
                    throw new SapphireException("General Error:", getTranslationProcessor().translate("Failed to Insert data into sdiworkitemitem table in " + ACTION_ID));
                }
            }
        }
    }

    /*************************************************************************************
     * Find dataset to add for only the parent sample WII which is not present in Child WII.
     * @param dsParentSampleWII
     * @param dsChildSampleWII
     * @return
     *****************************************************************************************/
    private DataSet parentWIIToBeAdded(DataSet dsParentSampleWII, DataSet dsChildSampleWII) {
        this.logger.debug("Processing " + ACTION_ID + ". (Action) : Inside parentWIIToBeAdded (method)");
        DataSet dsFilteredParent = new DataSet();
        HashMap<String, Object> hmFilter = new HashMap<>();
        for (int j = 0; j < dsChildSampleWII.getRowCount(); j++) {
            String workitem = dsChildSampleWII.getValue(j, "workitemid", "(null)");
            //String workitemversionid = dsChildSampleWII.getValue(j, "workitemversionid", "(null)");
            String workiteminstance = dsChildSampleWII.getValue(j, "workiteminstance", "(null)");
            hmFilter.put("workitemid", workitem);
            //  hmFilter.put("workitemversionid", workitemversionid);
            hmFilter.put("workiteminstance", new BigDecimal(workiteminstance));
            DataSet ds = dsParentSampleWII.getFilteredDataSet(hmFilter);
            if (ds != null && ds.getRowCount() > 0) {
                dsParentSampleWII.addColumnValues("ismatched", DataSet.STRING, "Y", ";");
            }
        }
        if (dsParentSampleWII.getRowCount() > 0) {
            hmFilter.clear();
            hmFilter.put("ismatched", "Y");
            // take only which is not matched
            dsFilteredParent = dsParentSampleWII.getFilteredDataSet(hmFilter, true);
        }
        return dsFilteredParent;
    }

    /************************************************************************************************
     * Validate if Parent Sample WII which is to be copied to child WII, if WI,Ver,Instance exist in the child sample TM.
     * @param dsParentSampleWII
     * @param newSampleId
     * @throws SapphireException
     *************************************************************************************************/
    private void validateWorkItemItem(DataSet dsParentSampleWII, String newSampleId) throws SapphireException {
        this.logger.debug("Processing " + ACTION_ID + ". (Action) : Inside validateWorkItemItem (method)");
        String rsetId = "";
        DataSet dsWI;
        try {
            rsetId = getDAMProcessor().createRSet(__SDC_SAMPLE, newSampleId, "(null)", "(null)");
            String sqlworkItem = "SELECT sw.workitemid, sw.workitemversionid,sw.workiteminstance FROM sdiworkitem sw, rsetitems rs WHERE sw.sdcid = rs.sdcid AND sw.keyid1 = rs.keyid1 and sw.keyid2=rs.keyid2 and sw.keyid3=rs.keyid3 and rs.rsetid = ?";
            dsWI = getQueryProcessor().getPreparedSqlDataSet(sqlworkItem, new Object[]{rsetId});
        } catch (SapphireException se) {
            throw new SapphireException("General Error : " + getTranslationProcessor().translate(se.getMessage()));
        } finally {
            if (!"".equalsIgnoreCase(rsetId)) {
                getDAMProcessor().clearRSet(rsetId);
            }
        }
        if (dsWI != null && dsWI.getRowCount() > 0 && dsParentSampleWII != null) {
            HashMap<String, Object> hm = new HashMap<>();
            DataSet filteredDsWI = new DataSet();
            for (int k = 0; k < dsParentSampleWII.getRowCount(); k++) {
                String workitem = dsParentSampleWII.getValue(k, "workitemid", "(null)");
                // String workitemversionid = dsParentSampleWII.getValue(k, "workitemversionid", "(null)");
                String workiteminstance = dsParentSampleWII.getValue(k, "workiteminstance", "(null)");
                hm.put("workitemid", workitem);
                //hm.put("workitemversionid", workitemversionid);
                hm.put("workiteminstance", new BigDecimal(workiteminstance));
                filteredDsWI = dsWI.getFilteredDataSet(hm);
                if (filteredDsWI.getRowCount() == 0) {
                    //not found match...safe to insert in sdiworkitemitem. Avoid foreign key error
                    throw new SapphireException("Validation Error: ", ErrorDetail.TYPE_FAILURE,
                            getTranslationProcessor().translate(String.format("Test Method ID: %s Instance %s not found in sample %s. Can not be applied to sample.", workitem, workiteminstance, newSampleId)));
                }
            }
        }
    }

    /*********************************************************
     * This method is used to get used version of WorkItem from Parent and copy to child sample.
     * @param parentSampleId Parent Sample Id
     * @param childSampleId  Child Sample Id
     * @throws SapphireException OOB Sapphire Exception
     *********************************************************/
    private void addParentWorkItem(String parentSampleId, String childSampleId) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside addCurrentWorkItem (method)");
        String rsetId = "";
        DataSet dsParentSampleWI = new DataSet();
        try {
            rsetId = getDAMProcessor().createRSet(__SDC_SAMPLE, parentSampleId, "(null)", "(null)");
            String sqlParentSampleWI = "SELECT sw.workitemid, sw.workitemversionid, sw.usersequence FROM sdiworkitem sw, rsetitems rs WHERE sw.sdcid = rs.sdcid AND sw.keyid1 = rs.keyid1 and sw.keyid2=rs.keyid2 and sw.keyid3=rs.keyid3  and sw.workitemtypeflag='W' and sw.groupid is null and rs.rsetid = ?";
            dsParentSampleWI = getQueryProcessor().getPreparedSqlDataSet(sqlParentSampleWI, new Object[]{rsetId});
        } catch (SapphireException se) {
            throw new SapphireException("General Error :", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Failed to execute query inside method - addParentWorkItem"));
        } finally {
            if (!"".equalsIgnoreCase(rsetId)) {
                getDAMProcessor().clearRSet(rsetId);
            }
        }
        if (null == dsParentSampleWI) {
            throw new SapphireException(" General Error :", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Inside addParentWorkItem - unable to execute query for getting workitem(s) for parent Sample." + parentSampleId));
        }
        // *********** Checking if WI exists or not ******************
        if (dsParentSampleWI.size() > 0) {
            // ********* Sorting by user sequence to add workitems in sequence ***********
            dsParentSampleWI.sort("usersequence");

            PropertyList plAddSDIWI = new PropertyList();
            plAddSDIWI.setProperty(AddSDIWorkItem.PROPERTY_SDCID, __SDC_SAMPLE);
            plAddSDIWI.setProperty(AddSDIWorkItem.PROPERTY_KEYID1, childSampleId);
            plAddSDIWI.setProperty(AddSDIWorkItem.PROPERTY_WORKITEMID, dsParentSampleWI.getColumnValues("workitemid", ";"));
            plAddSDIWI.setProperty(AddSDIWorkItem.PROPERTY_WORKITEMVERSIONID, dsParentSampleWI.getColumnValues("workitemversionid", ";"));
            plAddSDIWI.setProperty(AddSDIWorkItem.PROPERTY_APPLYWORKITEM, "N"); //only add workitem to child sample and not dataset from workitemitem
            plAddSDIWI.setProperty(AddSDIWorkItem.PROPERTY_FORCENEW, "N");

            try {
                getActionProcessor().processAction(AddSDIWorkItem.ID, AddSDIWorkItem.VERSIONID, plAddSDIWI);
            } catch (SapphireException ex) {
                throw new SapphireException(" General Error", ErrorDetail.TYPE_FAILURE, ex.getMessage());
            }
        }
    }

    /********************************************************************************************************************************
     * Remove the locking on BatchStage.
     * Reason: OOB limitation, if Batch Stage is already locked from Batch maintenance page, edit of linked sample(s) is not allowed.
     * @param limsBatchId LIMS Batch Id
     ********************************************************************************************************************************/

    private void unlockBatchStage(String limsBatchId) {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside unlockBatchStage (method)");
        String sql = "select rsetid from rsetitems where sdcid='LV_BatchStage'  and exists (" +
                " select null from s_batchstage bs where bs.s_batchstageid=rsetitems.keyid1" +
                " and bs.batchid= ? ) ";

        DataSet ds = getQueryProcessor().getPreparedSqlDataSet(sql, new Object[]{limsBatchId});
        if (ds != null && ds.size() > 0) {
            getDAMProcessor().clearRSet(ds.getValue(0, "rsetid", ""));
        }

    }

    /***************************************************************************************************
     * This method is used to create Child Sample Id and copy Parent Sample SDI details to Child Sample.
     * @param parentSampleId Parent Sample Id
     * @return Child Sample Id
     * @throws SapphireException OOB Sapphire Exception
     ****************************************************************************************************/

    private String copyParentSampleSDIDetails(String parentSampleId) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside copyParentSampleSDIDetails (method)");
        String newSampleId = "";
        PropertyList plCopySDIDetails = new PropertyList();
        plCopySDIDetails.setProperty(CopySDIDetail.PROPERTY_SDCID, __SDC_SAMPLE);
        plCopySDIDetails.setProperty(CopySDIDetail.PROPERTY_SOURCESDCID, __SDC_SAMPLE);
        plCopySDIDetails.setProperty(CopySDIDetail.PROPERTY_SOURCEKEYID1, parentSampleId);
        /*plCopySDIDetails.setProperty(CopySDIDetail.PROPERTY_COPYWORKITEM, "Y");
        plCopySDIDetails.setProperty(CopySDIDetail.PROPERTY_APPLYSOURCEWORKITEM, "Y");*/
        plCopySDIDetails.setProperty(CopySDIDetail.PROPERTY_COPYSPEC, "Y");
        try {
            getActionProcessor().processAction(CopySDIDetail.ID, CopySDIDetail.VERSIONID, plCopySDIDetails);
        } catch (SapphireException ex) {
            throw new SapphireException("General Error: ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside method copyParentSampleSDIDetails. Error is " + ex.getMessage()));
        }

        newSampleId = plCopySDIDetails.getProperty(__NEW_KEY_ID, "");
        return newSampleId;

    }

    /************************************************************************************
     * This method is used to add replicate details from Parent Samples to Child Samples.
     * @param newSampleId Child Sample Id
     * @param parentSampleId Parent Sample Id
     * @throws SapphireException OOB Sapphire Exception
     **************************************************************************************/
    private void addReplicateDetails(String newSampleId, String parentSampleId) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside addReplicateDetails (method)");
        // Find the Replicates if available for the Sample
        String sqlReplicateQuery = "SELECT '" + newSampleId + "' keyid1,paramlistid,paramlistversionid,variantid,dataset,paramid,paramtype, TO_CHAR(replicateid) repcount " +
                " FROM sdidataitem where keyid1= ? AND sdcid = ? ";
        DataSet dsReplicateDetails = getQueryProcessor().getPreparedSqlDataSet(sqlReplicateQuery, new Object[]{parentSampleId, __SDC_SAMPLE});

        if (null == dsReplicateDetails) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside addReplicateDetails - unable to execute sql sqlReplicateQuery. "));
        }

        // ****** Mandatory Sort before Group DataSet
        dsReplicateDetails.sort("keyid1 , paramlistid, paramlistversionid, variantid, dataset, paramid, paramtype");
        // group by keyid1,paramlistid,paramlistversionid,variantid,dataset,paramid,paramtype
        ArrayList alDataSet = dsReplicateDetails.getGroupedDataSets("keyid1 , paramlistid, paramlistversionid, variantid, dataset, paramid, paramtype");
        //ArrayList alDataSet = dsReplicateDetails.getGroupedDataSets("keyid1, sourceworkitemid, sourceworkiteminstance, paramlistid, paramlistversionid, variantid, dataset, paramid, paramtype");
        PropertyList plFilter = new PropertyList();
        DataSet dsReplicate = new DataSet();
        DataSet dsFilteredReplicate = new DataSet();
        // Looping Each group
        for (int dsNo = 0; dsNo < alDataSet.size(); dsNo++) {
            plFilter.clear();
            dsReplicate.clear();
            dsFilteredReplicate.clear();
            // Getting each group DataSet
            dsReplicate = (DataSet) alDataSet.get(dsNo);
            if (dsReplicate.size() > 1) {
                //  DataSet Having max(replicateid)>1
                plFilter.setProperty("repcount", "1");
                dsFilteredReplicate = dsReplicate.getFilteredDataSet(plFilter, true);

                int numreplicate = 0;

                if (dsFilteredReplicate.getRowCount() > 0) {
                    // Getting max replicateid at top
                    dsFilteredReplicate.sort("repcount D");
                    // Calculating number of replicate
                    numreplicate = Integer.valueOf(dsFilteredReplicate.getValue(0, "repcount", "0")) - 1;
                    PropertyList plAddReplicate = new PropertyList();
                    plAddReplicate.setProperty(AddReplicate.PROPERTY_SDCID, __SDC_SAMPLE);
                    plAddReplicate.setProperty(AddReplicate.PROPERTY_KEYID1, newSampleId);
                    plAddReplicate.setProperty(AddReplicate.PROPERTY_PARAMLISTID, dsFilteredReplicate.getValue(0, "paramlistid", ";"));
                    plAddReplicate.setProperty(AddReplicate.PROPERTY_PARAMLISTVERSIONID, dsFilteredReplicate.getValue(0, "paramlistversionid", ";"));
                    //plAddReplicate.setProperty(AddReplicate.PROPERTY_PARAMLISTVERSIONID, "".equalsIgnoreCase(getCurrentParamList(dsFilteredReplicate.getValue(0, "paramlistid", ";"), dsFilteredReplicate.getValue(0, "variantid", ";"))) ? dsFilteredReplicate.getValue(0, "paramlistversionid", ";") : getCurrentParamList(dsFilteredReplicate.getValue(0, "paramlistid", ";"), dsFilteredReplicate.getValue(0, "variantid", ";")));
                    plAddReplicate.setProperty(AddReplicate.PROPERTY_VARIANTID, dsFilteredReplicate.getValue(0, "variantid", ";"));
                    plAddReplicate.setProperty(AddReplicate.PROPERTY_DATASET, dsFilteredReplicate.getValue(0, "dataset", ";"));
                    plAddReplicate.setProperty(AddReplicate.PROPERTY_PARAMID, dsFilteredReplicate.getValue(0, "paramid", ";"));
                    plAddReplicate.setProperty(AddReplicate.PROPERTY_PARAMTYPE, dsFilteredReplicate.getValue(0, "paramtype", ";"));
                    plAddReplicate.setProperty(AddReplicate.PROPERTY_NUMREPLICATE, String.valueOf(numreplicate));
                    plAddReplicate.setProperty("propsmatch", "Y");
                    try {
                        getActionProcessor().processAction(AddReplicate.ID, AddReplicate.VERSIONID, plAddReplicate);
                    } catch (SapphireException ex) {
                        throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Inside addReplicateDetails - failed to execute AddReplicate. "));
                    }
                }
            }

        }

    }

    /*********************************************************************
     * This method is used to add adhoc DataSets to child sample which was referenced in parent sample
     * @param newSampleId Newly created Sample Id.
     * @param parentSampleId Existing Sample Id.
     * @throws SapphireException OOB Sapphire Exception.
     *********************************************************************/

    private void addAdhocDataSet(String newSampleId, String parentSampleId) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside addAdhocDataSet (method)");
        String sqlAdhocDS = " SELECT paramlistid,paramlistversionid, variantid, dataset FROM sdidata WHERE sdcid = ? AND keyid1 = ?";
        DataSet dsAdhocDS = getQueryProcessor().getPreparedSqlDataSet(sqlAdhocDS, new Object[]{__SDC_SAMPLE, parentSampleId});
        if (dsAdhocDS == null) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside addAdhocDataSet - unable to execute sql sqlAdhocDS. "));
        }
        if (dsAdhocDS.size() > 0) {
            PropertyList plAddDataSet = new PropertyList();
            plAddDataSet.setProperty(AddDataSet.PROPERTY_SDCID, __SDC_SAMPLE);
            plAddDataSet.setProperty(AddDataSet.PROPERTY_KEYID1, newSampleId);
            plAddDataSet.setProperty(AddDataSet.PROPERTY_PARAMLISTID, dsAdhocDS.getColumnValues("paramlistid", ";"));
            plAddDataSet.setProperty(AddDataSet.PROPERTY_PARAMLISTVERSIONID, dsAdhocDS.getColumnValues("paramlistversionid", ";"));
            plAddDataSet.setProperty(AddDataSet.PROPERTY_VARIANTID, dsAdhocDS.getColumnValues("variantid", ";"));
            plAddDataSet.setProperty(AddDataSet.PROPERTY_DATASET, dsAdhocDS.getColumnValues("dataset", ";"));
            plAddDataSet.setProperty("s_datasetstatus", "Initial");
            plAddDataSet.setProperty(AddDataSet.PROPERTY_AUDITREASON, "SyncCOASample");

            try {
                getActionProcessor().processAction(AddDataSet.ID, AddDataSet.VERSIONID, plAddDataSet);
            } catch (SapphireException ex) {
                throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Inside addAdhocDataSet - failed to execute CopyDataSet. "));
            }

        }
    }

    /**********************************************************************************
     * This method is used to update DataSet information to the Child Sample
     * @param dsLatestDS DataSet containing latest Parameter List details.
     * @throws SapphireException OOB Sapphire Exception
     **********************************************************************************/
    private void updateDataSet(DataSet dsLatestDS) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside updateDataSet (method)");
        // Identify the DataSet Value for Existiing Sample
        DataSet dsUpdateDataSet = dsLatestDS;
        if (null == dsUpdateDataSet) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Unable to execute sql sqlupdateDataSet inside - updateDataSet"));
        }
        if (dsUpdateDataSet.getRowCount() > 0) {
            PropertyList plEditDataSet = new PropertyList();
            plEditDataSet.setProperty("sdcid", __SDC_SAMPLE);
            plEditDataSet.setProperty("keyid1", dsUpdateDataSet.getColumnValues("newsampleid", ";"));
            plEditDataSet.setProperty("paramlistid", dsUpdateDataSet.getColumnValues("paramlistid", ";"));
            plEditDataSet.setProperty("paramlistversionid", dsUpdateDataSet.getColumnValues("plcurrentversion", ";"));
            plEditDataSet.setProperty("variantid", dsUpdateDataSet.getColumnValues("variantid", ";"));
            plEditDataSet.setProperty("dataset", dsUpdateDataSet.getColumnValues("dataset", ";"));
            plEditDataSet.setProperty("s_datasetstatus", dsUpdateDataSet.getColumnValues("s_datasetstatus", ";"));
            plEditDataSet.setProperty("s_retestedflag", dsUpdateDataSet.getColumnValues("s_retestedflag", ";"));
            plEditDataSet.setProperty("s_remeasuredflag", dsUpdateDataSet.getColumnValues("s_remeasuredflag", ";"));
            plEditDataSet.setProperty("s_remeasureinstance", dsUpdateDataSet.getColumnValues("s_remeasureinstance", ";"));
            plEditDataSet.setProperty("sourceworkitemid", dsUpdateDataSet.getColumnValues("sourceworkitemid", ";"));
            plEditDataSet.setProperty("sourceworkiteminstance", dsUpdateDataSet.getColumnValues("sourceworkiteminstance", ";"));
            plEditDataSet.setProperty("u_sapresultuploaded", dsUpdateDataSet.getColumnValues("u_sapresultuploaded", ";"));
            plEditDataSet.setProperty("auditreason", "SyncCOASample");
            plEditDataSet.setProperty("propsmatch", "Y");
            try {
                getActionProcessor().processAction("EditDataSet", "1", plEditDataSet);

            } catch (SapphireException ex) {
                throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Inside  remeasuredOrRetestedDataSet. - failed to edit DataSet. "));
            }
        }
    }

    /***************************************************************************
     * This method is used to get stage name from Policy - COAParentSamplePolicy
     * @return Stage Name
     * @throws SapphireException OOB Sapphire exception
     ***************************************************************************/
    private String getStageFromPolicy() throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside getStageFromPolicy (method)");
        String stageName = "";
        try {
            stageName = getConfigurationProcessor().getPolicy("COAParentSamplePolicy", "setValue").getProperty("stagename");
        } catch (SapphireException ex) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Inside  getStageFromPolicy - failed to get policy detais. "));
        }
        return stageName;
    }

    /**************************************************************************************
     * This method is used to find Batch Stage Id against Batch Stage name got from Policy.
     * @param strChildBatchIds Child LIMS Batch Id.
     * @param strPolicyStageName Stage Name from COAParentSamplePolicy policy.
     * @return DataSet of Batch Stage Id details.
     * @throws SapphireException OOB Sapphire exception.
     ***************************************************************************************/
    private DataSet findStageIdFromCurrentBatch(String strChildBatchIds, String strPolicyStageName) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside findStageIdFromCurrentBatch (method)");
        String sqlBatchStageQuery = "SELECT s_batchstage.s_batchstageid,s_batch.productid,s_batch.productversionid,s_batch.prodvariantid FROM s_batchstage,s_batch" +
                " WHERE s_batch.s_batchid=s_batchstage.batchid AND s_batchstage.batchid= ? AND s_batchstage.label= ? ";
        DataSet dsBatchStage = getQueryProcessor().getPreparedSqlDataSet(sqlBatchStageQuery, new Object[]{strChildBatchIds, strPolicyStageName});
        if (null == dsBatchStage) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Inside  findStageIdFromCurrentBatch - unable to create DataSet: dsBatchStage. "));
        }
        if (dsBatchStage.getRowCount() == 0) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Inside  findStageIdFromCurrentBatch - Batch Stage is blank in Policy/Not Available in Current Batch..!"));
        }

        return dsBatchStage;
    }

    /******************************************************************************************
     * Thid method is used to get Sampling Plan Spec details corresponing to Policy stage name.
     * @param strChildBatchIds Child LIMS Batch Id.
     * @param strPolicyStageName Stage Name from policy.
     * @return DataSet of Sampling Plan.
     * @throws SapphireException OOB Sapphire Exception.
     ******************************************************************************************/
    private DataSet findSpecFromSamplingPlan(String strChildBatchIds, String strPolicyStageName) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside findSpecFromSamplingPlan (method)");
        String sqlFindSpec = "SELECT sitem.itemsdcid,sstage.label,sitem.itemkeyid1,sitem.itemkeyid2,sb.batchdesc,sb.productid,sb.productversionid,splan.s_samplingplanid," +
                "splan.s_samplingplanversionid, sdetail.levelid, sdetail.sourcelabel,  sdetail.templatekeyid1, sdetail.defaultdepartmentid" +
                " FROM S_SAMPLINGPLAN SPLAN INNER JOIN S_PROCESSSTAGE SSTAGE ON SPLAN.S_SAMPLINGPLANID        =SSTAGE.S_SAMPLINGPLANID" +
                " AND SPLAN.S_SAMPLINGPLANVERSIONID=SSTAGE.S_SAMPLINGPLANVERSIONID INNER JOIN S_SPDETAIL SDETAIL ON SDETAIL.S_SAMPLINGPLANID =SSTAGE.S_SAMPLINGPLANID" +
                " AND SDETAIL.S_SAMPLINGPLANVERSIONID=SSTAGE.S_SAMPLINGPLANVERSIONID" +
                " AND SSTAGE.S_PROCESSSTAGEID  = SDETAIL.PROCESSSTAGEID INNER JOIN S_SPDETAILITEM SDETAILITEM ON SDETAILITEM.S_SAMPLINGPLANID        =SDETAIL.S_SAMPLINGPLANID AND SDETAILITEM.S_SAMPLINGPLANVERSIONID=SDETAIL.S_SAMPLINGPLANVERSIONID AND SDETAILITEM.S_SAMPLINGPLANDETAILNO =SDETAIL.S_SAMPLINGPLANDETAILNO INNER JOIN S_SPITEM SITEM ON SITEM.S_SAMPLINGPLANID        =SSTAGE.S_SAMPLINGPLANID AND SITEM.S_SAMPLINGPLANVERSIONID=SSTAGE.S_SAMPLINGPLANVERSIONID" +
                " AND SITEM.S_SAMPLINGPLANITEMNO   =SDETAILITEM.S_SAMPLINGPLANITEMNO INNER JOIN S_BATCH SB ON SB.SAMPLINGPLANID        =SPLAN.S_SAMPLINGPLANID AND SB.SAMPLINGPLANVERSIONID=SPLAN.S_SAMPLINGPLANVERSIONID" +
                " WHERE SB.S_BATCHID  = ? AND label  = ? AND SITEM.ITEMSDCID  ='SpecSDC' ORDER BY sitem.itemsdcid";

        DataSet dsFindSpecs = getQueryProcessor().getPreparedSqlDataSet(sqlFindSpec, new Object[]{strChildBatchIds, strPolicyStageName});
        if (null == dsFindSpecs) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, "Unable to execute sql query inside findSpecFromSamplingPlan --> sqlFindSpec ");
        }

        return dsFindSpecs;
    }

    /***************************************************************
     * This method is used to find and remove existing Spec details.
     * @param newSampleId Child Sample Id.
     * @throws SapphireException OOB Sapphire Exception.
     ***************************************************************/
    private void findAndRemoveExistingSpec(String newSampleId) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside findAndRemoveExistingSpec (method)");
        String sqlPresentSampleSpec = "SELECT specid,SPECVERSIONID FROM sdispec where keyid1= ? AND sdcid = ? ";
        DataSet dsPresentSampleSepc = getQueryProcessor().getPreparedSqlDataSet(sqlPresentSampleSpec, new Object[]{newSampleId, __SDC_SAMPLE});
        if (null == dsPresentSampleSepc) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Inside findAndRemoveExistingSpec - unable to create DataSet. "));
        }
        PropertyList plSpecList = new PropertyList();
        // Remove the Existing Spec only if Present
        if (dsPresentSampleSepc.getRowCount() > 0) {
            plSpecList.setProperty("sdcid", __SDC_SAMPLE);
            plSpecList.setProperty("keyid1", newSampleId);
            plSpecList.setProperty("specid", dsPresentSampleSepc.getColumnValues("specid", ";"));
            plSpecList.setProperty("specversionid", dsPresentSampleSepc.getColumnValues("specversionid", ";"));
            plSpecList.setProperty("auditreason", "SyncCOASample");
            try {
                getActionProcessor().processAction("RemoveSDISpec", "1", plSpecList);
            } catch (SapphireException ex) {
                throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Inside findAndRemoveExistingSpec - unable to remove Spec details from Sample. "));
            }

        }
    }

    /**************************************************************
     * This method is used to add new Spec details to Child Sample.
     * @param newSampleId Child Sample Id.
     * @param dsFindSpecs Existing Spec details.
     * @throws SapphireException OOB Sapphire Exception.
     ***************************************************************/
    private void addNewSpec(String newSampleId, DataSet dsFindSpecs) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside addNewSpec (method)");
        // Add new Spec to the Sample
        PropertyList plSpecList = new PropertyList();
        if (dsFindSpecs.getRowCount() > 0) {
            plSpecList.clear();
            plSpecList.setProperty("sdcid", __SDC_SAMPLE);
            plSpecList.setProperty("keyid1", newSampleId);
            plSpecList.setProperty("specid", dsFindSpecs.getColumnValues("itemkeyid1", ";"));
            plSpecList.setProperty("specversionid", dsFindSpecs.getColumnValues("itemkeyid2", ";"));
            plSpecList.setProperty("auditreason", "SyncCOASample");
            try {
                getActionProcessor().processAction("AddSDISpec", "1", plSpecList);
            } catch (SapphireException ex) {
                throw new SapphireException(" General Error ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside addNewSpec - unable to add Spec details in Sample."));
            }

        }
    }

    /******************************************************************
     * Method used for updating new Sample with Parent Sample details.
     * @param newSampleId New Sample Id
     * @param batchstageid Batch Stage Id
     * @param strChildBatchIds Child LIMS Batch
     * @param productid Product Id
     * @param productversionid Product version
     * @param sourcespsourcelabel Source label
     * @param sourcesplevelid Sample level
     * @param sourcespid Source Sampling Plan Id
     * @param sourcespversionid Source Sampling Plan Version Id
     * @throws SapphireException OOB Sapphire Exception
     *********************************************************************/
    private void updateSampleDetails(String newSampleId, String batchstageid, String strChildBatchIds, String productid, String productversionid, String sourcespsourcelabel, String sourcesplevelid, String sourcespid, String sourcespversionid) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside updateSampleDetails (method)");
        // ***************** Getting some SAP related fields
        String sqlText = "SELECT u_issapsampleflag,u_markedforcancel, u_payloadrejectedbatchflag, u_inspectionlot FROM s_sample WHERE s_sampleid = ? ";
        DataSet dsSAPField = getQueryProcessor().getPreparedSqlDataSet(sqlText, new Object[]{newSampleId});
        if (null == dsSAPField) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Inside updateSampleDetails - unable to get SAP fields from Sample. "));
        }
        // Update New Created Sample Information
        PropertyList plUpdateSample = new PropertyList();
        plUpdateSample.setProperty("sdcid", __SDC_SAMPLE);
        plUpdateSample.setProperty("keyid1", newSampleId);
        plUpdateSample.setProperty("batchstageid", batchstageid);
        plUpdateSample.setProperty("batchid", strChildBatchIds);
        plUpdateSample.setProperty("productid", productid);
        plUpdateSample.setProperty("productversionid", productversionid);
        plUpdateSample.setProperty("samplestatus", "Reviewed");
        plUpdateSample.setProperty("sourcespsourcelabel", sourcespsourcelabel);
        plUpdateSample.setProperty("sourcesplevelid", sourcesplevelid);
        plUpdateSample.setProperty("sourcespid", sourcespid);
        plUpdateSample.setProperty("sourcespversionid", sourcespversionid);
        plUpdateSample.setProperty("auditactivity", "SyncCOASample");
        plUpdateSample.setProperty("reviewdisposition", "Approved");
        plUpdateSample.setProperty("disposalstatus", "Y");
        plUpdateSample.setProperty("receiveddt", "N");
        plUpdateSample.setProperty("u_issapsampleflag", dsSAPField.getValue(0, "u_issapsampleflag", ""));
        plUpdateSample.setProperty("u_markedforcancel", dsSAPField.getValue(0, "u_markedforcancel", ""));
        plUpdateSample.setProperty("u_payloadrejectedbatchflag", dsSAPField.getValue(0, "u_payloadrejectedbatchflag", ""));
        plUpdateSample.setProperty("u_inspectionlot", dsSAPField.getValue(0, "u_inspectionlot", ""));
        try {
            getActionProcessor().processAction("EditSDI", "1", plUpdateSample);
        } catch (SapphireException ex) {
            throw new SapphireException(" General Error ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside updateSampleDetails - unable to update Sample."));
        }

    }

    /**********************************************************************
     * This method is used to get result.
     * @param newSampleId Child Sample id
     * @param existingSampleId Parent Sample Id
     * @return DataSet of results
     * @throws SapphireException OOB Sapphire exception.
     **********************************************************************/
    private DataSet getResults(String newSampleId, String existingSampleId) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside getResults (method)");
        // ********* Added for testing **************//
        String rsetId = "";
        DataSet dsDataValue = new DataSet();
        try {
            // Creating RSET using DAM Processor
            rsetId = getDAMProcessor().createRSet(__SDC_SAMPLE, existingSampleId, "(null)", "(null)");
            // Identify the DataItem Value for Existiing Sample by joinig with rsetitems table
            String sqlDataEntryValue = "SELECT sd.sdcid,'" + newSampleId + "' newsampleid, sd.paramlistid,sd.paramlistversionid,sd.variantid,sd.dataset, TO_CHAR(sd.dataset) str_dataset,sd.paramid,sd.paramtype,sd.replicateid, TO_CHAR(sd.replicateid) str_replicateid,sd.datatypes,sd.enteredtext,sd.transformdt,sd.calcexcludeflag, sd.u_sapconfnum, sd.u_sapselectedset, sd.u_issapresult FROM sdidataitem sd, rsetitems rs WHERE sd.sdcid = rs.sdcid AND sd.keyid1= rs.keyid1 AND sd.keyid2 = rs.keyid2 AND sd.keyid3 = rs.keyid3 AND rs.rsetid = ? ";
            dsDataValue = getQueryProcessor().getPreparedSqlDataSet(sqlDataEntryValue, new Object[]{rsetId});
        } catch (SapphireException ex) {
            throw new SapphireException("Unable to execute query  sqlDataEntryValue inside method - getResults");
        } finally {
            if (!"".equalsIgnoreCase(rsetId)) {
                getDAMProcessor().clearRSet(rsetId);
            }
        }

        return dsDataValue;
    }

    /*******************************************************************
     * ***********  Code added to resolve below problem ****************
     *  Problem Desc : To resolve actual date problem while input as 'N'
     *  Change Desc : Query Changed. Case statement added
     *  Changed By : Kaushik Ghosh, CTS
     *  Changed On : 20-OCT-2020
     * @param newSampleId Newly created Sample Id
     * @return DataSet
     * @throws SapphireException OOB Sapphire Exception
     *********************************************************************/
    private DataSet newSampleDataEntry(String newSampleId, DataSet dsLatestDataItem) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside newSampleDataEntry (method)");
        // ***************** If Some DataItem value exists ****************
        if (dsLatestDataItem.size() > 0) {
            //Logic added to handle date input n or N
            if (!dsLatestDataItem.isValidColumn("finaltext")) {
                dsLatestDataItem.addColumn("finaltext", DataSet.STRING);
            }

            for (int row = 0; row < dsLatestDataItem.getRowCount(); row++) {
                String enteredText = dsLatestDataItem.getValue(row, "enteredtext", "");
                // *************** Checking if Date / Time value is entered as N or N+ or N- format
                if (enteredText.equalsIgnoreCase("n") || enteredText.contains("n+") || enteredText.contains("n-") || enteredText.contains("t")) {
                    // ************* Checking if transformdt column is NULL or not
                    if (!"".equalsIgnoreCase(dsLatestDataItem.getValue(row, "transformdt", ""))) {
                        dsLatestDataItem.setValue(row, "finaltext", dsLatestDataItem.getValue(row, "transformdt", ""));
                    } else {
                        dsLatestDataItem.setValue(row, "finaltext", dsLatestDataItem.getValue(row, "enteredtext", ""));
                    }
                } else {
                    dsLatestDataItem.setValue(row, "finaltext", dsLatestDataItem.getValue(row, "enteredtext", ""));
                }
            }
            // Data Entry of new Sample based on Existing Sample
            if (dsLatestDataItem.getRowCount() > 0) {

                // Data Entry of new Sample based on Existing Sample
                PropertyList plNewSampleDE = new PropertyList();
                plNewSampleDE.setProperty("sdcid", __SDC_SAMPLE);
                plNewSampleDE.setProperty("keyid1", dsLatestDataItem.getColumnValues("newsampleid", ";"));
                plNewSampleDE.setProperty("paramlistid", dsLatestDataItem.getColumnValues("paramlistid", ";"));
                plNewSampleDE.setProperty("paramlistversionid", dsLatestDataItem.getColumnValues("plcurrentversion", ";"));
                plNewSampleDE.setProperty("variantid", dsLatestDataItem.getColumnValues("variantid", ";"));
                plNewSampleDE.setProperty("dataset", dsLatestDataItem.getColumnValues("dataset", ";"));
                plNewSampleDE.setProperty("paramid", dsLatestDataItem.getColumnValues("paramid", ";"));
                plNewSampleDE.setProperty("paramtype", dsLatestDataItem.getColumnValues("paramtype", ";"));
                plNewSampleDE.setProperty("replicateid", dsLatestDataItem.getColumnValues("replicateid", ";"));
                plNewSampleDE.setProperty("enteredtext", dsLatestDataItem.getColumnValues("finaltext", ";"));
                plNewSampleDE.setProperty("overridereleased", "Y");
                plNewSampleDE.setProperty("auditactivity", "SyncCOASample");
                //plNewSampleDE.setProperty("propsmatch", "Y");
                try {
                    getActionProcessor().processAction("EnterDataItem", "1", plNewSampleDE);
                } catch (SapphireException ex) {
                    throw new SapphireException(" General Error ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside newSampleDataEntry - unable to enter result to Sample." + newSampleId + " " + ex.getMessage()));
                }
            }
        }
        return dsLatestDataItem;
    }

    /***********************************************************************************************
     * This method is used to get Parameter List details for Child Sample
     * @param newSampleId Child Sample Id
     * @param existingSampleId Parent Sample Id
     * @return DataSet containing Parameter List associated with Parent Sample Id.
     * @throws SapphireException OOB Sapphire Exception.
     ************************************************************************************************/
    private DataSet getDataSet(String newSampleId, String existingSampleId) throws SapphireException {
        String rsetId = "";
        DataSet dsDataSet = new DataSet();
        String sqlupdateDataSet = "SELECT '" + newSampleId + "' newsampleid,paramlistid,paramlistversionid,variantid,dataset, TO_CHAR(dataset) str_dataset, s_datasetstatus, s_retestedflag, s_remeasuredflag, s_remeasureinstance, TO_CHAR(s_remeasureinstance) str_s_remeasureinstance, sourceworkitemid,  sourceworkiteminstance, u_sapresultuploaded" +
                " FROM sdidata sd, rsetitems rs WHERE sd.sdcid = rs.sdcid AND sd.keyid1= rs.keyid1 AND sd.keyid2 = rs.keyid2 AND sd.keyid3 = rs.keyid3 AND rs.rsetid = ? ";
        try {
            rsetId = getDAMProcessor().createRSet(__SDC_SAMPLE, existingSampleId, "(null)", "(null)");
            String[] strParams = new String[]{rsetId};
            dsDataSet = getQueryProcessor().getPreparedSqlDataSet(sqlupdateDataSet, strParams);
        } catch (Exception ex) {
            throw new SapphireException(" General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Inside getDataSet - " + ex.getMessage()));
        } finally {
            if (!"".equalsIgnoreCase(rsetId)) {
                getDAMProcessor().clearRSet(rsetId);
            }
        }
        return dsDataSet;
    }

    /*************************************************************************
     * This method is used to get Latest Parameter List version information.
     * @param newSampleId Child Sample Id
     * @param existingSampleId Parent Sample Id
     * @return DataSet containing Parameter Lis information.
     * @throws SapphireException OOB Sapphire Exception.
     **************************************************************************/
    private DataSet getLatestDataSet(String newSampleId, String existingSampleId) throws SapphireException {
        // ********* Getting Parent Sample SDIData
        DataSet dsParentSampleDS = getDataSet(newSampleId, existingSampleId);
        // ********* Getting Child Sample SDIData
        DataSet dsChildSampleDS = getDataSet(newSampleId, newSampleId);
        // ********** Creating Final DataSet *************
        if (!dsParentSampleDS.isValidColumn("plcurrentversion")) {
            dsParentSampleDS.addColumn("plcurrentversion", DataSet.STRING);
        }
        PropertyList plDataItemFilter = new PropertyList();
        int findRowNum;
        for (int parentCount = 0; parentCount < dsParentSampleDS.size(); parentCount++) {
            plDataItemFilter.clear();
            plDataItemFilter.setProperty("newsampleid", newSampleId);
            plDataItemFilter.setProperty("paramlistid", dsParentSampleDS.getValue(parentCount, "paramlistid"));
            plDataItemFilter.setProperty("variantid", dsParentSampleDS.getValue(parentCount, "variantid"));
            plDataItemFilter.setProperty("str_dataset", dsParentSampleDS.getValue(parentCount, "str_dataset"));
            findRowNum = dsChildSampleDS.findRow(plDataItemFilter);
            if (findRowNum != -1) {
                dsParentSampleDS.setValue(parentCount, "plcurrentversion", dsChildSampleDS.getValue(findRowNum, "paramlistversionid", "(null)"));
            } else {
                dsParentSampleDS.setValue(parentCount, "plcurrentversion", dsParentSampleDS.getValue(parentCount, "paramlistversionid", "(null)"));

            }
        }
        return dsParentSampleDS;
    }

    /******************************************************************
     * This method is used to get latest result information
     * @param newSampleId Child Sample id
     * @param existingSampleId Parent Sample Id
     * @return DataSet containing result information.
     * @throws SapphireException OOB Sapphire Exception.
     ********************************************************************/
    private DataSet getLatestDataItem(String newSampleId, String existingSampleId) throws SapphireException {
        // Identify the DataItem Value for Existiing Sample
        PropertyList plFilter = new PropertyList();
        // ********* Getting Parent Sample SDIDataItem
        DataSet dsDataValue = getResults(newSampleId, existingSampleId);
        // ********* Getting Child Sample SDIDataItem
        DataSet dsChildDataValue = getResults(newSampleId, newSampleId);
        // ********** Creating Final DataSet *************
        if (!dsDataValue.isValidColumn("plcurrentversion")) {
            dsDataValue.addColumn("plcurrentversion", DataSet.STRING);
        }
        PropertyList plDataItemFilter = new PropertyList();
        int findRowNum;
        for (int parentCount = 0; parentCount < dsDataValue.size(); parentCount++) {
            plDataItemFilter.clear();
            plDataItemFilter.setProperty("newsampleid", newSampleId);
            plDataItemFilter.setProperty("paramlistid", dsDataValue.getValue(parentCount, "paramlistid"));
            plDataItemFilter.setProperty("variantid", dsDataValue.getValue(parentCount, "variantid"));
            plDataItemFilter.setProperty("str_dataset", dsDataValue.getValue(parentCount, "dataset"));
            plDataItemFilter.setProperty("paramid", dsDataValue.getValue(parentCount, "paramid"));
            plDataItemFilter.setProperty("paramtype", dsDataValue.getValue(parentCount, "paramtype"));
            plDataItemFilter.setProperty("str_replicateid", dsDataValue.getValue(parentCount, "replicateid"));
            findRowNum = dsChildDataValue.findRow(plDataItemFilter);
            if (findRowNum != -1) {
                dsDataValue.setValue(parentCount, "plcurrentversion", dsChildDataValue.getValue(findRowNum, "paramlistversionid", "(null)"));
            } else {
                dsDataValue.setValue(parentCount, "plcurrentversion", dsDataValue.getValue(parentCount, "paramlistversionid", "(null)"));

            }
        }
        return dsDataValue;
    }

    /**********************************************************
     * This method is used to edit dataitem values.
     * @param newSampleId New Sample Id
     * @throws SapphireException OOB Sapphire Exception
     ***********************************************************/
    private void EditDataItem(String newSampleId, DataSet dsDataValue) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside EditDataItem (method)");
        PropertyList plFilter = new PropertyList();
        if (dsDataValue.size() > 0) {
            // Editing DataItem based on Existing Sample
            PropertyList plNewSampleDE = new PropertyList();
            plNewSampleDE.setProperty("sdcid", __SDC_SAMPLE);
            plNewSampleDE.setProperty("keyid1", dsDataValue.getColumnValues("newsampleid", ";"));
            plNewSampleDE.setProperty("paramlistid", dsDataValue.getColumnValues("paramlistid", ";"));
            plNewSampleDE.setProperty("paramlistversionid", dsDataValue.getColumnValues("plcurrentversion", ";"));
            plNewSampleDE.setProperty("variantid", dsDataValue.getColumnValues("variantid", ";"));
            plNewSampleDE.setProperty("dataset", dsDataValue.getColumnValues("dataset", ";"));
            plNewSampleDE.setProperty("paramid", dsDataValue.getColumnValues("paramid", ";"));
            plNewSampleDE.setProperty("paramtype", dsDataValue.getColumnValues("paramtype", ";"));
            plNewSampleDE.setProperty("replicateid", dsDataValue.getColumnValues("replicateid", ";"));
            plNewSampleDE.setProperty("u_sapconfnum", dsDataValue.getColumnValues("u_sapconfnum", ";"));
            plNewSampleDE.setProperty("u_issapresult", dsDataValue.getColumnValues("u_issapresult", ";"));
            plNewSampleDE.setProperty("u_sapselectedset", dsDataValue.getColumnValues("u_sapselectedset", ";"));
            plNewSampleDE.setProperty("calcexcludeflag", dsDataValue.getColumnValues("calcexcludeflag", ";"));
            plNewSampleDE.setProperty("propsmatch", "Y");

            plNewSampleDE.setProperty("auditactivity", "SyncCOASample");
            try {
                getActionProcessor().processAction("EditDataItem", "1", plNewSampleDE);
            } catch (SapphireException ex) {
                throw new SapphireException(" General Error ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside EditDataItem - unable to edit result to Sample." + newSampleId + " " + ex.getMessage()));
            }
        }
    }

    /***********************************************************************
     * This method is used to add Child Sample details to u_parentlotsamples
     * @param newSampleId Child Sample Id.
     * @param parentLotSampleId Parent Sample Id.
     * @throws SapphireException OOB Sapphire exception.
     ***********************************************************************/
    private void updateParentLotSample(String newSampleId, String parentLotSampleId) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside updateParentLotSample (method)");
        PropertyList plParentLotSample = new PropertyList();
        plParentLotSample.setProperty("sdcid", "ParentLotSamples");
        plParentLotSample.setProperty("keyid1", parentLotSampleId);
        plParentLotSample.setProperty("auditactivity", "SyncCOASample");
        plParentLotSample.setProperty("childsampleid", newSampleId);
        try {
            getActionProcessor().processAction("EditSDI", "1", plParentLotSample);
        } catch (SapphireException ex) {
            throw new SapphireException(" General Error ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside updateParentLotSample - unable to update child sample in parentlotsample details."));
        }
    }

    /*****************************************************
     * This method is used to release Batch Stage.
     * @param batchStageId Batch Stage Id.
     * @throws SapphireException OOB Sapphire exception.
     ****************************************************/
    private void releaseBatchStage(String batchStageId) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside releaseBatchStage (method)");
        // Update Batch Stage Detail
        PropertyList plBatchStage = new PropertyList();
        plBatchStage.setProperty("sdcid", "LV_BatchStage");
        plBatchStage.setProperty("keyid1", batchStageId);
        plBatchStage.setProperty("batchstagestatus", "Released");
        plBatchStage.setProperty("auditactivity", "SyncCOASample");
        try {
            getActionProcessor().processAction("EditSDI", "1", plBatchStage);
        } catch (SapphireException ex) {
            throw new SapphireException(" General Error ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside releaseBatchStage - unable to release batch stage."));
        }

    }

    /******************************************************************
     * This method is used to update SDIWorkItem table for Child Sample.
     * @param newSampleId Child Sample Id
     * @param parentSampleId Parent Sample Id
     * @throws SapphireException OOB Sapphier Exception
     ******************************************************************/
    private void updateSDIWorkItemBySample(String newSampleId, String parentSampleId) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside updateSDIWorkItemBySample (method)");
        // Retrieve the Work Item Information from the Existing Sample
        String sqlWorkItem = "SELECT workitemid,workiteminstance, workitemstatus, appliedflag, s_retestedflag, s_retestinstance FROM sdiworkitem where sdcid= ? and keyid1= ? ";
        DataSet dsWorkItem = getQueryProcessor().getPreparedSqlDataSet(sqlWorkItem, new Object[]{__SDC_SAMPLE, parentSampleId});
        if (null == dsWorkItem) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Unable to execute query sqlWorkItem inside method - updateSDIWorkItemBySample"));
        }
        // Updating SDI WorkItem Status for new Sample
        PropertyList plSDIWorkItem = new PropertyList();
        plSDIWorkItem.setProperty("sdcid", __SDC_SAMPLE);
        plSDIWorkItem.setProperty("keyid1", newSampleId);
        plSDIWorkItem.setProperty("workitemid", dsWorkItem.getColumnValues("workitemid", ";"));
        plSDIWorkItem.setProperty("workiteminstance", dsWorkItem.getColumnValues("workiteminstance", ";"));
        plSDIWorkItem.setProperty("s_retestedflag", dsWorkItem.getColumnValues("s_retestedflag", ";"));
        plSDIWorkItem.setProperty("s_retestinstance", dsWorkItem.getColumnValues("s_retestinstance", ";"));
        plSDIWorkItem.setProperty("appliedflag", dsWorkItem.getColumnValues("appliedflag", ";"));
        plSDIWorkItem.setProperty("auditactivity", "SyncCOASample");
        //plSDIWorkItem.setProperty("workitemstatus", "Completed");
        plSDIWorkItem.setProperty("workitemstatus", dsWorkItem.getColumnValues("workitemstatus", ";"));
        try {
            getActionProcessor().processAction("EditSDIWorkItem", "1", plSDIWorkItem);
        } catch (SapphireException ex) {
            throw new SapphireException(" General Error ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside updateSDIWorkItemBySample - unable to update SDIWorkItem."));
        }

    }

    /*********************************************************
     * This method is used to release DataSet of child Sample
     * @param dsLatestDS
     * @throws SapphireException OOB Sapphire Exception
     *********************************************************/
    private void releaseDataSet(DataSet dsLatestDS) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside releaseDataSet (method)");
        DataSet dsDataValue = dsLatestDS;
        PropertyList plReleaseDataSet = new PropertyList();
        plReleaseDataSet.setProperty("sdcid", __SDC_SAMPLE);
        plReleaseDataSet.setProperty("keyid1", dsDataValue.getColumnValues("newsampleid", ";"));
        plReleaseDataSet.setProperty("paramlistid", dsDataValue.getColumnValues("paramlistid", ";"));
        plReleaseDataSet.setProperty("paramlistversionid", dsDataValue.getColumnValues("plcurrentversion", ";"));
        plReleaseDataSet.setProperty("variantid", dsDataValue.getColumnValues("variantid", ";"));
        plReleaseDataSet.setProperty("dataset", dsDataValue.getColumnValues("dataset", ";"));
        plReleaseDataSet.setProperty("releasedflag", "Y");
        // ********* If any Cancelled DataSet is having NULL Mandatory value
        plReleaseDataSet.setProperty("allowmandatorynulls", "Y");
        try {
            getActionProcessor().processAction("ReleaseDataSet", "1", plReleaseDataSet);
        } catch (SapphireException ex) {
            throw new SapphireException(" General Error ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside releaseDataSet - unable to release DataSet."));
        }
    }

    /***************************************************
     * This metod is used to release DataItem
     * @param dsDataValue Result value
     * @throws SapphireException OOB Sapphire exception
     **************************************************/
    private void releaseDataItem(DataSet dsDataValue) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside releaseDataItem (method)");
        // Release Data Item
        PropertyList plReleaseDataItem = new PropertyList();
        plReleaseDataItem.setProperty("sdcid", "Sample");
        plReleaseDataItem.setProperty("keyid1", dsDataValue.getColumnValues("newsampleid", ";"));
        plReleaseDataItem.setProperty("paramlistid", dsDataValue.getColumnValues("paramlistid", ";"));
        plReleaseDataItem.setProperty("paramlistversionid", dsDataValue.getColumnValues("plcurrentversion", ";"));
        plReleaseDataItem.setProperty("variantid", dsDataValue.getColumnValues("variantid", ";"));
        plReleaseDataItem.setProperty("dataset", dsDataValue.getColumnValues("dataset", ";"));
        plReleaseDataItem.setProperty("paramid", dsDataValue.getColumnValues("paramid", ";"));
        plReleaseDataItem.setProperty("paramtype", dsDataValue.getColumnValues("paramtype", ";"));
        plReleaseDataItem.setProperty("replicateid", dsDataValue.getColumnValues("replicateid", ";"));
        plReleaseDataItem.setProperty("allowmandatorynulls", "Y");
        plReleaseDataItem.setProperty("auditreason", "SyncCoASample");
        try {
            getActionProcessor().processAction("ReleaseDataItem", "1", plReleaseDataItem);
        } catch (SapphireException ex) {
            throw new SapphireException(" General Error ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside releaseDataItem - unable to release DataItem."));
        }
    }

    /**************************************************
     * This method is used to update DataSet
     * @param dsLatestDS
     * @throws SapphireException OOB Sapphire Exception
     ***************************************************/
    private void updateDataSetStatus(DataSet dsLatestDS) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside updateDataSetStatus (method)");
        DataSet dsDataValue = dsLatestDS;
        PropertyList plUpdateDataSet = new PropertyList();
        plUpdateDataSet.setProperty("sdcid", "Sample");
        plUpdateDataSet.setProperty("keyid1", dsDataValue.getColumnValues("newsampleid", ";"));
        plUpdateDataSet.setProperty("auditreason", "SyncCOASample");
        try {
            getActionProcessor().processAction("UpdateDatasetStatus", "1", plUpdateDataSet);
        } catch (SapphireException ex) {
            throw new SapphireException(" General Error ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside updateDataSetStatus - unable to update DataSet."));
        }
    }

    /***************************************************************
     * This mentod is used to Cancel parent Sample Cancelled DataSet
     * @param dsLatestDS
     * @throws SapphireException OOB Sapphire exception
     ****************************************************************/
    private void cancelDataSet(DataSet dsLatestDS) throws SapphireException {
        logger.debug("Processing " + ACTION_ID + ". (Action) : Inside cancelDataSet (method)");
        PropertyList plCancelDataSet = new PropertyList();
        plCancelDataSet.setProperty("s_datasetstatus", "Cancelled");
        DataSet dsCancelDataSet = dsLatestDS.getFilteredDataSet(plCancelDataSet);
        if (null == dsCancelDataSet) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Inside cancelDataSet - unable to execute sql sqlCancelDataSet inside - cancelDataSet"));
        }
        if (dsCancelDataSet.getRowCount() > 0) {
            plCancelDataSet.clear();
            plCancelDataSet.setProperty("sdcid", "Sample");
            plCancelDataSet.setProperty("keyid1", dsCancelDataSet.getColumnValues("newsampleid", ";"));
            plCancelDataSet.setProperty("paramlistid", dsCancelDataSet.getColumnValues("paramlistid", ";"));
            plCancelDataSet.setProperty("paramlistversionid", dsCancelDataSet.getColumnValues("paramlistversionid", ";"));
            plCancelDataSet.setProperty("variantid", dsCancelDataSet.getColumnValues("variantid", ";"));
            plCancelDataSet.setProperty("dataset", dsCancelDataSet.getColumnValues("dataset", ";"));
            plCancelDataSet.setProperty("auditreason", "SyncCOASample");
            plCancelDataSet.setProperty("cancelincompleteonly", "N");
            try {
                getActionProcessor().processAction("CancelDataSet", "1", plCancelDataSet);
            } catch (SapphireException ex) {
                throw new SapphireException(" General Error ", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate("Aborting transaction. Inside cancelDataSet - unable to cancel DataSet."));
            }
        }
    }

    /**************************************************************
     * Thsi method is used to get Current version of Parameter List
     * @param paramlListId ParamList Id
     * @param variantId Variant Id
     * @return Current version.
     * @throws SapphireException OOB Sapphire Exception.
     ***************************************************************/
    private String getCurrentParamList(String paramlListId, String variantId) throws SapphireException {
        String curParamListVer = "";
        String sqlCurParamList = "SELECT paramlistversionid FROM paramlist WHERE paramlistid = ? AND variantid = ? AND versionstatus = 'C' ";
        String[] paramCurParamList = new String[]{paramlListId, variantId};
        DataSet dsCurParamList = new DataSet();
        try {
            dsCurParamList = getQueryProcessor().getPreparedSqlDataSet(sqlCurParamList, paramCurParamList);
        } catch (Exception ex) {
            throw new SapphireException(" General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Unable to get current version of Paramlist - " + paramlListId + " , Variant - " + variantId));
        }

        if (dsCurParamList.size() > 0) {
            curParamListVer = dsCurParamList.getColumnValues("paramlistversionid", ";");
        }
        return curParamListVer;
    }
}