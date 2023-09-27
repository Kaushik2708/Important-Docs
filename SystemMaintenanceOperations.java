package labvantage.custom.alcon.actions;

import labvantage.custom.alcon.util.AlconUtil;
import sapphire.SapphireException;
import sapphire.action.AddSDIDetail;
import sapphire.action.BaseAction;
import sapphire.action.EditSDI;
import sapphire.error.ErrorDetail;
import sapphire.util.DataSet;
import sapphire.xml.PropertyList;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * $Author: BAGCHAN1 $
 * $Date: 2022-04-20 01:39:24 +0530 (Wed, 20 Apr 2022) $
 * $Revision: 48 $
 */

/********************************************************************************************
 * $Revision: 48 $
 * Description:
 * This class is being called to control all the operation performed during System Maintenance.
 *
 *
 *
 *******************************************************************************************/

public class SystemMaintenanceOperations extends BaseAction {

    public static final String DEVOPS_ID = "$Revision: 48 $";
    private static final String __PROPS_OPERATION = "opr";
    private static final String __PROPS_CURRENTUSER = "currentuser";
    private static final String __PROPS_ACTIVE_FLAG = "Y";
    private static final String __PROPS_INACTIVE_FLAG = "N";
    private static final String __PROPS_DATE_FORMAT = "MM/dd/yyyy hh:mm a";
    // System Maintenance SDC properties
    private static final String __PROPS_SYSMAINT_SDCID = "SysMaintenance";
    private static final String PROPERTY_SYSMAINT_KEYID1 = "keyid1";
    private static final String __PROPS_SYSMAINT_STARTED_BY = "startedby";
    private static final String __PROPS_SYSMAINT_ACTUAL_START_DATE = "actualstartdate";
    private static final String __PROPS_SYSMAINT_EXTENDED_BY = "extendedby";
    private static final String __PROPS_SYSMAINT_ACTUAL_EXTEND_DATE = "actualextendeddate";
    private static final String __PROPS_SYSMAINT_ENDED_BY = "endedby";
    private static final String __PROPS_SYSMAINT_ACTUAL_END_DATE = "actualenddate";
    private static final String __PROPS_SYSMAINT_MAINTEANCE_STATUS = "maintenancestatus";
    private static final String __PROPS_SYSMAINT_DISABLEDREASON = "disablereason";
    private static final String __PROPS_SYSMAINT_EXTENDED_BY_HOURS = "extendedbyhh";
    private static final String __PROPS_SYSMAINT_EXTENDED_BY_MINUTES = "entendedbymm";
    private static final String __PROPS_SYSMAINT_PLANNED_END_DATE_CDT = "plannedenddate";
    private static final String __PROPS_SYSMAINT_PLANNED_END_DATE_CET = "plannedEndDateCET";
    private static final String __PROPS_SYSMAINT_PLANNED_END_DATE_SGT = "plannedEndDateSGT";
    private static final String __PROPS_SYSMAINT_PLANNED_END_DATE_HOURS = "plannedendtimehh";
    private static final String __PROPS_SYSMAINT_PLANNED_END_DATE_MINUTES = "plannedendtimemm";
    // User SDC properties
    private static final String __PROPS_USER_SDCID = "User";
    private static final String __PROPS_USER_KEYID1 = "sysuserid";
    private static final String __PROPS_USER_LINKID = "sysmaintenanceuser_link";
    private static final String __PROPS_USER_DISABLEDFORSYSMAINT = "u_disabledforsysmaint";
    private static final String __PROPS_USER_DISABLEDFLAG = "disabledflag";
    private static final String __PROPS_USER_DISABLEDREASON = "disabledreason";
    // Opeartion Types
    private static final String START_SYSTEM_MAINTENANCE = "StartSystemMaintenance";
    private static final String EXTEND_SYSTEM_MAINTENANCE = "ExtendSystemMaintenance";
    private static final String END_SYSTEM_MAINTENANCE = "EndSystemMaintenance";
    // Maintenace Status
    private static final String CREATED_SYSTEM_MAINTENANCE = "Created";
    private static final String STARTED_SYSTEM_MAINTENANCE = "Started";
    private static final String EXTENDED_SYSTEM_MAINTENANCE = "Extended";
    private static final String ENDED_SYSTEM_MAINTENANCE = "Ended";
    private String __ERRORMSG = "";
    private PropertyList plEditSDI = new PropertyList();

    /**
     * This methhod is called to perform various operation during System Maintenance phase
     *
     * @param properties
     * @return void
     * @throws SapphireException
     */
    @Override
    public void processAction(PropertyList properties) throws SapphireException {
        logger.debug("SystemMaintenanceOperations starts");


        String opr = properties.getProperty(__PROPS_OPERATION);
        if (null == opr || "".equalsIgnoreCase(opr)) {
            __ERRORMSG = "Cannot Proceed. Operation not specified.";
            throw new SapphireException(__ERRORMSG);
        }

        String id = properties.getProperty(PROPERTY_SYSMAINT_KEYID1);
        String currentUser = properties.getProperty(__PROPS_CURRENTUSER);

        switch (opr) {
            case START_SYSTEM_MAINTENANCE:
                startSystemMaintenance(id, opr, currentUser);
                break;
            case EXTEND_SYSTEM_MAINTENANCE:
                String extendedByHours = properties.getProperty(__PROPS_SYSMAINT_EXTENDED_BY_HOURS);
                String extendedByMinutes = properties.getProperty(__PROPS_SYSMAINT_EXTENDED_BY_MINUTES);
                extendSystemMaintenance(id, opr, currentUser, extendedByHours, extendedByMinutes);
                break;
            case END_SYSTEM_MAINTENANCE:
                endSystemMaintenance(id, opr, currentUser);
                break;

        }

    }


    /**
     * This methhod is called to perform start System Maintenance phase
     *
     * @param id
     * @param currentUser
     * @param opr
     * @return void
     * @throws SapphireException
     */
    private void startSystemMaintenance(String id, String opr, String currentUser) throws SapphireException {
        DataSet dsActiveUsers;
        DataSet dsExcludedUsers;
        DataSet dsSystemMaintenance;
        String disableReason = "";

        if (otherSystemMaintenanceInProgress()) {
            __ERRORMSG = "Another system maintenance event is in progress. This event cannot be started.";
            throw new SapphireException("Validation", ErrorDetail.TYPE_VALIDATION, __ERRORMSG);
        }

        dsSystemMaintenance = fetchCurrentSystemMaintenaceDetails(id);

        String maintenanceStatus = dsSystemMaintenance.getValue(0, __PROPS_SYSMAINT_MAINTEANCE_STATUS, "");
        if (maintenanceStatus.equalsIgnoreCase(CREATED_SYSTEM_MAINTENANCE)) {
            dsActiveUsers = fetchListOfActiveUsers();
            dsExcludedUsers = fetchListOfExcludedUsers(id);

            if (dsExcludedUsers.size() == 0 || dsExcludedUsers.findRow(__PROPS_USER_KEYID1, currentUser) == -1) {
                addCurrentUserToExcludedUserList(id, currentUser, dsActiveUsers);
                int rowid = dsExcludedUsers.addRow();
                dsExcludedUsers.setValue(rowid, "sysuserid", currentUser);
            }

            removeFromDataSet(__PROPS_USER_KEYID1, dsActiveUsers, dsExcludedUsers);

            Date plannedEndDateCDT = dsSystemMaintenance.getCalendar(0, __PROPS_SYSMAINT_PLANNED_END_DATE_CDT).getTime();
            Date plannedEndDateCET = dsSystemMaintenance.getCalendar(0, __PROPS_SYSMAINT_PLANNED_END_DATE_CET).getTime();
            Date plannedEndDateSGT = dsSystemMaintenance.getCalendar(0, __PROPS_SYSMAINT_PLANNED_END_DATE_SGT).getTime();

            boolean usDayLightSavingOn = validateDayLightSaving(plannedEndDateCDT, "US/Central");
            boolean germanyDayLightSavingOn = validateDayLightSaving(plannedEndDateCET, "CET");

            if (usDayLightSavingOn == true) {
                Calendar plannedEndDateSGTFormatted = AlconUtil.addHoursMinutesToJavaUtilDate(plannedEndDateSGT, "-1", "00");
                if (germanyDayLightSavingOn == false) {
                    Calendar plannedEndDateCETFormatted = AlconUtil.addHoursMinutesToJavaUtilDate(plannedEndDateCET, "-1", "00");
                    disableReason = formDisableReason(plannedEndDateCDT, plannedEndDateCETFormatted.getTime(), plannedEndDateSGTFormatted.getTime());
                } else {
                    disableReason = formDisableReason(plannedEndDateCDT, plannedEndDateCET, plannedEndDateSGTFormatted.getTime());
                }
            } else {
                disableReason = formDisableReason(plannedEndDateCDT, plannedEndDateCET, plannedEndDateSGT);
            }


            if (dsActiveUsers.size() > 0) {
                callEditSdiOnUserSDC(dsActiveUsers, __PROPS_ACTIVE_FLAG, __PROPS_ACTIVE_FLAG, disableReason);
                forceLogoffActiveUsers(dsActiveUsers);
            }

            callEditSdiOnSysMaintenanceSDC(id, opr, currentUser, disableReason);

        } else {
            __ERRORMSG = "System maintenance can be started only if system maintenance event is not yet already started or completed.";
            throw new SapphireException("Validation", ErrorDetail.TYPE_VALIDATION, getTranslationProcessor().translate(__ERRORMSG));
        }


    }

    /**
     * This methhod is called to perform extend System Maintenance after it has been started
     *
     * @param id
     * @param currentUser
     * @param opr
     * @param extendedByHours
     * @param extendedByMinutes
     * @return void
     * @throws SapphireException
     */
    private void extendSystemMaintenance(String id, String opr, String currentUser, String extendedByHours, String extendedByMinutes) throws SapphireException {
        DataSet dsUsers;
        DataSet dsSystemMaintenance;
        dsSystemMaintenance = fetchCurrentSystemMaintenaceDetails(id);
        String maintenanceStatus = dsSystemMaintenance.getValue(0, __PROPS_SYSMAINT_MAINTEANCE_STATUS, "");
        if (maintenanceStatus.equalsIgnoreCase(STARTED_SYSTEM_MAINTENANCE) || maintenanceStatus.equalsIgnoreCase(EXTENDED_SYSTEM_MAINTENANCE)) {
            dsUsers = fetchListOfDisabledUsers();

            Date plannedEndDateCDT = dsSystemMaintenance.getCalendar(0, __PROPS_SYSMAINT_PLANNED_END_DATE_CDT).getTime();
            Date plannedEndDateCET = dsSystemMaintenance.getCalendar(0, __PROPS_SYSMAINT_PLANNED_END_DATE_CET).getTime();
            Date plannedEndDateSGT = dsSystemMaintenance.getCalendar(0, __PROPS_SYSMAINT_PLANNED_END_DATE_SGT).getTime();

            Integer plannedEndTimeHours = (Integer.parseInt(dsSystemMaintenance.getValue(0, __PROPS_SYSMAINT_PLANNED_END_DATE_HOURS, "0")) + Integer.parseInt(extendedByHours)) % 24;
            Integer plannedEndTimeMinutes = (Integer.parseInt(dsSystemMaintenance.getValue(0, __PROPS_SYSMAINT_PLANNED_END_DATE_MINUTES, "0")) + Integer.parseInt(extendedByMinutes)) % 60;

            Calendar plannedEndDateCDTFormatted = AlconUtil.addHoursMinutesToJavaUtilDate(plannedEndDateCDT, extendedByHours, extendedByMinutes);
            Calendar plannedEndDateCETFormatted = AlconUtil.addHoursMinutesToJavaUtilDate(plannedEndDateCET, extendedByHours, extendedByMinutes);
            Calendar plannedEndDateSGTFormatted = AlconUtil.addHoursMinutesToJavaUtilDate(plannedEndDateSGT, extendedByHours, extendedByMinutes);

            String disableReason = formDisableReason(plannedEndDateCDTFormatted.getTime(), plannedEndDateCETFormatted.getTime(), plannedEndDateSGTFormatted.getTime());

            if (dsUsers.size() > 0) {
                callEditSdiOnUserSDC(dsUsers, __PROPS_ACTIVE_FLAG, __PROPS_ACTIVE_FLAG, disableReason);
            }

            DataSet dsTemp = new DataSet(connectionInfo);
            dsTemp.addColumn(__PROPS_SYSMAINT_PLANNED_END_DATE_CDT, DataSet.DATE);
            dsTemp.addRow(0);
            dsTemp.setDate(0, __PROPS_SYSMAINT_PLANNED_END_DATE_CDT, plannedEndDateCDTFormatted);
            plEditSDI.setProperty(__PROPS_SYSMAINT_PLANNED_END_DATE_HOURS, plannedEndTimeHours.toString());
            plEditSDI.setProperty(__PROPS_SYSMAINT_PLANNED_END_DATE_MINUTES, plannedEndTimeMinutes.toString());
            plEditSDI.setProperty(__PROPS_SYSMAINT_PLANNED_END_DATE_CDT, dsTemp.getValue(0, __PROPS_SYSMAINT_PLANNED_END_DATE_CDT, ""));
            callEditSdiOnSysMaintenanceSDC(id, opr, currentUser, disableReason);


        } else {
            __ERRORMSG = "System maintenance extension for the selected item is only possible when system maintenance is in progress.";
            throw new SapphireException("Validation", ErrorDetail.TYPE_VALIDATION, __ERRORMSG);
        }
    }

    /**
     * This methhod is called to perform end System Maintenance after it has been started
     *
     * @param id
     * @param currentUser
     * @param opr
     * @return void
     * @throws SapphireException
     */
    private void endSystemMaintenance(String id, String opr, String currentUser) throws SapphireException {
        DataSet dsUsers;
        DataSet dsSystemMaintenance;
        dsSystemMaintenance = fetchCurrentSystemMaintenaceDetails(id);
        String maintenanceStatus = dsSystemMaintenance.getValue(0, __PROPS_SYSMAINT_MAINTEANCE_STATUS, "");
        if (maintenanceStatus.equalsIgnoreCase(STARTED_SYSTEM_MAINTENANCE) || maintenanceStatus.equalsIgnoreCase(EXTENDED_SYSTEM_MAINTENANCE)) {
            dsUsers = fetchListOfDisabledUsers();

            if (dsUsers.size() > 0) {
                callEditSdiOnUserSDC(dsUsers, __PROPS_INACTIVE_FLAG, __PROPS_INACTIVE_FLAG, "");
            }

            callEditSdiOnSysMaintenanceSDC(id, opr, currentUser, "");

        } else {
            __ERRORMSG = "System maintenance for the selected item can be ended only when system maintenance is in progress.";
            throw new SapphireException("Validation", ErrorDetail.TYPE_VALIDATION, __ERRORMSG);
        }
    }

    /**
     * This methhod is called to check if any other system maintenance is in progress or not
     *
     * @return boolean
     */
    private boolean otherSystemMaintenanceInProgress() {
        boolean otherSystemMaintenanceInProgress = true;
        String sqlText = "select count(*) systemmaintenancecount from U_SYSMAINTENANCE WHERE maintenancestatus in (?, ?)";
        DataSet dsTemp = getQueryProcessor().getPreparedSqlDataSet(sqlText, new Object[]{STARTED_SYSTEM_MAINTENANCE, EXTENDED_SYSTEM_MAINTENANCE});
        if (dsTemp.getInt(0, "systemmaintenancecount", 0) > 0) {
            otherSystemMaintenanceInProgress = true;
        } else {
            otherSystemMaintenanceInProgress = false;
        }
        return otherSystemMaintenanceInProgress;
    }

    /**
     * This methhod is called to fetch ongoing System Maintenance details
     *
     * @param id
     * @return DataSet
     * @throws SapphireException
     */
    private DataSet fetchCurrentSystemMaintenaceDetails(String id) throws SapphireException {
        String sqlText = "select u_sysmaintenanceid, createdt, createby, plannedenddate, (plannedenddate + 1 / 1440 * 420) plannedEndDateCET, (plannedenddate + 1 / 1440 * 840) plannedEndDateSGT, " +
                "plannedendtimehh, plannedendtimemm, actualstartdate, startedby, actualextendeddate, extendedby, actualenddate, endedby, " +
                "disablereason, maintenancestatus FROM U_SYSMAINTENANCE where u_sysmaintenanceid = ?";
        DataSet dsTemp = getQueryProcessor().getPreparedSqlDataSet(sqlText, new Object[]{id});
        if (dsTemp.getRowCount() == 0) {
            __ERRORMSG = "Operation Failure. Incorrect Data.";
            throw new SapphireException(__ERRORMSG);
        }
        return dsTemp;
    }

    /**
     * This methhod is called to form the reason for disabling based on the date and time entered
     *
     * @param plannedEndTimeCDT
     * @param plannedEndTimeCET
     * @param plannedEndTimeSGT
     * @return String
     */
    private String formDisableReason(Date plannedEndTimeCDT, Date plannedEndTimeCET, Date plannedEndTimeSGT) {
        String rtrn = "";
        rtrn = "System Maintenance until " + new SimpleDateFormat("hh:mm a").format(plannedEndTimeCDT) + " (CST), "
                + new SimpleDateFormat("hh:mm a").format(plannedEndTimeCET) + " (CET), "
                + new SimpleDateFormat("hh:mm a").format(plannedEndTimeSGT) + " (SGT)";
        return rtrn;
    }

    /**
     * This method is called to fetch list of all active user (who are not disabled)
     *
     * @return DataSet
     */
    private DataSet fetchListOfActiveUsers() {
        DataSet dsTemp;
        String sqlText = "select distinct sysuserid FROM sysuser where nvl(disabledflag, 'N') = 'N'";
        dsTemp = getQueryProcessor().getPreparedSqlDataSet(sqlText, new Object[]{});
        return dsTemp;
    }

    /**
     * This method is called to fetch list of all excluded user based on the entered list
     *
     * @param id
     * @return DataSet
     */
    private DataSet fetchListOfExcludedUsers(String id) {
        String sqlText = "select distinct usr.sysuserid FROM sysuser usr, sysuserjobtype job " +
                "where usr.sysuserid = job.sysuserid " +
                "and (job.jobtypeid in (select jobtypeid from u_sysmaintenancejobtype where u_sysmaintenanceid = ?) " +
                "or (usr.sysuserid in (select sysuserid from u_sysmaintenanceuser where u_sysmaintenanceid = ?))) " +
                "and nvl(usr.disabledflag, 'N') = 'N'";
        DataSet dsTemp = getQueryProcessor().getPreparedSqlDataSet(sqlText, new Object[]{id, id});


        return dsTemp;
    }

    /**
     * This method is called to fetch list of all disabled user disabled during System Maintenance
     *
     * @return DataSet
     */
    private DataSet fetchListOfDisabledUsers() {

        String sqlText = "select distinct sysuserid from sysuser where u_disabledforsysmaint = 'Y' and disabledflag = 'Y' ";
        DataSet dsTemp = getQueryProcessor().getPreparedSqlDataSet(sqlText, new Object[]{});
        return dsTemp;
    }

    /**
     * This method is called to edit the User SDC based on the operation being performed
     *
     * @param disabledflag
     * @param disabledforsysmaint
     * @param disabledreason
     * @param dsUserList
     * @throws SapphireException
     */
    private void callEditSdiOnUserSDC(DataSet dsUserList, String disabledforsysmaint, String disabledflag, String disabledreason) throws SapphireException {
        plEditSDI.setProperty(EditSDI.PROPERTY_SDCID, __PROPS_USER_SDCID);
        plEditSDI.setProperty(EditSDI.PROPERTY_KEYID1, dsUserList.getColumnValues(__PROPS_USER_KEYID1, ";"));
        plEditSDI.setProperty(__PROPS_USER_DISABLEDFORSYSMAINT, disabledforsysmaint);
        plEditSDI.setProperty(__PROPS_USER_DISABLEDFLAG, disabledflag);
        plEditSDI.setProperty(__PROPS_USER_DISABLEDREASON, disabledreason);
        getActionProcessor().processAction(EditSDI.ID, EditSDI.VERSIONID, plEditSDI);
        plEditSDI.clear();
    }

    /**
     * This method is called to edit the System Maintenance SDC based on the operation being performed
     *
     * @param id
     * @param currentUser
     * @param opr
     * @param disableReason
     * @throws SapphireException
     */
    private void callEditSdiOnSysMaintenanceSDC(String id, String opr, String currentUser, String disableReason) throws SapphireException {

        plEditSDI.setProperty(EditSDI.PROPERTY_SDCID, __PROPS_SYSMAINT_SDCID);
        plEditSDI.setProperty(EditSDI.PROPERTY_KEYID1, id);
        switch (opr) {
            case START_SYSTEM_MAINTENANCE:
                plEditSDI.setProperty(__PROPS_SYSMAINT_ACTUAL_START_DATE, "n");
                plEditSDI.setProperty(__PROPS_SYSMAINT_STARTED_BY, currentUser);
                plEditSDI.setProperty(__PROPS_SYSMAINT_MAINTEANCE_STATUS, STARTED_SYSTEM_MAINTENANCE);
                plEditSDI.setProperty(__PROPS_SYSMAINT_DISABLEDREASON, disableReason);
                getActionProcessor().processAction(EditSDI.ID, EditSDI.VERSIONID, plEditSDI);
                plEditSDI.clear();
                break;
            case EXTEND_SYSTEM_MAINTENANCE:
                plEditSDI.setProperty(__PROPS_SYSMAINT_ACTUAL_EXTEND_DATE, "n");
                plEditSDI.setProperty(__PROPS_SYSMAINT_EXTENDED_BY, currentUser);
                plEditSDI.setProperty(__PROPS_SYSMAINT_MAINTEANCE_STATUS, EXTENDED_SYSTEM_MAINTENANCE);
                plEditSDI.setProperty(__PROPS_SYSMAINT_DISABLEDREASON, disableReason);
                getActionProcessor().processAction(EditSDI.ID, EditSDI.VERSIONID, plEditSDI);
                plEditSDI.clear();
                break;
            case END_SYSTEM_MAINTENANCE:
                plEditSDI.setProperty(__PROPS_SYSMAINT_ACTUAL_END_DATE, "n");
                plEditSDI.setProperty(__PROPS_SYSMAINT_ENDED_BY, currentUser);
                plEditSDI.setProperty(__PROPS_SYSMAINT_MAINTEANCE_STATUS, ENDED_SYSTEM_MAINTENANCE);
                getActionProcessor().processAction(EditSDI.ID, EditSDI.VERSIONID, plEditSDI);
                plEditSDI.clear();
                break;
        }
    }

    /**
     * This method is called to remove excluded users from the list of active users
     *
     * @param columnId
     * @param dsFrom
     * @param dsTo
     * @return DataSet
     */
    private DataSet removeFromDataSet(String columnId, DataSet dsFrom, DataSet dsTo) {
        HashMap hmFilter = new HashMap();
        for (int i = 0; i < dsTo.getRowCount(); i++) {
            hmFilter.put(columnId, dsTo.getValue(i, columnId, ""));
            if (dsFrom.findRow(hmFilter) != -1) {
                dsFrom.remove(dsFrom.findRow(hmFilter));
            }
        }
        return dsFrom;
    }

    /**
     * This method is called to force lof off all active users once the system maintenance phase has started
     *
     * @param dsActiveUsers
     * @return DataSet
     * @throws SapphireException
     */
    private void forceLogoffActiveUsers(DataSet dsActiveUsers) throws SapphireException {
        String activeUsers = dsActiveUsers.getColumnValues("sysuserid", ";");
        String rsetId = getDAMProcessor().createRSet("User", activeUsers, "", "");

        String sql = "delete from connection where exists (select null from sysuser s, rsetitems r where s.sysuserid=connection.sysuserid and r.sdcid='User' and r.keyid1=s.sysuserid and r.rsetid = '" + rsetId + "'  ) ";
        getQueryProcessor().execSQL(sql);

        if (rsetId != null) {
            getDAMProcessor().clearRSet(rsetId);
        }

    }

    /**
     * This method is called to add current user to excluded user list
     *
     * @param id
     * @param currentUser
     * @param dsActiveUsers
     * @return DataSet
     * @throws SapphireException
     */
    private void addCurrentUserToExcludedUserList(String id, String currentUser, DataSet dsActiveUsers) throws SapphireException {
        PropertyList plAddSDIDetail = new PropertyList();
        plAddSDIDetail.setProperty(AddSDIDetail.PROPERTY_SDCID, __PROPS_SYSMAINT_SDCID);
        plAddSDIDetail.setProperty(AddSDIDetail.PROPERTY_KEYID1, id);
        plAddSDIDetail.setProperty(AddSDIDetail.PROPERTY_LINKID, __PROPS_USER_LINKID);
        plAddSDIDetail.setProperty(__PROPS_USER_KEYID1, currentUser);
        getActionProcessor().processAction(AddSDIDetail.ID, AddSDIDetail.VERSIONID, plAddSDIDetail);

    }

    private boolean validateDayLightSaving(Date date, String timeZone) {
        boolean dayLightSavingMode = false;
        TimeZone tz = TimeZone.getTimeZone(timeZone);
        if (tz.inDaylightTime(date)) {
            dayLightSavingMode = true;
        }
        return dayLightSavingMode;
    }

}
