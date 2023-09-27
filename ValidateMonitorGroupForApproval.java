package labvantage.custom.alcon.ajax;

import com.labvantage.sapphire.admin.ddt.LV_MonitorGroup;
import sapphire.servlet.AjaxResponse;
import sapphire.servlet.BaseAjaxRequest;
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

/********************************************************************************************
 * $Revision: 47 $
 * Description:
 * This class is being called to validate the monitor group status is Pending Release or not
 * and the samples testing is completed or not.
 *
 *
 *
 *******************************************************************************************/

public class ValidateMonitorGroupForApproval extends BaseAjaxRequest {

    public static final String DEVOPS_ID = "$Revision: 47 $";

    public void processRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, ServletContext servletContext) throws ServletException {
        AjaxResponse ajaxResponse = new AjaxResponse(httpServletRequest, httpServletResponse);
        try {
            ajaxResponse.addCallbackArgument("completestatus", "1");
            String monitorGroupIds = ajaxResponse.getRequestParameter("keyid1");
            monitorGroupIds = StringUtil.replaceAll(monitorGroupIds, "%3B", ";");

            verifyMonitorGroup(monitorGroupIds, ajaxResponse);
        } catch (Exception ex) {
            ajaxResponse.addCallbackArgument("completestatus", "2");
        }
        ajaxResponse.print();
    }

    private void verifyMonitorGroup(String monitorGroupIds, AjaxResponse ajaxResponse) {
        boolean isAllTestsFinished = true;
        String monitorGroupTestingMsg = "All testing has to be finished before proceeding. MonitorGroups testing is not finished yet for: ";
        String arrMonitorGroupId[] = StringUtil.split(monitorGroupIds, ";");
        String monitorGroupId = "";

        for (int i = 0; i < arrMonitorGroupId.length; i++) {
            monitorGroupId = arrMonitorGroupId[i];
            if (!LV_MonitorGroup.hasAllTestingFinished(monitorGroupId, this.getQueryProcessor())) {
                monitorGroupTestingMsg = monitorGroupTestingMsg + monitorGroupId + ", ";
                isAllTestsFinished = false;
            }
        }

        ajaxResponse.addCallbackArgument("isalltestsfinished", String.valueOf(isAllTestsFinished));
        ajaxResponse.addCallbackArgument("monitorgrouptestingmsg", monitorGroupTestingMsg.substring(0, monitorGroupTestingMsg.length() - 2));
    }

}
