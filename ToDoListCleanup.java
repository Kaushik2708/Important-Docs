package labvantage.custom.alcon.actions;

import sapphire.SapphireException;
import sapphire.action.AddSDI;
import sapphire.action.BaseAction;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.xml.PropertyList;

/**
 * Clear error items from TodoList. The action is called from a recurring task.
 * $Author: BAGCHAN1 $
 * $Date: 2022-02-08 00:16:36 -0500 (Tue, 08 Feb 2022) $
 * $Revision: 13 $
 */

/*********************************************************************
 * $Revision: 13 $
 * Description:
 * Story item MDLIMS 640 Todolist cleanup script for error item
 ********************************************************************/

public class ToDoListCleanup extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 13 $";

    @Override
    public void processAction(PropertyList properties) throws SapphireException {

        String truncateHistoricalTodolist = properties.getProperty("truncatehistoricaltodolist", "Y");
        String truncateForPreviousMonth = properties.getProperty("truncateforpreviousmonth", "3");

        DataSet dsSql = getHistoricalTodoListData();

        if (dsSql.size() == 0) {
            truncateHistoricalTodolist(truncateHistoricalTodolist, truncateForPreviousMonth);
            return;
        }

        handleHistoricalTodoLIst(dsSql, truncateHistoricalTodolist, truncateForPreviousMonth);

        clearLVTodoList();

    }

    /**
     * Get historical todo list
     *
     * @return
     * @throws SapphireException
     */
    private DataSet getHistoricalTodoListData() throws SapphireException {
        //--All column values are fetched ---
        String sqlText = "select * from todolist where statusflag='E' ";
        DataSet dsSql = getQueryProcessor().getSqlDataSet(sqlText, true);

        if (dsSql == null) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, "Failed to query ToDolist table");
        }

        return dsSql;
    }

    /**
     * 1. Copy all errored items from TodoList to a new table HistoricalTodoIist.
     * 2. Clear all data from table HistoricalTodoIist older than 3 months from current date of run of the task.
     *
     * @throws SapphireException
     */
    private void handleHistoricalTodoLIst(DataSet dsSql, String truncateHistoricalTodolist, String truncateForPreviousMonth) throws SapphireException {

        String colNames[] = dsSql.getColumns();
        PropertyList pl = new PropertyList();
        for (int i = 0; i < dsSql.size(); i++) {
            pl.clear();
            pl.setProperty(AddSDI.PROPERTY_SDCID, "HistoricalTodoList");
            pl.setProperty(AddSDI.PROPERTY_COPIES, "1");

            for (int j = 0; j < colNames.length; j++) {
                String col = colNames[j];
                if (DataSet.CLOB == dsSql.getColumnType(col)) {
                    pl.setProperty(col, dsSql.getClob(i, col, ""));
                } else {
                    pl.setProperty(col, dsSql.getValue(i, col, ""));
                }
            }

            // Writing AddSDI in a loop because clob data can't be inserted with multiple semicolon separated values.
            getActionProcessor().processAction(AddSDI.ID, AddSDI.VERSIONID, pl);
        }

        truncateHistoricalTodolist(truncateHistoricalTodolist, truncateForPreviousMonth);

    }


    /**
     * Clear old records from historical todolist sdc.
     *
     * @param truncateHistoricalTodolist
     * @param truncateForPreviousMonth
     * @throws SapphireException
     */
    private void truncateHistoricalTodolist(String truncateHistoricalTodolist, String truncateForPreviousMonth) throws SapphireException {

        if ("Y".equalsIgnoreCase(truncateHistoricalTodolist) || "Yes".equalsIgnoreCase(truncateHistoricalTodolist)) {

            String deleteSQL = "delete from u_historicaltodolist where trunc(queueddt) <= (trunc(add_months(sysdate, -" + truncateForPreviousMonth + ")))";
            database.executeUpdate(deleteSQL);

        }
    }

    /**
     * Delete table - TODOLIST
     *
     * @throws SapphireException
     */
    private void clearLVTodoList() throws SapphireException {
        String sql = "delete from todolist where statusflag='E'";
        database.executeUpdate(sql);

    }


}
