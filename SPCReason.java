/**
 * Copyright (c) 2017 LabVantage.  All rights reserved.
 * <p/>
 * This software and documentation is the confidential and proprietary
 * information of LabVantage. ("Confidential Information").
 * You shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement you
 * entered into with LabVantage.
 * <p/>
 * If you are not authorized by LabVantage to utilize this
 * software and/or documentation, you must immediately discontinue any
 * further use or viewing of this software and documentation.
 * Violators will be prosecuted to the fullest extent of the law by
 * LabVantage.
 * <p/>
 * Developed by LabVantage Solutions, Inc.
 * 265 Davidson Avenue
 * Suite 220
 * Somerset, NJ 08873
 * <p/>
 */
package labvantage.custom.alcon.ajax;

import labvantage.custom.alcon.util.AlconUtil;
import sapphire.servlet.AjaxResponse;
import sapphire.servlet.BaseAjaxRequest;
import sapphire.util.DataSet;
import sapphire.util.StringUtil;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * $Author: BAGCHAN1 $
 * $Date: 2022-04-20 01:35:09 +0530 (Wed, 20 Apr 2022) $
 * $Revision: 47 $
 */

/*****************************************************************************************************
 * $Revision: 47 $
 * Description:
 * Returns the spc_reason and spc_template from DataSet and Sample.
 *
 ******************************************************************************************************/

public class SPCReason extends BaseAjaxRequest {

    public static final String ID = "SPCReason";
    public static final String VERSIONID = "1";
    static final String LABVANTAGE_CVS_ID = "$Revision: 47 $";

    @Override
    public void processRequest(HttpServletRequest request, HttpServletResponse response, ServletContext servletContext)
            throws ServletException {
         /*
		//--------------------- Original code --------------
        AjaxResponse ajaxResponse = new AjaxResponse( request, response );
        String sampleid = ajaxResponse.getRequestParameter( "sampleID", "" );

        String SPCValue = "select sdi.u_spc_reason,s.spc_templateid \nfrom sdidata sdi\njoin s_sample s on s.s_sampleid = sdi.keyid1\nwhere sdi.keyid1  = '%s' \nand sdi.sdcid = 'Sample'";

        DataSet dsSPCReasonEntered = getQueryProcessor().getPreparedSqlDataSet( String.format( SPCValue, new Object[] { sampleid } ), new Object[0] );

        ajaxResponse.addCallbackArgument( "spcreason", dsSPCReasonEntered.getColumnValues( "u_spc_reason", "';'" ) );
        ajaxResponse.addCallbackArgument( "spc_templateid", dsSPCReasonEntered.getColumnValues( "spc_templateid", "';'" ) );
        ajaxResponse.print();

		//--------------------- Original code --------------
		*/

        //Commenting out the original code above and writing a new code to resolve Jira item MDLIMS-552
        //Issue Description: When user is trying to review data, they receive the following message:
        //'Save changes before proceeding'. This is not always reproducible. As per the user, this issue reproduces once in a shift.

        AjaxResponse ajaxResponse = new AjaxResponse(request, response);
        String sampleid = ajaxResponse.getRequestParameter("sampleID", "");
        if (sampleid.equals("")) {
            ajaxResponse.setError("Please select atleast one item.");
            ajaxResponse.print();
            return;
        }

        //--- Get Unique SampleIds since from dataentry page repeatitive sampleid may appear ---//
        sampleid = AlconUtil.getUniqueList(sampleid, ";");

        //-- Assumed in the query - less than 1000 Samples will be coming from dataentry page---//
        String sqlSPCValue = " SELECT  " +
                "s.s_sampleid,  " +
                "sd.u_spc_reason,  " +
                "s.spc_templateid,  " +
                "spcc.spc_controlcardid,  " +
                "sd.paramlistid,  " +
                "sd.paramlistversionid,  " +
                "sd.variantid,  " +
                "sd.dataset  " +
                "FROM  " +
                "s_sample s " +
                " INNER JOIN sdidata sd  ON sd.sdcid = 'Sample' and sd.keyid1 = s.s_sampleid " +
                " LEFT OUTER JOIN spc_sdicontrolcard spcc ON s.s_sampleid = spcc.key1   " +
                " WHERE   " +
                " s.s_sampleid IN ('" + StringUtil.replaceAll(sampleid, ";", "','") + "') ";

        DataSet dsSPCReasonEntered = getQueryProcessor().getSqlDataSet(sqlSPCValue);
        if (dsSPCReasonEntered == null) {
            logger.error(" Aborting transaction. Unable to execute SQL command. SQL is" + sqlSPCValue);
            throw new ServletException(" Aborting transaction. Unable to execute SQL command.Please refer to log file for more details.");
        }
        ajaxResponse.addCallbackArgument("sampleid", dsSPCReasonEntered.getColumnValues("s_sampleid", ";"));
        ajaxResponse.addCallbackArgument("spcreason", dsSPCReasonEntered.getColumnValues("u_spc_reason", ";"));
        ajaxResponse.addCallbackArgument("spc_templateid", dsSPCReasonEntered.getColumnValues("spc_templateid", ";"));
        ajaxResponse.addCallbackArgument("spc_controlcardid", dsSPCReasonEntered.getColumnValues("spc_controlcardid", ";"));
        ajaxResponse.addCallbackArgument("paramlistid", dsSPCReasonEntered.getColumnValues("paramlistid", ";"));
        ajaxResponse.addCallbackArgument("paramlistversionid", dsSPCReasonEntered.getColumnValues("paramlistversionid", ";"));
        ajaxResponse.addCallbackArgument("variantid", dsSPCReasonEntered.getColumnValues("variantid", ";"));
        ajaxResponse.addCallbackArgument("dataset", dsSPCReasonEntered.getColumnValues("dataset", ";"));
        ajaxResponse.print();

    }

}

