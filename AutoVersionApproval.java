package labvantage.custom.alcon.actions;

import sapphire.SapphireException;
import sapphire.action.BaseAction;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.xml.PropertyList;

import java.util.HashMap;

/**
 * $Revision: 13 $
 * This class is used to automatically set the version status to courrent based on effective date.
 * The action will be added to a task which will be invoked once  in 24 hours
 * <p>
 * $Author: BAGCHAN1 $
 * $Date: 2022-02-08 00:16:36 -0500 (Tue, 08 Feb 2022) $
 * $Revision: 13 $
 */
public class AutoVersionApproval extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 13 $";

    public void processAction(PropertyList properties) throws SapphireException {
        String sdcid = properties.getProperty("sdcid", "");
        String columnid = properties.getProperty("columnid", "");
        if ("".equals(sdcid) || "".equals(columnid)) {
            throw new SapphireException("Missing input values ");
        }
        setVersionStatus(sdcid, columnid);
    }

    public void setVersionStatus(String sdcid, String columnid) throws SapphireException {
        String sql = "select tableid from sdc where sdcid in ('" + sdcid + "')";
        DataSet dsTable = getQueryProcessor().getSqlDataSet(sql);
        if (dsTable == null || dsTable.size() == 0) {
            throw new SapphireException("Invalid SDC: " + sdcid);
        }
        DataSet dsColumnDetails = getSDCProcessor().getColumnData(sdcid);
        if (dsColumnDetails != null && dsColumnDetails.size() > 0) {
            HashMap hmFilter = new HashMap();
            hmFilter.put("pkflag", "Y");
            DataSet filterPrimaryDs = dsColumnDetails.getFilteredDataSet(hmFilter);
            String sqlEffectiveDt = "select " + columnid + ", " + filterPrimaryDs.getColumnValues("columnid", ",") + " " +
                    "from " + dsTable.getValue(0, "tableid", "") + " where " +
                    "versionstatus ='P' and " +
                    columnid + " <= sysdate";
            DataSet dsPrimary = getQueryProcessor().getSqlDataSet(sqlEffectiveDt);

            if (dsPrimary == null) {
                throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, "Failed to query data from table. Query issued: " + sqlEffectiveDt);
            }

            if (dsPrimary.size() == 0) {
                return; // No update needed
            }

            String sortBy = columnid + ", " + filterPrimaryDs.getColumnValues("columnid", ",");
            dsPrimary.sort(sortBy);

            int columncount = dsPrimary.getColumnCount();
            PropertyList pl = new PropertyList();

            for (int i = 0; i < dsPrimary.size(); i++) {
                pl.clear();
                pl.setProperty("sdcid", sdcid);
                pl.setProperty("versionstatus", "C");
                pl.setProperty("auditactivity", "Auto approval based on effective date");

                int keyIdCounter = 1;
                for (int j = 0; j < columncount; j++) {
                    if (!dsPrimary.getColumnId(j).equalsIgnoreCase(columnid)) {
                        pl.setProperty("keyid" + keyIdCounter++, dsPrimary.getValue(i, dsPrimary.getColumnId(j), ""));
                    }
                }

                try {
                    getActionProcessor().processAction("SetSDIVersionStatus", "1", pl);
                } catch (SapphireException e) {
                    throw new SapphireException("Unable to set the version to current for " + dsPrimary.getColumnValues(dsPrimary.getColumnId(0), ";") + " SDI(s). " + e.getMessage());
                }

            }

        }
    }
}
