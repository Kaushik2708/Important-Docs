package labvantage.custom.alcon.actions;

import sapphire.SapphireException;
import sapphire.action.BaseAction;
import sapphire.util.StringUtil;
import sapphire.xml.PropertyList;
/**
 * $Author: BAGCHAN1 $
 * $Date: 2022-02-08 00:16:36 -0500 (Tue, 08 Feb 2022) $
 * $Revision: 13 $
 */

/*******************************************************************
 *  $Revision: 13 $
 * The action will delete all sdiapproval and sdiapprovalstep records linked to master data objects passed from Task input property irrespective of master data versionstatus.
 * This action is called from ResetMDApproval task.
 * Jira ID: MDLIMS-21
 ************************************************************************/
public class ResetMasterDataApproval extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 13 $";

    public void processAction(PropertyList properties) throws SapphireException {
        String strSDCName = properties.getProperty("sdcname");
        if (null == strSDCName) {
            throw new SapphireException(" No SDC List passed!!! ");
        }
        if ("".equalsIgnoreCase(strSDCName)) {
            throw new SapphireException((" SDC List is blank!!! "));
        }

        String[] arrSDCName = StringUtil.split(strSDCName, ";");
        for (int i = 0; i < arrSDCName.length; i++) {
            deleteApproval(arrSDCName[i]);
        }
    }

    public void deleteApproval(String sdcid) {

        PropertyList sdcProps = this.getSDCProcessor().getProperties(sdcid);
        String table_id = sdcProps.getProperty("tableid");
        String table_keyid1 = sdcProps.getProperty("keycolid1");
        String table_keyid2 = sdcProps.getProperty("keycolid2");
        String table_keyid3 = sdcProps.getProperty("keycolid3");
        String approvalTypeId = "MasterData;MasterDataApproval";

        //Delete SDIApprovalStep, SDIApproval
        String sqlDelApprovalStep = "delete  from sdiapprovalstep ss where ss.approvaltypeid in ('" + StringUtil.replaceAll(approvalTypeId, ";", "','") + "')  and ss.sdcid= '" + sdcid + "'" +
                " and exists (select null from " + table_id + " s where s." + table_keyid1 + " = ss.keyid1 and s." + table_keyid2 + " = ss.keyid2";

        String sqlDelApproval = "delete  from sdiapproval sa where sa.approvaltypeid in ('" + StringUtil.replaceAll(approvalTypeId, ";", "','") + "')  and sa.sdcid= '" + sdcid + "'" +
                " and exists (select null from " + table_id + " s where s." + table_keyid1 + " = sa.keyid1 and s." + table_keyid2 + " = sa.keyid2";

        if ("ParamList".equalsIgnoreCase(sdcid)) {
            sqlDelApprovalStep += " and s." + table_keyid3 + " = ss.keyid3 ";
            sqlDelApproval += " and s." + table_keyid3 + " = sa.keyid3 ";
        }
        sqlDelApprovalStep += " )";
        sqlDelApproval += " )";

        try {

            database.executeUpdate(sqlDelApprovalStep);
            database.executeUpdate(sqlDelApproval);
        } catch (Exception e) {
            //Do not throw this exception..
            logger.error("failed SQL *********************************");
            logger.error("failed SQL " + sqlDelApprovalStep);
            logger.error("failed SQL " + sqlDelApproval);
        }

    }
}