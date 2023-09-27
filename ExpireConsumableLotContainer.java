package labvantage.custom.alcon.actions;

import labvantage.custom.alcon.sap.util.ErrorMessageUtil;
import sapphire.SapphireException;
import sapphire.accessor.ActionException;
import sapphire.action.BaseAction;
import sapphire.action.EditSDI;
import sapphire.action.EditTrackItem;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.util.StringUtil;
import sapphire.xml.PropertyList;


/**
 * $Author: DASSA3 $
 * $Date: 2022-07-26 07:40:24 -0500 (Tue, 26 Jul 2022) $
 * $Revision: 73 $
 */

/********************************************************************************************
 * $Revision: 73 $
 * Description:
 * This class is being called to expire all consumable and reagent lot whose expiry date has crossed.
 *
 *
 *
 *******************************************************************************************/

public class ExpireConsumableLotContainer extends BaseAction {

    public static final String DEVOPS_ID = "$Revision: 73 $";
    private static final String __PROPS_ROWCOUNT = "RowCount";

    private static final String __PROPS_REAGENTLOT_SDCID = "LV_ReagentLot";
    private static final String __PROPS_REAGENTLOT_REAGENTLOTID = "reagentlotid";
    private static final String __PROPS_REAGENTLOT_REAGENTLOTSTATUS = "reagentstatus";

    private static final String __PROPS_STATUS = "Expired";
    private static final int __PROPS_MAX_ROW_COUNT = 999;

    private static final String __PROPS_TRACKITEM_TRACKITEMID = "trackitemid";
    private static final String __PROPS_TRACKITEM_LINKKEYID1 = "linkkeyid1";
    private static final String __PROPS_TRACKITEM_REAGENTLOTSTATUS = "trackitemstatus";


    /**
     * This methhod is called to perform expiration of Consumable Lot on a routine whose expiry dates have already crossed
     *
     * @param properties
     * @return void
     * @throws SapphireException
     */
    @Override
    public void processAction(PropertyList properties) throws SapphireException {
        DataSet dsExpiredTrackItem;
        DataSet dsExpiredReagentLot;
        int rowCount = __PROPS_MAX_ROW_COUNT;

        if (!(null == properties.getProperty(__PROPS_ROWCOUNT) || properties.getProperty(__PROPS_ROWCOUNT).equalsIgnoreCase(""))) {
            if (isNumeric(properties.getProperty(__PROPS_ROWCOUNT))) {
                rowCount = Integer.parseInt(properties.getProperty(__PROPS_ROWCOUNT));
            }
        } else {
            return;
        }

        dsExpiredTrackItem = fetchExpirdTrackItem(rowCount);
        dsExpiredReagentLot = fetchExpirdReagentLot(rowCount);

        if (dsExpiredTrackItem.getRowCount() == 0 && dsExpiredReagentLot.getRowCount() == 0) {
            return;
        } else {
            updateConsumableLotStatus(dsExpiredTrackItem, dsExpiredReagentLot);
        }
    }

    /**
     * This methhod is called to fetch list of all Consumable Lot that needs to be expired
     *
     * @param rowCount
     * @return DataSet
     * @throws
     */
    private DataSet fetchExpirdTrackItem(int rowCount) {
        String sqlText = "SELECT t.trackitemid, t.linkkeyid1 FROM trackitem t WHERE t.linksdcid = 'LV_ReagentLot' " +
                "AND (t.trackitemstatus IN ('Valid', 'Invalid') OR t.trackitemstatus IS NULL ) " +
                "AND SYSDATE > (CASE WHEN t.expirydt IS NULL THEN(SELECT lot.expirydt FROM reagentlot lot WHERE " +
                "lot.reagentlotid = t.linkkeyid1) ELSE expirydt END) AND ROWNUM <= ?";
        DataSet dsTemp = getQueryProcessor().getPreparedSqlDataSet(sqlText, new Object[]{rowCount});
        return dsTemp;
    }

    /**
     * This methhod is called to fetch list of all Reagent Lot that needs to be expired
     *
     * @param rowCount
     * @return DataSet
     * @throws
     */
    private DataSet fetchExpirdReagentLot(int rowCount) {
/*
        String sqlText = "SELECT lot.reagentlotid, lot.EXPIRYDT, lot.reagentstatus FROM REAGENTLOT lot, TRACKITEM item " +
                            "WHERE (lot.reagentstatus IN ('Active', 'Inactive', 'Initial', 'PendingApproval') OR lot.reagentstatus IS NULL) " +
                            "AND item.linkkeyid1 = lot.reagentlotid AND (item.trackitemstatus IN ('Valid', 'Invalid') OR item.trackitemstatus IS NULL) " +
                            "AND TRUNC(SYSDATE) > TRUNC(lot.EXPIRYDT) AND ROWNUM <= ?";
*/

        String sqlText = "SELECT  lot.reagentlotid, lot.EXPIRYDT, lot.reagentstatus FROM REAGENTLOT lot " +
                " where SYSDATE > lot.EXPIRYDT " +
                " and exists (select null from TRACKITEM item " +
                " WHERE (lot.reagentstatus IN ('Active', 'Inactive', 'Initial', 'PendingApproval') OR lot.reagentstatus IS NULL) " +
                " AND item.linkkeyid1 = lot.reagentlotid AND (item.trackitemstatus IN ('Valid', 'Invalid') OR item.trackitemstatus IS NULL)  " +
                " ) AND ROWNUM <= ?";


        DataSet dsTemp = getQueryProcessor().getPreparedSqlDataSet(sqlText, new Object[]{rowCount});
        return dsTemp;
    }

    /**
     * This method is called to update the status of Consumable Lot and Reagent Lot to Expired
     *
     * @param dsExpiredReagentLot
     * @param dsExpiredTrackItem
     * @return void
     * @throws SapphireException
     */
    private void updateConsumableLotStatus(DataSet dsExpiredTrackItem, DataSet dsExpiredReagentLot) throws SapphireException {

        try {
            if (dsExpiredTrackItem.size() > 0) {
                PropertyList plEditTrackItem = new PropertyList();
                plEditTrackItem.setProperty(EditTrackItem.PROPERTY_SDCID, __PROPS_REAGENTLOT_SDCID);
                plEditTrackItem.setProperty(EditTrackItem.PROPERTY_TRACKITEMID, dsExpiredTrackItem.getColumnValues(__PROPS_TRACKITEM_TRACKITEMID, ";"));
                plEditTrackItem.setProperty(EditTrackItem.PROPERTY_KEYID1, dsExpiredTrackItem.getColumnValues(__PROPS_TRACKITEM_LINKKEYID1, ";"));
                plEditTrackItem.setProperty(__PROPS_TRACKITEM_REAGENTLOTSTATUS, StringUtil.repeat(__PROPS_STATUS, dsExpiredTrackItem.getRowCount(), ";"));
                getActionProcessor().processAction(EditTrackItem.ID, EditTrackItem.VERSIONID, plEditTrackItem);
            }

            if (dsExpiredReagentLot.size() > 0) {
                PropertyList plEditSDI = new PropertyList();
                plEditSDI.setProperty(EditSDI.PROPERTY_SDCID, __PROPS_REAGENTLOT_SDCID);
                plEditSDI.setProperty(EditSDI.PROPERTY_KEYID1, dsExpiredReagentLot.getColumnValues(__PROPS_REAGENTLOT_REAGENTLOTID, ";"));
                plEditSDI.setProperty(__PROPS_REAGENTLOT_REAGENTLOTSTATUS, StringUtil.repeat(__PROPS_STATUS, dsExpiredReagentLot.getRowCount(), ";"));
                getActionProcessor().processAction(EditSDI.ID, EditSDI.VERSIONID, plEditSDI);
            }

        } catch (ActionException ex) {
            throw new SapphireException(ErrorMessageUtil.GENERAL_ERROR, ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(ex.getMessage()));
        }

    }

    /**
     * This methhod is called to fetch list of all Consumable Lot that needs to be expired
     *
     * @param str
     * @return boolean
     * @throws
     */
    private boolean isNumeric(String str) {
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c))
                return false;
        }
        return true;
    }

}
