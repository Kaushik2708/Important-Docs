package labvantage.custom.alcon.actions;

/**
 * $Author: BAGCHAN1 $
 * $Date: 2022-11-12 03:05:12 +0530 (Sat, 12 Nov 2022) $
 * $Revision: 307 $
 */

import labvantage.custom.alcon.sap.util.ErrorMessageUtil;
import sapphire.SapphireException;
import sapphire.action.BaseAction;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.xml.PropertyList;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/********************************************************************************************
 * Description:
 * This class is being called to update the security user for all samples and batches
 * where the security user is blank.
 *
 *******************************************************************************************/
public class UpdateSecurityUser extends BaseAction {

    public static final String DEVOPS_ID = "$Revision: 307 $";

    @Override
    public void processAction(PropertyList properties) throws SapphireException {

        logger.info("---------Starting UpdateSecurityUser ---------");

        //Getting all the required inputs
        String securityUser = properties.getProperty("securityuser", "");
        if ("".equalsIgnoreCase(securityUser)) {
            throw new SapphireException(ErrorMessageUtil.GENERAL_ERROR, ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Please enter Security User ID "));
        }
        validateAndUpdateSecurityUser(securityUser);
    }

    /**
     * @param securityUser
     * @return void
     * @throws SapphireException
     */
    private void validateAndUpdateSecurityUser(String securityUser) throws SapphireException {
        if (validateSecurityUser(securityUser)) {
            updateSecurityUser(securityUser);
        }
    }

    /**
     * @param securityUser
     * @return void
     * @throws SapphireException
     */
    private boolean validateSecurityUser(String securityUser) throws SapphireException {
        boolean validSecurityUser = false;
        String sqlText = " Select sysuserid from sysuser where sysuserid = ? ";
        DataSet dsUserCount = getQueryProcessor().getPreparedSqlDataSet(sqlText, new Object[]{securityUser});
        if (dsUserCount.getRowCount() == 0) {
            throw new SapphireException(ErrorMessageUtil.GENERAL_ERROR, ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" The User ID entered does not exist. Please enter a valid User ID. "));
        } else
            validSecurityUser = true;
        return validSecurityUser;
    }

    /**
     * @param securityUser
     * @return void
     * @throws SapphireException
     */
    private void updateSecurityUser(String securityUser) throws SapphireException {
        String updateSampleSql = " Update s_sample set securityuser = ? where securityuser is null ";
        String updateBatchSql = " Update s_batch set securityuser = ? where securityuser is null ";
        // ************* Updating Samples with security users
        updateSQL(updateSampleSql, securityUser);
        // ************* Updating Batch with security users
        updateSQL(updateBatchSql, securityUser);
    }

    /***************************************************
     * This method is used to update database tables.
     * @param sqlText SQL Text
     * @param securityUser Secuirity user
     * @throws SapphireException OOB Sapphire Exception
     ***************************************************/

    private void updateSQL(String sqlText, String securityUser) throws SapphireException {
        try {
            PreparedStatement psSecurityUser = database.prepareStatement(sqlText);
            try {
                psSecurityUser.setString(1, securityUser);
            } catch (SQLException ex) {
                throw new SapphireException("Unable to set Prepared Statement:Reason:" + ex.getMessage());
            }
            try {
                psSecurityUser.executeUpdate();
                psSecurityUser.clearParameters();
                psSecurityUser.close();
            } catch (SQLException e) {
                throw new SapphireException(ErrorMessageUtil.GENERAL_ERROR, ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Unable to execute UPDATE statement. " + e.getMessage()));
            } finally {
                psSecurityUser.close();
            }
        } catch (SQLException se) {
            throw new SapphireException(ErrorMessageUtil.GENERAL_ERROR, ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Exception thrown during execution of updateSQL method. " + se.getMessage()));
        }

    }
}