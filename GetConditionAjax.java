package labvantage.custom.alcon.ajax;

import sapphire.servlet.AjaxResponse;
import sapphire.servlet.BaseAjaxRequest;
import sapphire.util.DataSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * $Author: BAGCHAN1 $
 * $Date: 2022-02-08 00:16:36 -0500 (Tue, 08 Feb 2022) $
 * $Revision: 13 $
 */

/*******************************************************************
 * $Revision: 13 $
 * Description: This class call onload of the Study Maint page. This is used for populating condition under Storage details.
 *
 *******************************************************************/

public class GetConditionAjax extends BaseAjaxRequest {

    public static final String DEVOPS_ID = "$Revision: 13 $";

    public void processRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, ServletContext servletContext) {

        AjaxResponse ajaxResponse = new AjaxResponse(httpServletRequest, httpServletResponse);
        try {
            String studyId = ajaxResponse.getRequestParameter("studyid", "");
            String sql = "SELECT  ('Plan: ' || sp.scheduleplandesc || ' |Condition: '|| sc.conditionlabel) conditionlabel " +
                    " FROM  study_scheduleplan ssp, scheduleplan sp, schedulecondition sc " +
                    " WHERE ssp.scheduleplanid = sp.scheduleplanid and sc.scheduleplanid = sp.scheduleplanid " +
                    " AND ssp.studyid = ?  order by ssp.usersequence, sp.scheduleplandesc , conditionlabel ";

            //Processing the query
            DataSet dsSQL = getQueryProcessor().getPreparedSqlDataSet(sql, new Object[]{studyId});

            if (dsSQL == null || dsSQL.size() == 0) {
                ajaxResponse.addCallbackArgument("dataset", "No data found");
            } else {
                ajaxResponse.addCallbackArgument("dataset", dsSQL);
            }
        } catch (Exception e) {
            ajaxResponse.setError(e.getMessage());
        } finally {
            ajaxResponse.print();
        }

    }
}
