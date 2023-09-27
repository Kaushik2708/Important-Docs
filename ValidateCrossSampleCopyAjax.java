package labvantage.custom.alcon.ajax;

import sapphire.SapphireException;
import sapphire.servlet.AjaxResponse;
import sapphire.servlet.BaseAjaxRequest;
import sapphire.util.DataSet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;


/**
 * $Author: BAGCHAN1 $
 * $Date: 2022-04-20 01:35:09 +0530 (Wed, 20 Apr 2022) $
 * $Revision: 47 $
 */

/*******************************************************************
 * $Revision: 47 $
 * Description: Cross Sample Data Item Copy.
 *
 *******************************************************************/

public class ValidateCrossSampleCopyAjax extends BaseAjaxRequest {

    public static final String DEVOPS_ID = "$Revision: 47 $";

    public void processRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, ServletContext servletContext) throws ServletException {
        AjaxResponse ajaxResponse = new AjaxResponse(httpServletRequest, httpServletResponse);

        String keyid1 = ajaxResponse.getRequestParameter("src_keyid1", "");
        String paramlistid = ajaxResponse.getRequestParameter("src_paramlistid", "");
        String paramlistversionid = ajaxResponse.getRequestParameter("src_paramlistversionid", "");
        String variantid = ajaxResponse.getRequestParameter("src_variantid", "");
        String dataset = ajaxResponse.getRequestParameter("src_dataset", "");
        String paramid = ajaxResponse.getRequestParameter("src_paramid", "");
        String paramtype = ajaxResponse.getRequestParameter("src_paramtype", "");
        String replicateid = ajaxResponse.getRequestParameter("src_replicateid", "");
        if (keyid1.equalsIgnoreCase("")) {
            String err = "No Sample Id provided";
            throw new ServletException(err);
        }

        DataSet ds = null;

        try {
            validateSelectedSample(keyid1, paramlistid, paramlistversionid, variantid, dataset, paramid, paramtype, replicateid);
            ds = getDataItem(keyid1);

            ajaxResponse.addCallbackArgument("srckeyid1", keyid1);
            ajaxResponse.addCallbackArgument("srcparamlistid", paramlistid);
            ajaxResponse.addCallbackArgument("srcparamlistversionid", paramlistversionid);
            ajaxResponse.addCallbackArgument("srcvariantid", variantid);
            ajaxResponse.addCallbackArgument("srcdataset", dataset);
            ajaxResponse.addCallbackArgument("srcparamid", paramid);
            ajaxResponse.addCallbackArgument("srcparamtype", paramtype);
            ajaxResponse.addCallbackArgument("srcreplicateid", replicateid);
            ajaxResponse.addCallbackArgument("targetdataset", ds);
        } catch (Exception exp) {
            ajaxResponse.setError(exp.getMessage());
        } finally {
            ajaxResponse.print();
        }
    }

    /**
     * Validate the input sampleid.
     *
     * @param keyid1
     * @param paramlistid
     * @param paramlistversionid
     * @param variantid
     * @param dataset
     * @param paramid
     * @param paramtype
     * @param replicateid
     * @throws SapphireException
     */
    private void validateSelectedSample(String keyid1, String paramlistid, String paramlistversionid, String variantid, String dataset,
                                        String paramid, String paramtype, String replicateid) throws SapphireException {
        String sql = " select datatypes,calcrule,releasedflag from sdidataitem where sdcid = 'Sample' and" +
                " keyid1 = ? and paramlistid = ? and paramlistversionid = ? and variantid = ?" +
                " and dataset = ? and paramid = ? and paramtype = ? and replicateid = ?";


        DataSet ds = getQueryProcessor().getPreparedSqlDataSet(sql, new Object[]{keyid1, paramlistid, paramlistversionid, variantid, dataset, paramid, paramtype, replicateid});
        if (ds == null || ds.size() == 0) {
            throw new SapphireException("No data item found.");
        }

        if ("NC".equalsIgnoreCase(ds.getValue(0, "datatypes", ""))) {
            String err = "Please select Non Calculated field.";
            throw new SapphireException(err);
        }
        if ("Y".equalsIgnoreCase(ds.getValue(0, "releasedflag", "N"))) {
            String err = "Please unrelease the selected item.";
            throw new SapphireException(err);
        }

    }

    /**
     * Description: Returns data item.
     *
     * @param sourceSampleId
     * @return
     * @throws SapphireException
     */

    private DataSet getDataItem(String sourceSampleId) throws SapphireException {

        String sqlBatch = " select s.s_sampleid from s_sample s, s_batch b where s.batchid= b.s_batchid and b.s_batchid = (" +
                " select batchid from s_sample where s_sampleid = ? )" +
                " and s.samplestatus!= 'Cancelled'";

        DataSet dsBatch = getQueryProcessor().getPreparedSqlDataSet(sqlBatch, new Object[]{sourceSampleId});
        if (dsBatch == null || dsBatch.size() == 0) {
            String err = "Please select Sample that is linked to a Batch.";
            throw new SapphireException(err);
        }

        //Filter out the source Sample Id.
        dsBatch.addColumn("sourcesampleflag", DataSet.STRING);
        for (int i = 0; i < dsBatch.size(); i++) {
            if (sourceSampleId.equalsIgnoreCase(dsBatch.getValue(i, "s_sampleid", ""))) {
                dsBatch.setValue(i, "sourcesampleflag", "Y");
            } else {
                dsBatch.setValue(i, "sourcesampleflag", "N");
            }
        }
        HashMap hm = new HashMap();
        hm.put("sourcesampleflag", "N");
        DataSet dsFilterBatch = dsBatch.getFilteredDataSet(hm);
        if (dsFilterBatch.size() == 0) {
            String err = "No other active Sample found.";
            throw new SapphireException(err);
        }

        String otherSamples = dsFilterBatch.getColumnValues("s_sampleid", ";");
        // -- Parmlist dataSet for the other Sample ----
        String strRSet = getDAMProcessor().createRSet("Sample", otherSamples, "", "");
        String strQuery = "select sd.sdcid, sd.keyid1, sd.keyid2, sd.paramlistid, sd.paramlistversionid, sd.variantid, sd.paramid, sd.paramtype," +
                " sd.dataset, sd.replicateid,sd.enteredtext, pli.u_allowsourcevaluecopyflag, b.label " +
                " from sdidataitem sd, paramlistitem pli,rsetitems r, s_batchstage b, s_sample s where sd.sdcid='Sample'" +
                " and sd.paramlistid = pli.paramlistid and sd.paramlistversionid = pli.paramlistversionid" +
                " and sd.variantid = pli.variantid and sd.paramid = pli.paramid and sd.paramtype = pli.paramtype and  pli.u_allowsourcevaluecopyflag='Y'" +
                " and r.sdcid=sd.sdcid and r.keyid1=sd.keyid1 and r.keyid2='(null)' and r.keyid3='(null)' " +
                " and b.s_batchstageid = s.batchstageid and s.s_sampleid = r.keyid1" +
                " and r.rsetid = ? ";
        DataSet dsParamList = this.getQueryProcessor().getPreparedSqlDataSet(strQuery, new Object[]{strRSet});
        if (!"".equals(strRSet)) {
            this.getDAMProcessor().clearRSet(strRSet);
        }
        if (dsParamList == null || dsParamList.size() == 0) {
            String err = "Relevant data not found.";
            throw new SapphireException(err);
        }
        dsParamList.sort("keyid1"); // Display the grid in sorted manner.

        return dsParamList;
    }
}
