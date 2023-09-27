package labvantage.custom.alcon.actions;

/**
 * $Author:  $
 * $Date:  $
 * $Revision:  $
 */

import labvantage.custom.alcon.sap.util.ErrorMessageUtil;
import sapphire.SapphireException;
import sapphire.action.BaseAction;
import sapphire.action.EditSDI;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.xml.PropertyList;
import java.util.HashMap;

/********************************************************************************************
 * Description:
 * This class is being called to update the instrument's connect server id with primary host name
 *******************************************************************************************/
public class UpdateInstrumentConnectServerID extends BaseAction {

    public static final String DEVOPS_ID = "$Revision:  $";
    public static final String __HOST_NAME = "CONNECT";

    @Override
    public void processAction(PropertyList properties) throws SapphireException {

        logger.info("---------Starting UpdateInstrumentConnectServerID ---------");
        String hostName = getPrimaryServerHostName();
        String connectInstrumentIDs = getInstrumentConnectServerID();

        if (hostName.equals("")) {
            hostName = __HOST_NAME;
        }
        if (!connectInstrumentIDs.equals("")) {
            updateInstrumentServerID(connectInstrumentIDs, hostName);
        }
    }

    /**
     * This method will update the instrumentserverid in Instrument SDC
     * @param connectInstrumentIDs
     * @param hostName
     * @throws SapphireException SapphireException
     */
    private void updateInstrumentServerID(String connectInstrumentIDs, String hostName) throws SapphireException {
        HashMap<String, String> hmProps = new HashMap<>();
        hmProps.put("sdcid", "Instrument");
        hmProps.put("keyid1", connectInstrumentIDs);
        hmProps.put("cin_instrumentserverid", hostName);
        try {
            getActionProcessor().processAction(EditSDI.ID, EditSDI.VERSIONID, hmProps);
        } catch (SapphireException ex) {
            throw new SapphireException(ErrorMessageUtil.GENERAL_ERROR, ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(ex.getMessage()));
        }
    }

    /**
     * This method will return host name of primary server
     * @return
     * @throws SapphireException
     */
    private String getPrimaryServerHostName() throws SapphireException {
        String sqlText = "select distinct hostid from serverinstance where primaryautomationserverflag = 'Y'";
        DataSet dsHostName = getQueryProcessor().getSqlDataSet(sqlText);
        if (dsHostName == null) {
            throw new SapphireException(ErrorMessageUtil.GENERAL_ERROR, ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Failed to execute SQL query. "));
        } else
            return dsHostName.getColumnValues("hostid", ";");
    }

    /**
     * This method will return the list of connect instruments whose instrument server ID is not the primary host name
     * @return
     * @throws SapphireException
     */
    private String getInstrumentConnectServerID() throws SapphireException {
        String sqlText = "select distinct instrumentid from instrument where cin_instrumentserverid <> (select hostid from serverinstance where primaryautomationserverflag = 'Y') and cin_instrumentserverid is not null";
        DataSet dsConnectInstrument = getQueryProcessor().getSqlDataSet(sqlText);
        if (dsConnectInstrument == null) {
            throw new SapphireException(ErrorMessageUtil.GENERAL_ERROR, ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Failed to execute SQL query. "));
        } else
            return dsConnectInstrument.getColumnValues("instrumentid", ";");
    }
}