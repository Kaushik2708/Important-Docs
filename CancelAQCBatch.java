package labvantage.custom.alcon.actions;

import sapphire.SapphireException;
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

/***************************************************************************
 * $Revision: 48 $
 * Description: Cancel AQC Batch and all samples other than "Unknown" samples. Samples with status Cancelled/Completed/Reviewed/Disposed are not marked for cancellation.
 *
 * @author DASSA1
 * @version 1
 *
 *******************************************************************************/
public class CancelAQCBatch extends BaseAction {

    public static final String DEVOPS_ID = "$Revision: 48 $";


    @Override
    public void processAction(PropertyList properties) throws SapphireException {

        logger.info("---------Starting CancelAQCBatch Service---------");
        String error = "";
        //Getting the selected aqc batch ids.
        String qcbatchId = properties.getProperty("keyid1", "");

        if (qcbatchId == null) {
            error = "No Batch Selected.";
            throw new SapphireException(ErrorDetail.TYPE_VALIDATION, error);
        }

        //Getting the batch details along with the associated samples.
        DataSet dsResult = new DataSet();
        String rsetId = "";
        try {
            rsetId = getDAMProcessor().createRSet("QCBatch", qcbatchId, null, null);

            String sqlText = " select q.s_qcbatchid, q.qcbatchstatus, sd.keyid1, qb.batchitemtype, sd.sdidataid, s.samplestatus\n" +
                    " from s_qcbatch q" +
                    " join rsetitems r on q.s_qcbatchid=r.keyid1" +
                    " left join s_qcbatchitem qb on q.s_qcbatchid = qb.s_qcbatchid" +
                    " left join sdidata sd on sd.s_qcbatchid = qb.s_qcbatchid and sd.s_qcbatchitemid = qb.s_qcbatchitemid" +
                    " left join s_sample s  on s.s_sampleid = sd.keyid1 and q.s_qcbatchid = qb.s_qcbatchid" +
                    " where r.sdcid='QCBatch' and r.rsetid='" + rsetId + "'";

            dsResult = getQueryProcessor().getSqlDataSet(sqlText);
        } catch (SapphireException e) {

        } finally {
            if (null != rsetId || !"".equals(rsetId)) {
                getDAMProcessor().clearRSet(rsetId);
            }
        }


        if (dsResult == null) {
            error = "Failed to execute sample details for QC Batch: " + qcbatchId;
            throw new SapphireException(ErrorDetail.TYPE_VALIDATION, error);
        }

        if (dsResult.size() == 0) {
            error = "No Batch found ";
            throw new SapphireException(ErrorDetail.TYPE_VALIDATION, error);
        }


        logger.info("---------Checking Unknown Samples---------");
        //Filtering the dataset for sample type = unknown.
        PropertyList plFilter = new PropertyList();
        plFilter.setProperty("batchitemtype", "Unknown");
        DataSet dsUnknowSamples = dsResult.getFilteredDataSet(plFilter);


        //Checking for unknown samples.
        if (dsUnknowSamples.size() > 0) {

            //Throw error msg only if an unknown sample have a valid sample id .
            for (int row = 0; row < dsUnknowSamples.size(); row++) {
                if (dsUnknowSamples.getValue(row, "keyid1").equalsIgnoreCase("") ||
                        dsUnknowSamples.getValue(row, "keyid1").equals(null)) {
                    //Don't throw error msg if Sample id is null for unknown sample, so do nothing.
                } else {
                    //throw error msg for unknown samples.
                    error = "One or more of the selected item(s) has linked Unknown Sample(s).Please remove them to proceed";
                    throw new SapphireException(ErrorDetail.TYPE_VALIDATION, error);
                }
            }
        }

        //flagging the samples from the DataSet with sample status not in Cancelled/Completed/Reviewed.
        if (!dsResult.isValidColumn("selectforcancel")) {
            dsResult.addColumn("selectforcancel", DataSet.STRING);
        }

        //selecting the samples which are eligible to get cancelled.
        for (int row = 0; row < dsResult.size(); row++) {
            if (dsResult.getValue(row, "samplestatus").equalsIgnoreCase("Cancelled") ||
                    dsResult.getValue(row, "samplestatus").equalsIgnoreCase("Completed") ||
                    dsResult.getValue(row, "samplestatus").equalsIgnoreCase("Reviewed") ||
                    dsResult.getValue(row, "samplestatus").equalsIgnoreCase("Disposed") ||
                    dsResult.getValue(row, "samplestatus").equalsIgnoreCase("") ||
                    dsResult.getValue(row, "samplestatus").equals(null)) {
                //nominated for not cancel
                dsResult.setValue(row, "selectforcancel", "N");
            } else {
                //nominated for cancel
                dsResult.setValue(row, "selectforcancel", "Y");
            }
        }
        //Filtering the dataset for samples where selectforcancel flag = Y.
        plFilter.clear();
        plFilter.setProperty("selectforcancel", "Y");
        DataSet dsFilterDeletedItems = dsResult.getFilteredDataSet(plFilter);

        if (dsFilterDeletedItems == null) {
            error = "Failed to execute sample details for selectforcancel column value = : Y";
            throw new SapphireException(ErrorDetail.TYPE_VALIDATION, error);
        }

        //if no samples are eligible to be cancelled then just cancel the batch.
        if (dsFilterDeletedItems.size() == 0) {
            String nominatedBatch = dsResult.getColumnValues("s_qcbatchid", ";");

            //Cancelling the Batch.
            logger.info("---------Cancelling the Batch---------");
            cancelLIMSEntity("QCBatch", nominatedBatch, "qcbatchstatus");
        } else {
            //Getting all the batches and samples eligible to be cancelled.
            String nominatedSamples = dsFilterDeletedItems.getColumnValues("keyid1", ";");
            String nominatedBatch = dsResult.getColumnValues("s_qcbatchid", ";");

            //Cancelling the Samples.
            logger.info("---------Cancelling the Samples---------");
            cancelLIMSEntity("Sample", nominatedSamples, "samplestatus");

            //Cancelling the Batch.
            logger.info("---------Cancelling the Batch---------");
            cancelLIMSEntity("QCBatch", nominatedBatch, "qcbatchstatus");
        }
    }

    //Method created for executing EditSDI functionality.
    private void cancelLIMSEntity(String sdcName, String nomonatedEntity, String statusColName) throws SapphireException {
        String error = "";
        PropertyList plEntity = new PropertyList();
        plEntity.setProperty(EditSDI.PROPERTY_SDCID, sdcName);
        plEntity.setProperty(EditSDI.PROPERTY_KEYID1, nomonatedEntity);
        plEntity.setProperty(statusColName, "Cancelled");
        try {
            getActionProcessor().processAction(EditSDI.ID, EditSDI.VERSIONID, plEntity);
        } catch (SapphireException ex) {
            error = "Failed to execute EditSDI for SDC" + nomonatedEntity;
            throw new SapphireException(ErrorDetail.TYPE_VALIDATION, error);
        }
    }

}
