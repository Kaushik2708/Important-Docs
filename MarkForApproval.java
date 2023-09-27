package labvantage.custom.alcon.actions;

import sapphire.SapphireException;
import sapphire.action.BaseAction;
import sapphire.action.EditSDI;
import sapphire.error.ErrorDetail;
import sapphire.xml.PropertyList;

/**
 * $Author: BAGCHAN1 $
 * $Date: 2022-04-20 01:39:24 +0530 (Wed, 20 Apr 2022) $
 * $Revision: 48 $
 */

public class MarkForApproval extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 48 $";
    private static final String PROP_WORKORDERSDC = "WorkOrderSDC";
    private static final String PROP_WORKORDERID = "workorderid";
    private static final String PROP_MARKED_FOR_APPROVAL = "u_markedforapprovalflag";
    public static String ID = "MarkForApproval";
    public static String VERSIONID = "1";

    @Override
    public void processAction(PropertyList props) throws SapphireException {
        logger.info("----- Starting of processing : " + ID + " , Version: " + VERSIONID + " ------");
        String strWorkOrdeId = props.getProperty(PROP_WORKORDERID, "");
        if ("".equalsIgnoreCase(strWorkOrdeId)) {
            logger.error(" Blank value found for workorder id.");
            throw new SapphireException(" General Error::", ErrorDetail.TYPE_VALIDATION, getTranslationProcessor().translate("Please contact system administrator. Blank value found for workorder id."));
        }
        setMarkForApprovalFlag(strWorkOrdeId);
        logger.info("----- End of processing : " + ID + " , Version: " + VERSIONID + " ------");
    }

    private void setMarkForApprovalFlag(String strWorkOrdeId) throws SapphireException {
        logger.info("----- Inside Service: " + ID + " , Inside: setMarkForApprovalFlag ------");
        PropertyList plMarkForApproval = new PropertyList();
        plMarkForApproval.setProperty(EditSDI.PROPERTY_SDCID, PROP_WORKORDERSDC);
        plMarkForApproval.setProperty(EditSDI.PROPERTY_KEYID1, strWorkOrdeId);
        plMarkForApproval.setProperty(PROP_MARKED_FOR_APPROVAL, "Y");
        try {
            getActionProcessor().processAction(EditSDI.ID, EditSDI.VERSIONID, plMarkForApproval);
        } catch (SapphireException ex) {
            logger.error(" Exception occured. Error is" + ex.getStackTrace());
            throw new SapphireException(" General Error::", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" PLease contact your system administrator. Error is: " + ex.getMessage()));
        }

    }
}
