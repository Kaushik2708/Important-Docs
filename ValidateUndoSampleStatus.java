package labvantage.custom.alcon.actions;

import sapphire.SapphireException;
import sapphire.action.BaseAction;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.util.StringUtil;
import sapphire.xml.PropertyList;

/**
 * $Author: BAGCHAN1 $
 * $Date: 2022-02-08 00:16:36 -0500 (Tue, 08 Feb 2022) $
 * $Revision: 13 $
 */

/*****************************************************************************************************
 * $Revision: 13 $
 * Description:
 * 1.	Check if the linked batch is in status Released or Rejected for the changed sample whose status is modified.
 * 2.	Throw error – message – Samples status cannot be modified for the selected item(s) as one or more of the linked batches are either in Released or Rejected status.
 * 3.	Note: Implement with hasprimaryvaluechanged  multiple sample can be updated

 *
 * @author Soumen Mitra
 * @version 1
 ******************************************************************************************************/


public class ValidateUndoSampleStatus extends BaseAction {

    private static final String DEVOPS_ID = "$Revision: 13 $";
    private static final String BATCH_STATUS_RELEASED = "Released";
    private static final String BATCH_STATUS_REJECTED = "Rejected";
    private static final String SAMPLE_STATUS_REVIEWED = "Reviewed";
    private static final String SAMPLE_STATUS_DISPOSED = "Disposed";

    public void processAction(PropertyList properties) throws SapphireException {

        String sampleId = properties.getProperty("s_sample_s_sampleid", ""); // semi colon seperated values
        String beforeStatus = properties.getProperty("beforestatus", "");// semi colon seperated values
        String afterStatus = properties.getProperty("s_sample_samplestatus", "");// semi colon seperated values

        if (beforeStatus.equalsIgnoreCase(SAMPLE_STATUS_REVIEWED)) {
            //Check all of the targetted status=Disposed
            boolean allDisposedFlag = true;
            String[] arrAfterStatus = StringUtil.split(afterStatus, ";");
            for (int i = 0; i < arrAfterStatus.length; i++) {
                if (!SAMPLE_STATUS_DISPOSED.equalsIgnoreCase(arrAfterStatus[i])) {
                    allDisposedFlag = false;
                    break;
                }
            }

            if (allDisposedFlag) {
                return; // Do nothing
            }

        }

        if (sampleId.equalsIgnoreCase("")) {
            return;
        }
        try {
            validateBatchStatus(sampleId);
        } catch (SapphireException ex) {
            throw new SapphireException(ex.getMessage());
        }
    }

    /**
     * Validation Batch status. If Released or Rejected - do not allow Sample status change
     *
     * @param sampleId
     * @throws SapphireException
     */
    private void validateBatchStatus(String sampleId) throws SapphireException {
        String strRSet = "";
        String strQuery = "";
        DataSet dsBatch = new DataSet();
        try {
            //unlockSample(sampleId);
            strRSet = getDAMProcessor().createRSet("Sample", sampleId, "(null)", "(null)");
            strQuery = "select s.s_sampleid, s.samplestatus, b.s_batchid, b.batchstatus " +
                    " from s_sample s, s_batch b, rsetitems r where r.sdcid='Sample' " +
                    " and r.keyid1=s.s_sampleid and r.keyid2 = '(null)' and r.keyid3='(null)' " +
                    " and s.batchid = b.s_batchid " +
                    " and r.rsetid= ? ";

            dsBatch = getQueryProcessor().getPreparedSqlDataSet(strQuery, new Object[]{strRSet});

            if (dsBatch == null || dsBatch.size() == 0) {
                return;
            }

            String qBatchStatus = "";

            for (int i = 0; i < dsBatch.size(); i++) {

                qBatchStatus = dsBatch.getValue(0, "batchstatus", "");

                if (qBatchStatus.equalsIgnoreCase(BATCH_STATUS_RELEASED) || qBatchStatus.equalsIgnoreCase(BATCH_STATUS_REJECTED)) {
                    String errMsg = "Sample(s) cannot be updated for the selected item(s) as one or more of the linked batches are either in Released or Rejected status.";
                    throw new SapphireException(errMsg);
                }
            }
        } catch (SapphireException ex) {
            throw new SapphireException(" General Error", ErrorDetail.TYPE_FAILURE, ex.getMessage());
        } finally {
            if (!"".equals(strRSet)) {
                getDAMProcessor().clearRSet(strRSet);
            }
        }

    }

    private void unlockSample(String sampleId) {
        String sql = "SELECT rsetid FROM rsetitems WHERE sdcid = 'Sample'  and keyid1 = ? ";
        DataSet dsRsetItems = getQueryProcessor().getPreparedSqlDataSet(sql, new Object[]{sampleId});
        if (dsRsetItems != null && dsRsetItems.size() > 0) {
            for (int i = 0; i < dsRsetItems.size(); i++) {
                getDAMProcessor().clearRSet(dsRsetItems.getValue(i, "rsetid", ""));
            }
        }

    }

}
