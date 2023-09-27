package labvantage.custom.alcon.actions;

import sapphire.SapphireException;
import sapphire.action.AddSDIDetail;
import sapphire.action.BaseAction;
import sapphire.action.EditSDI;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.xml.PropertyList;

/**
 * $Author: BAGCHAN1 $
 * $Date: 2022-04-20 01:39:24 +0530 (Wed, 20 Apr 2022) $
 * $Revision: 48 $
 */

/**************************************************************************************************************
 * $Revision: 48 $
 * Description: This action is used to add values to Sampling Plan detail tables with reference to MDLIMS -914.
 *
 * @author DASSA1
 * @version 1
 *
 **************************************************************************************************************/

public class BulkUpdateSamplingPlan extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 48 $";

    @Override
    public void processAction(PropertyList properties) throws SapphireException {

        logger.info("---------Processing BulkUpdateSamplingPlan Action---------");

        //Getting all the selected Sampling Plan details.
        String samplingPlanId = properties.getProperty("samplingplanid", "");
        String samplingPlanVersion = properties.getProperty("samplingplanversion", "");
        String newNotes = properties.getProperty("notes", "");
        String auditReason = properties.getProperty("auditreason", "");

        //Getting all the Process Stage details.
        String processStage = properties.getProperty("processstagestage", "");
        String stageCount = properties.getProperty("processstagecount", "");
        String stageTemplate = properties.getProperty("processstagetemplate", "");
        String stageType = properties.getProperty("processstagestagetype", "");

        //Getting all the Level Samples details.
        String level = properties.getProperty("levelsampleslevel", "");
        String sourceLabel = properties.getProperty("levelsamplessourcelabel", "");
        String samplesTemplate = properties.getProperty("levelsamplestemplate", "");
        String samplesCount = properties.getProperty("levelsamplescount", "");
        String markForCOA = properties.getProperty("markforcoa", "");
        String department = properties.getProperty("levelsamplesdepartment", "");

        //Getting Test Method and Spec details.
        String testMethod = properties.getProperty("testmethod", "");
        String[] arrWorkItemDetails = testMethod.split(";");
        String specification = properties.getProperty("specification", "");
        String[] arrSpecDetails = specification.split(";");

        //First validation :: Checking if (oldNotes + CarriageReturn + newNotes) > 2000 characters.
        if (!"".equalsIgnoreCase(newNotes)) {
            checkValidation(samplingPlanId, samplingPlanVersion, newNotes, "notes");
        }

        //Second validation :: Checking if the selected process stage already exits in the selected sampling plans.
        checkValidation(samplingPlanId, samplingPlanVersion, processStage, "processstage");

        //Looping through each sampling plan and adding details.
        String[] samplingPlanIdArr = samplingPlanId.split(";");
        String[] samplingPlanVersionArr = samplingPlanVersion.split(";");

        for (int i = 0; i < samplingPlanIdArr.length; i++) {

            String eachsamplingPlanId = samplingPlanIdArr[i];
            String eachsamplingPlanVersion = samplingPlanVersionArr[i];

            //This method is used to add input notes to each sampling plan.
            if (!"".equalsIgnoreCase(newNotes)) {
                addToNotes(eachsamplingPlanId, eachsamplingPlanVersion, newNotes, auditReason);
            }

            //This method is used to add process stage details to each sampling plan.
            int processStageId = addProcessStageDetails(eachsamplingPlanId, eachsamplingPlanVersion, processStage, stageCount, stageTemplate, stageType, auditReason);

            //This method is used to add level sample details to each sampling plan.
            int detailNumber = addlevelSamplesDetails(eachsamplingPlanId, eachsamplingPlanVersion, processStageId, level, sourceLabel, samplesTemplate, samplesCount, markForCOA, department, auditReason);

            //This method is used to add test methods details to each sampling plan.
            if (!"".equalsIgnoreCase(testMethod)) {
                addTestMethodDetails(eachsamplingPlanId, eachsamplingPlanVersion, arrWorkItemDetails, detailNumber, auditReason);
            }

            //This method is used to add specification details to each sampling plan.
            if (!"".equalsIgnoreCase(specification)) {
                addSpecDetails(eachsamplingPlanId, eachsamplingPlanVersion, arrSpecDetails, detailNumber, auditReason);
            }
        }
    }

    /**
     * @param samplingPlanId
     * @param samplingPlanVersion
     * @param processStage
     * @param count
     * @param template
     * @param type
     * @param reason
     * @return
     * @throws SapphireException
     */

    private int addProcessStageDetails(String samplingPlanId, String samplingPlanVersion, String processStage, String count, String template, String type, String reason) throws SapphireException {

        int processStageId;
        //Getting max s_processstageid value from s_processstage table.
        String maxStageValue = getValue(samplingPlanId, samplingPlanVersion, "processstage");

        if ("".equalsIgnoreCase(maxStageValue)) {
            processStageId = 1;
        } else {
            processStageId = Integer.valueOf(maxStageValue) + 1;
        }

        int userSequence;
        //Getting max usersequence value from s_processstage table.
        String maxUserSequence = getValue(samplingPlanId, samplingPlanVersion, "processstagesequence");

        if ("".equalsIgnoreCase(maxUserSequence)) {
            userSequence = 1;
        } else {
            userSequence = Integer.valueOf(maxUserSequence) + 1;
        }

        PropertyList detailProps = new PropertyList();

        detailProps.setProperty("s_samplingplanid", samplingPlanId);
        detailProps.setProperty("s_samplingplanversionid", samplingPlanVersion);
        detailProps.setProperty("u_stagetype", (type != "" ? type : "(null)"));
        detailProps.setProperty("applylock", "N");
        detailProps.setProperty("label", processStage);
        detailProps.setProperty("processstagedesc", processStage);
        detailProps.setProperty("sdcid", "LV_SamplingPlan");
        detailProps.setProperty("repeatcount", count);
        detailProps.setProperty("propsmatch", "Y");
        detailProps.setProperty("templatesdcid", "LV_BatchStage");
        detailProps.setProperty("s_processstageid", Integer.toString(processStageId));
        detailProps.setProperty("linkid", "process stage");
        detailProps.setProperty("templatekeyid1", template);
        detailProps.setProperty("copies", "1");
        detailProps.setProperty("usersequence", Integer.toString(userSequence));
        detailProps.setProperty("auditreason", reason);
        //detailProps.setProperty("tracelogid","2242777");

        getActionProcessor().processAction(AddSDIDetail.ID, AddSDIDetail.VERSIONID, detailProps);
        return processStageId;
    }

    /**
     * @param samplingPlanId
     * @param samplingPlanVersion
     * @param processStageId
     * @param level
     * @param sourceLabel
     * @param template
     * @param count
     * @param markForCOA
     * @param department
     * @param reason
     * @return
     * @throws SapphireException
     */
    private int addlevelSamplesDetails(String samplingPlanId, String samplingPlanVersion, int processStageId, String level, String sourceLabel, String template, String count, String markForCOA, String department, String reason) throws SapphireException {

        int detailNumber;
        //Getting max s_samplingplandetailno from s_spdetail table.
        String maxDetailNo = getValue(samplingPlanId, samplingPlanVersion, "levelsamples");

        if ("".equalsIgnoreCase(maxDetailNo)) {
            detailNumber = 1;
        } else {
            detailNumber = Integer.valueOf(maxDetailNo) + 1;
        }

        int userSequence;
        //Getting max usersequence value from s_spdetail table.
        String maxUserSequence = getValue(samplingPlanId, samplingPlanVersion, "levelsamplessequence");

        if ("".equalsIgnoreCase(maxUserSequence)) {
            userSequence = 1;
        } else {
            userSequence = Integer.valueOf(maxUserSequence) + 1;
        }

        PropertyList detailProps = new PropertyList();

        detailProps.setProperty("sdcid", "LV_SamplingPlan");
        detailProps.setProperty("templatesdcid", "Sample");
        detailProps.setProperty("u_quantity", "(null)");
        detailProps.setProperty("templatekeyid3", "(null)");
        detailProps.setProperty("linkid", "detail");
        detailProps.setProperty("templatekeyid2", "(null)");
        detailProps.setProperty("templatekeyid1", template);
        detailProps.setProperty("defaultdepartmentid", department);
        detailProps.setProperty("processstageid", Integer.toString(processStageId));
        detailProps.setProperty("u_coaflag", markForCOA);
        detailProps.setProperty("s_samplingplanid", samplingPlanId);
        detailProps.setProperty("s_samplingplanversionid", samplingPlanVersion);
        detailProps.setProperty("applylock", "N");
        detailProps.setProperty("countruletype", "Number");
        detailProps.setProperty("propsmatch", "Y");
        detailProps.setProperty("levelid", level);
        detailProps.setProperty("countrule", count);
        detailProps.setProperty("copies", "1");
        detailProps.setProperty("u_storagecondition", "(null)");
        detailProps.setProperty("u_containertype", "(null)");
        detailProps.setProperty("sourcelabel", sourceLabel);
        detailProps.setProperty("u_quantityunit", "(null)");
        detailProps.setProperty("s_samplingplandetailno", Integer.toString(detailNumber));
        detailProps.setProperty("usersequence", Integer.toString(userSequence));
        detailProps.setProperty("auditreason", reason);
        //detailProps.setProperty("tracelogid","2242780");

        getActionProcessor().processAction(AddSDIDetail.ID, AddSDIDetail.VERSIONID, detailProps);
        return detailNumber;
    }


    /**
     * @param samplingPlanId
     * @param samplingPlanVersion
     * @param arrWorkItemDetails
     * @param detailNumber
     * @param reason
     * @throws SapphireException
     */
    private void addTestMethodDetails(String samplingPlanId, String samplingPlanVersion, String[] arrWorkItemDetails, int detailNumber, String reason) throws SapphireException {

        int itemNumber;
        //Looping through each Test Method and adding to each Sampling Plan.
        for (int i = 0; i < arrWorkItemDetails.length; i++) {

            String eachWorkitemId = arrWorkItemDetails[i].split("\\|")[0];
            String eachWorkitemVersion = arrWorkItemDetails[i].split("\\|")[1];

            //Getting max s_samplingplanitemno value from s_spitem table.
            String maxItemNo = getValue(samplingPlanId, samplingPlanVersion, "testsandspecs");

            if ("".equalsIgnoreCase(maxItemNo)) {
                itemNumber = 1;
            } else {
                itemNumber = Integer.valueOf(maxItemNo) + 1;
            }

            PropertyList detailProps = new PropertyList();

            detailProps.setProperty("s_samplingplanid", samplingPlanId);
            detailProps.setProperty("s_samplingplanversionid", samplingPlanVersion);
            detailProps.setProperty("applylock", "N");
            detailProps.setProperty("linkid", "item");
            detailProps.setProperty("s_samplingplanitemno", Integer.toString(itemNumber));
            detailProps.setProperty("sdcid", "LV_SamplingPlan");
            detailProps.setProperty("itemsdcid", "WorkItem");
            detailProps.setProperty("itemkeyid2", eachWorkitemVersion);
            detailProps.setProperty("separator", ";");
            detailProps.setProperty("itemkeyid1", eachWorkitemId);
            detailProps.setProperty("auditreason", reason);
            //detailProps.setProperty("usersequence", Integer.toString(userSequence));
            //detailProps.setProperty("tracelogid","2242785");

            getActionProcessor().processAction(AddSDIDetail.ID, AddSDIDetail.VERSIONID, detailProps);

            //Marking the checkbox in s_spdetailitem table.
            detailProps.clear();
            detailProps.setProperty("s_samplingplanid", samplingPlanId);
            detailProps.setProperty("detaillinkid", "detail");
            detailProps.setProperty("s_samplingplanversionid", samplingPlanVersion);
            detailProps.setProperty("applylock", "N");
            detailProps.setProperty("s_samplingplandetailno", Integer.toString(detailNumber));
            detailProps.setProperty("linkid", "item");
            detailProps.setProperty("s_samplingplanitemno", Integer.toString(itemNumber));
            detailProps.setProperty("sdcid", "LV_SamplingPlan");
            detailProps.setProperty("separator", ";");
            detailProps.setProperty("auditreason", reason);
            //detailProps.setProperty("tracelogid", "2243374");

            getActionProcessor().processAction(AddSDIDetail.ID, AddSDIDetail.VERSIONID, detailProps);
        }
    }

    /**
     * @param samplingPlanId
     * @param samplingPlanVersion
     * @param arrSpecDetails
     * @param detailNumber
     * @param reason
     * @throws SapphireException
     */
    private void addSpecDetails(String samplingPlanId, String samplingPlanVersion, String[] arrSpecDetails, int detailNumber, String reason) throws SapphireException {

        int itemNumber;
        //Looping through each Specification and adding to each Sampling Plan.
        for (int i = 0; i < arrSpecDetails.length; i++) {

            String eachSpecId = arrSpecDetails[i].split("\\|")[0];
            String eachSpecVersion = arrSpecDetails[i].split("\\|")[1];

            //Getting max s_samplingplanitemno value from s_spitem table.
            String maxItemNo = getValue(samplingPlanId, samplingPlanVersion, "testsandspecs");

            if ("".equalsIgnoreCase(maxItemNo)) {
                itemNumber = 1;
            } else {
                itemNumber = Integer.valueOf(maxItemNo) + 1;
            }

            PropertyList detailProps = new PropertyList();

            detailProps.setProperty("s_samplingplanid", samplingPlanId);
            detailProps.setProperty("s_samplingplanversionid", samplingPlanVersion);
            detailProps.setProperty("applylock", "N");
            detailProps.setProperty("linkid", "item");
            detailProps.setProperty("s_samplingplanitemno", Integer.toString(itemNumber));
            detailProps.setProperty("sdcid", "LV_SamplingPlan");
            detailProps.setProperty("itemsdcid", "SpecSDC");
            detailProps.setProperty("itemkeyid2", eachSpecVersion);
            detailProps.setProperty("separator", ";");
            detailProps.setProperty("itemkeyid1", eachSpecId);
            detailProps.setProperty("auditreason", reason);
            //detailProps.setProperty("usersequence", Integer.toString(userSequence));
            //detailProps.setProperty("tracelogid","2242785");

            getActionProcessor().processAction(AddSDIDetail.ID, AddSDIDetail.VERSIONID, detailProps);

            //Marking the checkbox in s_spdetailitem table.
            detailProps.clear();
            detailProps.setProperty("s_samplingplanid", samplingPlanId);
            detailProps.setProperty("detaillinkid", "detail");
            detailProps.setProperty("s_samplingplanversionid", samplingPlanVersion);
            detailProps.setProperty("applylock", "N");
            detailProps.setProperty("s_samplingplandetailno", Integer.toString(detailNumber));
            detailProps.setProperty("linkid", "item");
            detailProps.setProperty("s_samplingplanitemno", Integer.toString(itemNumber));
            detailProps.setProperty("sdcid", "LV_SamplingPlan");
            detailProps.setProperty("separator", ";");
            detailProps.setProperty("auditreason", reason);
            //detailProps.setProperty("tracelogid", "2243374");

            getActionProcessor().processAction(AddSDIDetail.ID, AddSDIDetail.VERSIONID, detailProps);
        }
    }

    /**
     * @param samplingPlanId
     * @param samplingPlanVersion
     * @param value
     * @param flag
     * @throws SapphireException
     */
    private void checkValidation(String samplingPlanId, String samplingPlanVersion, String value, String flag) throws SapphireException {

        //Building the sql string.
        String selectClause = "";
        String fromClause = "";
        String andClause1 = "";
        String andClause2 = "";
        String andClause3 = "";

        if ("notes".equalsIgnoreCase(flag)) {
            selectClause = " select s_samplingplan.s_samplingplanid, s_samplingplan.s_samplingplanversionid";
            fromClause = " from s_samplingplan, rsetitems";
            andClause1 = " and length(nvl(s_samplingplan.notes,'0')) + length('" + value + "') + length('\\r\\n') > 2000";
            andClause2 = " and s_samplingplan.s_samplingplanid = rsetitems.keyid1";
            andClause3 = " and s_samplingplan.s_samplingplanversionid = rsetitems.keyid2";

        } else if ("processstage".equalsIgnoreCase(flag)) {
            selectClause = " select s_processstage.s_samplingplanid, s_processstage.s_samplingplanversionid";
            fromClause = " from s_processstage, rsetitems";
            andClause1 = " and s_processstage.label = '" + value + "'";
            andClause2 = " and s_processstage.s_samplingplanid = rsetitems.keyid1";
            andClause3 = " and s_processstage.s_samplingplanversionid = rsetitems.keyid2";
        }

        DataSet dsResult = new DataSet(connectionInfo);
        String rsetId = "";
        try {
            rsetId = getDAMProcessor().createRSet("LV_SamplingPlan", samplingPlanId, samplingPlanVersion, null);

            String sqlText = selectClause + fromClause +
                    " where rsetitems.sdcid ='LV_SamplingPlan'" +
                    andClause1 + andClause2 + andClause3 +
                    " and rsetitems.rsetid='" + rsetId + "'";

            dsResult = getQueryProcessor().getSqlDataSet(sqlText);
        } catch (SapphireException e) {
            throw new SapphireException(e);
        } finally {
            if (null != rsetId || !"".equals(rsetId)) {
                getDAMProcessor().clearRSet(rsetId);
            }
        }
        if (dsResult.size() > 0) {
            //Build the error message in format ::: sampling plan id(version id).
            String errMsgBuilder = "";
            for (int row = 0; row < dsResult.size(); row++) {
                //For removing "," at last.
                if (row == dsResult.size() - 1) {
                    errMsgBuilder += dsResult.getValue(row, "s_samplingplanid") + "(" + dsResult.getValue(row, "s_samplingplanversionid") + ")";
                } else {
                    errMsgBuilder += dsResult.getValue(row, "s_samplingplanid") + "(" + dsResult.getValue(row, "s_samplingplanversionid") + ")" + ", ";
                }
            }

            if ("notes".equalsIgnoreCase(flag)) {
                throw new SapphireException("General Error", ErrorDetail.TYPE_VALIDATION, "Notes of the following Sampling Plans exceed capacity of 2000 characters - " + errMsgBuilder);

            } else if ("processstage".equalsIgnoreCase(flag)) {
                throw new SapphireException("General Error", ErrorDetail.TYPE_VALIDATION, "Process Stage already exists for the following Sampling Plans - " + errMsgBuilder);
            }
        }
        return;
    }

    /**
     * @param samplingPlanId
     * @param samplingPlanVersion
     * @param newNotes
     * @param reason
     * @throws SapphireException
     */
    private void addToNotes(String samplingPlanId, String samplingPlanVersion, String newNotes, String reason) throws SapphireException {

        String finalNotes = "";
        String sqltext = "select notes from s_samplingplan" +
                " where s_samplingplanid = '" + samplingPlanId + "'" +
                " and s_samplingplanversionid = '" + samplingPlanVersion + "'";
        DataSet dsOutput = getQueryProcessor().getSqlDataSet(sqltext);
        String oldNotes = dsOutput.getColumnValues("notes", "");

        if ("".equalsIgnoreCase(oldNotes)) {
            finalNotes = newNotes;
        } else {
            finalNotes = oldNotes + "\r\n" + newNotes;
        }

        PropertyList editProps = new PropertyList();

        editProps.setProperty(EditSDI.PROPERTY_KEYID1, samplingPlanId);
        editProps.setProperty(EditSDI.PROPERTY_KEYID2, samplingPlanVersion);
        editProps.setProperty(EditSDI.PROPERTY_SDCID, "LV_SamplingPlan");
        editProps.setProperty("notes", finalNotes);
        editProps.setProperty("auditreason", reason);

        getActionProcessor().processAction(EditSDI.ID, EditSDI.VERSIONID, editProps);
    }

    /**
     * @param samplingPlanId
     * @param samplingPlanVersion
     * @param flag
     * @return
     * @throws SapphireException
     */
    private String getValue(String samplingPlanId, String samplingPlanVersion, String flag) throws SapphireException {

        String value = "";

        if ("processstage".equalsIgnoreCase(flag)) {
            //Getting max s_processstageid value from s_processstage table.
            String sql = "select max(cast(s_processstageid as int)) as maxvalue from s_processstage" +
                    " where s_samplingplanid = '" + samplingPlanId + "'" +
                    " and s_samplingplanversionid = '" + samplingPlanVersion + "'";
            DataSet dsOutput = getQueryProcessor().getSqlDataSet(sql);
            value = dsOutput.getColumnValues("maxvalue", "");

        } else if ("levelsamples".equalsIgnoreCase(flag)) {
            //Getting max s_samplingplandetailno from s_spdetail table.
            String sqlText = "select max(s_samplingplandetailno) as maxvalue from s_spdetail" +
                    " where s_samplingplanid = '" + samplingPlanId + "'" +
                    " and s_samplingplanversionid = '" + samplingPlanVersion + "'";
            DataSet dsResult = getQueryProcessor().getSqlDataSet(sqlText);
            value = dsResult.getColumnValues("maxvalue", "");

        } else if ("testsandspecs".equalsIgnoreCase(flag)) {
            //Getting max s_samplingplanitemno value from s_spitem table.
            String sql = "select max(s_samplingplanitemno) as maxvalue from s_spitem" +
                    " where s_samplingplanid = '" + samplingPlanId + "'" +
                    " and s_samplingplanversionid = '" + samplingPlanVersion + "'";
            DataSet dsOutput = getQueryProcessor().getSqlDataSet(sql);
            value = dsOutput.getColumnValues("maxvalue", "");

        } else if ("processstagesequence".equalsIgnoreCase(flag)) {
            //Getting max usersequence value from s_processstage table.
            String sql = "select max(usersequence) as maxvalue from s_processstage" +
                    " where s_samplingplanid = '" + samplingPlanId + "'" +
                    " and s_samplingplanversionid = '" + samplingPlanVersion + "'";
            DataSet dsOutput = getQueryProcessor().getSqlDataSet(sql);
            value = dsOutput.getColumnValues("maxvalue", "");

        } else if ("levelsamplessequence".equalsIgnoreCase(flag)) {
            //Getting max usersequence from s_spdetail table.
            String sqlText = "select max(usersequence) as maxvalue from s_spdetail" +
                    " where s_samplingplanid = '" + samplingPlanId + "'" +
                    " and s_samplingplanversionid = '" + samplingPlanVersion + "'";
            DataSet dsResult = getQueryProcessor().getSqlDataSet(sqlText);
            value = dsResult.getColumnValues("maxvalue", "");

        } else if ("testsandspecssequence".equalsIgnoreCase(flag)) {
            //Getting max usersequence value from s_spitem table.
            String sql = "select max(usersequence) as maxvalue from s_spitem" +
                    " where s_samplingplanid = '" + samplingPlanId + "'" +
                    " and s_samplingplanversionid = '" + samplingPlanVersion + "'";
            DataSet dsOutput = getQueryProcessor().getSqlDataSet(sql);
            value = dsOutput.getColumnValues("maxvalue", "");
        }

        return value;
    }
}
