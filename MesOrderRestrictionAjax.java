package labvantage.custom.alcon.ajax;

import sapphire.SapphireException;
import sapphire.error.ErrorDetail;
import sapphire.servlet.AjaxResponse;
import sapphire.servlet.BaseAjaxRequest;
import sapphire.util.DataSet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;


/**
 * $Author $
 * $Date: 2022-04-20 01:35:09 +0530 (Wed, 20 Apr 2022) $
 * $Revision: 47 $
 */

/*******************************************************************
 * $Revision: 47 $
 * Description: This class called from First validation of Save in BatchMaint ( Add Batch Tram )
 *
 *******************************************************************/

public class MesOrderRestrictionAjax extends BaseAjaxRequest {

    public static final String DEVOPS_ID = "$Revision: 47 $";

    public void processRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, ServletContext servletContext) throws ServletException {
        AjaxResponse ajaxResponse = new AjaxResponse(httpServletRequest, httpServletResponse);

        String mesOrderNo = "";
        String product = "";
        String securityDept = "";
        String stop = ""; //HARD/SOFT
        String outputmsg = "";
        String isRestricted = "N";
        String currentuser = getConnectionProcessor().getConnectionInfo(getConnectionid()).getSysuserId();
        //TODo Batch Security Dept (restricted Y/N) and then write SDC Rule
        try {
            mesOrderNo = ajaxResponse.getRequestParameter("mesorderno", "");
            product = ajaxResponse.getRequestParameter("product", "");
            //******* Added as a part of MDLIMS 1157
            // Getting seciruty department from screen
            securityDept = ajaxResponse.getRequestParameter("securitydept", "");
            // Checking if INPUT security department  is BLANK
            if ("".equalsIgnoreCase(securityDept)) {
                // Getting Default department of current user
                String sqlDefaultUserDept = "SELECT defaultdepartment FROM sysuser  WHERE sysuserid = ? ";
                DataSet dsDefaultUserDept = getQueryProcessor().getPreparedSqlDataSet(sqlDefaultUserDept, new Object[]{currentuser});
                if (null == dsDefaultUserDept) {
                    throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction .Incorrect SQL statement found. SQL is \n" + sqlDefaultUserDept));
                } else if (dsDefaultUserDept.size() == 0) {
                    return;
                } else {
                    securityDept = dsDefaultUserDept.getValue(0, "defaultdepartment", "");
                }
            }
            // Check if security department is still BLANK or not ?
            if ("".equalsIgnoreCase(securityDept)) {
                return;
            }
            // Get the MES Order Restriction of Department
            String sqlMESOrderRestriction = " SELECT u_mesorderrestriction FROM department WHERE departmentid = ? ";
            DataSet dsMESOrderRestriction = getQueryProcessor().getPreparedSqlDataSet(sqlMESOrderRestriction, new Object[]{securityDept});
            if (null == dsMESOrderRestriction) {
                throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction .Incorrect SQL statement found. SQL is \n" + sqlMESOrderRestriction));
            } else {
                isRestricted = dsMESOrderRestriction.getValue(0, "u_mesorderrestriction", "N");
            }


            /***** Commented below codes as a part of MDLIMS - 1157
             /*

             String sqlBatch =
             "select b.s_batchid,b.batchstatus,b.productid,b.u_mesordersapbatch1,b.securitydepartment," +
             " (select NVL(d.u_mesorderrestriction,'N') from department d where d.departmentid = b.securitydepartment) restriction from s_batch b" +
             " where u_mesordersapbatch1 ='" + mesOrderNo + "' and  b.productid='" + product + "' ";

             if (!restrictionFromBatch.equals("")) {
             isRestricted = restrictionFromBatch;
             } else {
             String sqlDept =
             "select s.sysuserid,s.defaultdepartment,NVL(d.u_mesorderrestriction,'N') restriction" +
             " from sysuser s, department d where s.defaultdepartment = d.departmentid" +
             " and s.sysuserid='" + currentuser + "'";

             DataSet dsDept = getQueryProcessor().getSqlDataSet(sqlDept);
             if (dsDept == null) {
             String errStr = "Query returns null. Please contact your administrator.";
             logger.error(errStr + "\nSQL failed: " + sqlDept);
             throw new SapphireException(errStr);
             }
             isRestricted = dsDept.getValue(0, "restriction", "");
             }*/

            // Getting all Existing LIMS Batch details by matching MES Order / SAP Batch # + Product Id + Security Department combination
            String sqlBatch = " SELECT s_batchid,batchstatus,createdt FROM s_batch WHERE u_mesordersapbatch1 = ? AND productid = ? AND securitydepartment = ?  ";
            DataSet dsExistingBatch = getQueryProcessor().getPreparedSqlDataSet(sqlBatch, new Object[]{mesOrderNo, product, securityDept});
            if (null == dsExistingBatch) {
                throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction .Incorrect SQL statement found. SQL is \n" + sqlBatch));
            } else if (dsExistingBatch.getRowCount() == 0) {
                //indicates that no batch exists with dupilicate mesorder no.
                return;
            }


            if (isRestricted.equalsIgnoreCase("N")) {
                //Non restricted department block
                if (dsExistingBatch.size() > 0) {
                    stop = "SOFT";
                    outputmsg = "MES Order/SAP Batch " + mesOrderNo + " is already in use in other batch. Want to proceed?.";
                }
            } else { // restricted department block
                String batchIds = dsExistingBatch.getColumnValues(" s_batchid", ";");
                String batchStatus = dsExistingBatch.getColumnValues("batchstatus", ";");

                //Note: I am assuming that there can be multiple batches where one is Canncelled and another in Completed/Hold
                //1. One batch comes status is cancelled.
                //2. One batch comes and status is Non Cancelled( In Progress/On Hold/Completed)
                //3. Multiple batches comes and status is Cancelled;Completed

                int noOfRecord = dsExistingBatch.getRowCount();
                if (noOfRecord == 1) {
                    HashMap hmfilter = new HashMap();
                    hmfilter.clear();
                    hmfilter.put("batchstatus", "Cancelled");
                    DataSet dsfilteredBatch = dsExistingBatch.getFilteredDataSet(hmfilter);

                    if (dsfilteredBatch.size() == 0) {
                        //Hard Alert
                        stop = "HARD";
                        outputmsg = "MES Order/SAP Batch " + mesOrderNo + " is already in use. \nPlease use different MES Order/SAP Batch.";
                    } else {
                        //Soft alert with cancelled batch
                        stop = "SOFT";
                        outputmsg = "MES Order/SAP Batch " + mesOrderNo + " is already in use in other cancelled batch. Want to proceed?";
                    }
                } else {

                    dsExistingBatch.addColumn("iscancelled", DataSet.STRING);

                    for (int i = 0; i < dsExistingBatch.getRowCount(); i++) {
                        if ("Cancelled".equalsIgnoreCase(dsExistingBatch.getValue(i, "batchstatus", ""))) {
                            dsExistingBatch.setValue(i, "iscancelled", "Y");
                        } else {
                            dsExistingBatch.setValue(i, "iscancelled", "N");
                        }
                    }
                    HashMap hmfilter = new HashMap();
                    hmfilter.clear();
                    hmfilter.put("iscancelled", "N"); //Other than Cancelled
                    DataSet dsfilter = dsExistingBatch.getFilteredDataSet(hmfilter);
                    if (dsfilter != null && dsfilter.size() > 0) {
                        //Hard Alert
                        stop = "HARD";
                        outputmsg = "MES Order/SAP Batch " + mesOrderNo + " is already in use. \nPlease use different MES Order/SAP Batch.";
                    } else {
                        //Soft alert with cancelled batch
                        stop = "SOFT";
                        outputmsg = "MES Order/SAP Batch " + mesOrderNo + " is already in use in other cancelled batch. Want to proceed?";
                    }

                }
            }
        } catch (Exception exp) {
            ajaxResponse.setError(exp.getMessage());
        } finally {
            ajaxResponse.addCallbackArgument("stoptype", stop);
            ajaxResponse.addCallbackArgument("message", outputmsg);
            ajaxResponse.addCallbackArgument("isrestricted", isRestricted);
            ajaxResponse.print();
        }
    }
}