package labvantage.custom.alcon.actions;

import labvantage.custom.alcon.sap.util.ErrorMessageUtil;
import sapphire.SapphireException;
import sapphire.action.ApproveSDIStep;
import sapphire.action.BaseAction;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.util.StringUtil;
import sapphire.xml.PropertyList;

/**
 * $Author: BAGCHAN1 $
 * $Date: 2022-04-20 01:39:24 +0530 (Wed, 20 Apr 2022) $
 * $Revision: 48 $
 */

/********************************************************************************************
 * $Revision: 48 $
 * Description:
 * This class is being called to approve all the monitor group in bulk order from the list page.
 *
 *
 *
 *******************************************************************************************/

public class ApproveMultiMonitorGroups extends BaseAction {

    public static final String DEVOPS_ID = "$Revision: 48 $";

    private static final String __PROPS_STATUS_PENDING = "U";
    private static final String __PROPS_SEPERATOR = ";";

    private static final String __PROPS_KEYID1 = "keyid1";
    private static final String __PROPS_USERSEQUENCE = "usersequence";
    private static final String __PROPS_SDCID = "LV_MonitorGroup";

    @Override
    public void processAction(PropertyList properties) throws SapphireException {
        String approvalFlag = properties.getProperty("disposition").equalsIgnoreCase("Accept") ? "P" : "F";       //P - Pass, F - Fail
        String comment = properties.getProperty("comment");
        String auditReason = properties.getProperty("auditreason");
        String monitorGroupId = properties.getProperty("monitorgroupid");
        DataSet dsMonitorGroups = fetchApprovalDetailsForMonitorGroup(monitorGroupId);
        if (dsMonitorGroups.getRowCount() == 0) {
            throw new SapphireException(ErrorMessageUtil.GENERAL_ERROR, ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" No SDI Addoval Details found "));
        } else {
            String sortColumn = __PROPS_KEYID1 + "," + __PROPS_USERSEQUENCE;
            dsMonitorGroups.sort(sortColumn);

            for (int i = 0; i < dsMonitorGroups.getRowCount(); i++) {
                processApproveSDIStep(dsMonitorGroups, approvalFlag, comment, auditReason, i);
            }

        }
    }

    private DataSet fetchApprovalDetailsForMonitorGroup(String monitorGroupId) {
        String sqlText = "SELECT approvaltypestep.approvalstep, approvaltypestep.approvaltypeid, approvaltypestep.roleid, sdiapproval.keyid1, approvaltypestep.usersequence " +
                "FROM approvaltypestep, sdiapproval " +
                "where sdiapproval.approvaltypeid = approvaltypestep.approvaltypeid " +
                "and sdiapproval.sdcid = 'LV_MonitorGroup' " +
                "and sdiapproval.approvalflag in ('" + __PROPS_STATUS_PENDING + "') " +
                "and sdiapproval.keyid1 in ('" + StringUtil.replaceAll(monitorGroupId, __PROPS_SEPERATOR, "','") + "')";
        DataSet dsTemp = getQueryProcessor().getSqlDataSet(sqlText);
        return dsTemp;
    }

    private void processApproveSDIStep(DataSet dsMonitorGroups, String approvalFlag, String comment, String auditReason, int rowCount) throws SapphireException {
        PropertyList plMonitorGroupStatusChanged = new PropertyList();
        plMonitorGroupStatusChanged.setProperty(ApproveSDIStep.PROPERTY_SDCID, __PROPS_SDCID);
        plMonitorGroupStatusChanged.setProperty(ApproveSDIStep.PROPERTY_KEYID1, dsMonitorGroups.getValue(rowCount, __PROPS_KEYID1, ""));
        plMonitorGroupStatusChanged.setProperty(ApproveSDIStep.PROPERTY_KEYID2, "(null)");
        plMonitorGroupStatusChanged.setProperty(ApproveSDIStep.PROPERTY_KEYID3, "(null)");

        plMonitorGroupStatusChanged.setProperty(ApproveSDIStep.PROPERTY_APPROVALSTEP, dsMonitorGroups.getValue(rowCount, "approvalstep", ""));
        plMonitorGroupStatusChanged.setProperty(ApproveSDIStep.PROPERTY_APPROVALFLAG, approvalFlag);
        plMonitorGroupStatusChanged.setProperty(ApproveSDIStep.PROPERTY_APPROVALTYPEID, dsMonitorGroups.getValue(rowCount, "approvaltypeid", ""));
        plMonitorGroupStatusChanged.setProperty(ApproveSDIStep.PROPERTY_APPROVALSTEPINSTANCE, "1");
        plMonitorGroupStatusChanged.setProperty(ApproveSDIStep.PROPERTY_APPROVALNOTE, comment);

        plMonitorGroupStatusChanged.setProperty(ApproveSDIStep.PROPERTY_AUDITSIGNEDFLAG, "Y");
        plMonitorGroupStatusChanged.setProperty(ApproveSDIStep.PROPERTY_AUDITREASON, auditReason);
        plMonitorGroupStatusChanged.setProperty(ApproveSDIStep.PROPERTY_AUDITACTIVITY, "SDI Approval");

        try {
            getActionProcessor().processAction(ApproveSDIStep.ID, ApproveSDIStep.VERSIONID, plMonitorGroupStatusChanged);
        } catch (SapphireException e) {
            throw new SapphireException(e.getMessage());
        }
    }
}
