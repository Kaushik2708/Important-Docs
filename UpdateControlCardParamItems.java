package labvantage.custom.alcon.actions;


import sapphire.SapphireException;
import sapphire.action.BaseAction;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.xml.PropertyList;

/**
 * $Author: BAGCHAN1 $
 * $Date: 2022-08-10 09:57:18 +0530 (Wed, 10 Aug 2022) $
 * $Revision: 76 $
 */

public class UpdateControlCardParamItems extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 76 $";

    @Override
    public void processAction(PropertyList properties) throws SapphireException {

        logger.debug("---------Starting UpdateControlCardParamItems Service---------");

        //Getting all the required inputs
        String activityflag = properties.getProperty("activityflag", "");
        String controlcardid = properties.getProperty("controlcardid", "");
        String paramitemvalue = properties.getProperty("paramitemvalue", "");

        //Function to update the param items fields of the selected control cards
        updateParamFields(activityflag, controlcardid, paramitemvalue);
    }

    public void updateParamFields(String activityflag, String controlcardid, String paramitemvalue) throws SapphireException {

        //=========================================================================//
        //-- Update 10Aug2022:: Aniruddha Bagchi
        //-- The bulk update will be allowed only if Version Control=Y (Automatic) for Control Cards

        DataSet dsResult;
        String rsetId = "";
        try {
            rsetId = getDAMProcessor().createRSet("SPC_ControlCard", controlcardid, null, null);
            String sqlText = "select  spc_controlcardid, autoversionflag" +
                    " from spc_controlcard" +
                    " join rsetitems on  spc_controlcard.spc_controlcardid = rsetitems.keyid1" +
                    " where rsetitems.sdcid = 'SPC_ControlCard' and rsetitems.rsetid = ?";

            dsResult = getQueryProcessor().getPreparedSqlDataSet(sqlText, new Object[]{rsetId});

        } catch (SapphireException e) {
            throw new SapphireException("Unexpected error occured while executing the query. Error is " + e.getMessage());
        } finally {
            if (null != rsetId || !"".equals(rsetId)) {
                getDAMProcessor().clearRSet(rsetId);
            }
        }
        if (dsResult == null) {
            throw new SapphireException(ErrorDetail.TYPE_FAILURE, "Failed to fetch details for control card: " + controlcardid);
        }
        if (dsResult.size() == 0) {
            throw new SapphireException(ErrorDetail.TYPE_VALIDATION, "Failed to fetch details for control card: " + controlcardid);
        }

        for (int i = 0; i < dsResult.size(); i++) {
            if (!"Y".equalsIgnoreCase(dsResult.getValue(i, "autoversionflag", ""))) {
                throw new SapphireException("Validation", TYPE_VALIDATION
                        , "The process is allowed for Control Cards having Version Control marked as 'Automatic'."
                );
            }
        }

        //==========================================================================//

        //Getting all the param items of versionstatus = current, associated with the selected control cards
        dsResult.clear();
        try {
            rsetId = getDAMProcessor().createRSet("SPC_CCParamItem", controlcardid, null, null);
            String sqlText = "select distinct spc_ccparamitemid" +
                    " from spc_ccparamitem" +
                    " join rsetitems on  spc_ccparamitem.controlcardid = rsetitems.keyid1" +
                    " where rsetitems.sdcid = 'SPC_CCParamItem' and rsetitems.rsetid = ?" +
                    " and spc_ccparamitem.versionstatus = 'C'";

            dsResult = getQueryProcessor().getPreparedSqlDataSet(sqlText, new Object[]{rsetId});
        } catch (SapphireException e) {
            throw new SapphireException("Unexpected error occured while executing the query. Error is " + e.getMessage());
        } finally {
            if (null != rsetId || !"".equals(rsetId)) {
                getDAMProcessor().clearRSet(rsetId);
            }
        }
        if (dsResult == null) {
            throw new SapphireException(ErrorDetail.TYPE_FAILURE, "Failed to execute param details for control card: " + controlcardid);
        }
        if (dsResult.size() == 0) {
            throw new SapphireException(ErrorDetail.TYPE_VALIDATION, "No Current Param Items found for the selected control card(s)");
        }
        if (dsResult.size() > 0) {
            //Looping through each param item and updating the fields
            for (int row = 0; row < dsResult.size(); row++) {

                String paramitemid = dsResult.getValue(row, "spc_ccparamitemid", "");

                //Finding the max version number of each param item
                String sql = "select max(to_number(spc_ccparamitemversionid)) as maxvalue" +
                        " from spc_ccparamitem" +
                        " where spc_ccparamitemid = ?";
                DataSet dsOutput = getQueryProcessor().getPreparedSqlDataSet(sql, new Object[]{paramitemid});
                String paramitemversion = dsOutput.getValue(0, "maxvalue", "");

                PropertyList plValues = new PropertyList();

                //Processing Action EditSDI on the original param item id
                logger.debug("-------Processing Action EditSDI-------");

                plValues.setProperty("keyid2", paramitemversion);
                plValues.setProperty("applylock", "N");
                if (activityflag.equalsIgnoreCase("Update field - Any Parameter List")) {
                    if (paramitemvalue.equalsIgnoreCase("")) {
                        plValues.setProperty("anyparamlist", "(null)");
                    } else {
                        plValues.setProperty("anyparamlist", paramitemvalue);
                    }
                } else if (activityflag.equalsIgnoreCase("Update field - Any Parameter Type")) {
                    if (paramitemvalue.equalsIgnoreCase("")) {
                        plValues.setProperty("anyparamtype", "(null)");
                    } else {
                        plValues.setProperty("anyparamtype", paramitemvalue);
                    }
                }
                plValues.setProperty("sdcid", "SPC_CCParamItem");
                plValues.setProperty("keyid1", paramitemid);
                plValues.setProperty("propsmatch", "Y");

                getActionProcessor().processAction(com.labvantage.sapphire.actions.sdi.EditSDI.ID, com.labvantage.sapphire.actions.sdi.EditSDI.VERSIONID, plValues);
                plValues.clear();

                //Processing Action AddSDIVersion on the original param item id
                logger.debug("-------Processing Action AddSDIVersion-------");

                plValues.setProperty("keyid2", paramitemversion);
                plValues.setProperty("applylock", "N");
                plValues.setProperty("sdcid", "SPC_CCParamItem");
                plValues.setProperty("automatedversionedit", "Y");
                plValues.setProperty("keyid1", paramitemid);

                getActionProcessor().processAction(com.labvantage.sapphire.actions.sdi.AddSDIVersion.ID, com.labvantage.sapphire.actions.sdi.AddSDIVersion.VERSIONID, plValues);
                plValues.clear();

                //Processing Action EditSDI on the new version of param item id
                logger.debug("-------Processing Action EditSDI-------");

                plValues.setProperty("keyid2", Integer.toString(Integer.valueOf(paramitemversion) + 1));
                plValues.setProperty("applylock", "N");
                if (activityflag.equalsIgnoreCase("Update field - Any Parameter List")) {
                    if (paramitemvalue.equalsIgnoreCase("")) {
                        //Do nothing
                    } else {
                        plValues.setProperty("anyparamlist", paramitemvalue);
                    }
                } else if (activityflag.equalsIgnoreCase("Update field - Any Parameter Type")) {
                    if (paramitemvalue.equalsIgnoreCase("")) {
                        //Do nothing
                    } else {
                        plValues.setProperty("anyparamtype", paramitemvalue);
                    }
                }
                plValues.setProperty("sdcid", "SPC_CCParamItem");
                plValues.setProperty("keyid1", paramitemid);
                plValues.setProperty("automatedversionedit", "Y");

                getActionProcessor().processAction(com.labvantage.sapphire.actions.sdi.EditSDI.ID, com.labvantage.sapphire.actions.sdi.EditSDI.VERSIONID, plValues);
                plValues.clear();

                //Processing Action SetSDIVersionStatus on the new version of param item id
                logger.debug("-------Processing Action SetSDIVersionStatus-------");

                plValues.setProperty("keyid2", Integer.toString(Integer.valueOf(paramitemversion) + 1));
                plValues.setProperty("applylock", "N");
                plValues.setProperty("sdcid", "SPC_CCParamItem");
                plValues.setProperty("versionstatus", "C");
                plValues.setProperty("keyid1", paramitemid);

                getActionProcessor().processAction(com.labvantage.sapphire.actions.sdi.SetSDIVersionStatus.ID, com.labvantage.sapphire.actions.sdi.SetSDIVersionStatus.VERSIONID, plValues);
                plValues.clear();

                //Processing Action EditSDI on the original param item
                logger.debug("-------Processing Action EditSDI-------");

                plValues.setProperty("keyid2", paramitemversion);
                plValues.setProperty("applylock", "N");
                plValues.setProperty("enddt", "n");
                plValues.setProperty("sdcid", "SPC_CCParamItem");
                plValues.setProperty("automatedversionedit", "Y");
                plValues.setProperty("keyid1", paramitemid);

                getActionProcessor().processAction(com.labvantage.sapphire.actions.sdi.EditSDI.ID, com.labvantage.sapphire.actions.sdi.EditSDI.VERSIONID, plValues);
                plValues.clear();
            }
        }

    }
}
