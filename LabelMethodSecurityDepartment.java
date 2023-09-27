package labvantage.custom.alcon.actions;

import com.labvantage.sapphire.modules.eln.gwt.server.worksheetitem.Attachment;
import sapphire.SapphireException;
import sapphire.accessor.AttachmentProcessor;
import sapphire.action.AddSDISecurityDept;
import sapphire.action.BaseAction;
import sapphire.action.DeleteSDISecurityDep;
import sapphire.error.ErrorDetail;
import sapphire.util.ConnectionInfo;
import sapphire.util.StringUtil;
import sapphire.xml.PropertyList;

/**
 * $Author: SARKASAB $
 * $Date: 2022-04-21 11:23:13 +0530 (Thu, 21 Apr 2022) $
 * $Revision: 52 $
 */

/*****************************************************************************************************
 * $Revision: 52 $
 * Description: This class is for MDLIMS-1109 - Label Method department control and manage attachment.
 * @author
 * @version
 ******************************************************************************************************/

public class LabelMethodSecurityDepartment extends BaseAction {

    public static final String DEVOPS_ID = "$Revision: 52 $";
    private static final String ID = "LabelMethodSecurityDepartment";
    private static final String VERSIONID = "1";
    private static final String __SDC_LABEL_METHOD = "LV_LabelMethod";
    private static final String __PROP_LABEL_METHOD_ID = "selectedlabelmethod";
    private static final String __PROP_LABEL_METHOD_VERSION = "selectedlabelmethodversionid";
    private static final String __PROP_DEPARTMENT_ID = "departmentid";
    private static final String __PROP_OPERATION = "operations";
    private static final String __PROP_ACTIVITY = "activity";

    @Override
    public void processAction(PropertyList properties) throws SapphireException {
        logger.info("=============== Start Processing Action: " + ID + ", Version:" + VERSIONID + "===============");
        // ********* Getting all input property
        String labelMethodId = properties.getProperty(__PROP_LABEL_METHOD_ID);
        String labelMethodVersionId = properties.getProperty(__PROP_LABEL_METHOD_VERSION);
        String departmentid = properties.getProperty(__PROP_DEPARTMENT_ID);
        String operations = properties.getProperty(__PROP_OPERATION);
        String activity = properties.getProperty(__PROP_ACTIVITY);
        // ********* Checking mandatory fields
        if (!checkNullOrEmpty(labelMethodId)) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Mandatory field " + __PROP_LABEL_METHOD_ID + " not found."));
        }
        if (!checkNullOrEmpty(labelMethodVersionId)) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Mandatory field " + __PROP_LABEL_METHOD_VERSION + " not found."));
        }
        if (!checkNullOrEmpty(departmentid)) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Mandatory field " + __PROP_DEPARTMENT_ID + " not found."));
        }
        if (!checkNullOrEmpty(activity)) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. Mandatory field " + __PROP_ACTIVITY + " not found."));
        }
        // *********** By default operation value is list
        if (!checkNullOrEmpty(operations)) {
            operations = "list";
        }

        if ("add".equalsIgnoreCase(activity)) {
            // ********* adding details to sdisecuritydepartment
            addSDISecurityDepartment(labelMethodId, labelMethodVersionId, departmentid, operations);
        } else {
            // ************* delete departments from sdisecuritydepartment
            deleteSDISecurityDepartment(labelMethodId, labelMethodVersionId, departmentid);
        }

    }

    /**********************************************************************************
     * This method is used to register or unregister shared department.
     * @param labelMethodId Label Method Id.
     * @param labelMethodVersionId Label Method Version Id.
     * @param departmentid Shared Department.
     * @param operations Always List.
     * @throws SapphireException OOB Sapphire Exception.
     **********************************************************************************/
    private void addSDISecurityDepartment(String labelMethodId, String labelMethodVersionId, String departmentid, String operations) throws SapphireException {
        logger.debug("Processing " + ID + ". (Action) : Inside addSDISecurityDepartment (method)");
        PropertyList plAddSDISecurityDept = new PropertyList();
        plAddSDISecurityDept.setProperty(AddSDISecurityDept.PROPERTY_SDCID, __SDC_LABEL_METHOD);
        plAddSDISecurityDept.setProperty(AddSDISecurityDept.PROPERTY_KEYID1, labelMethodId);
        plAddSDISecurityDept.setProperty(AddSDISecurityDept.PROPERTY_KEYID2, labelMethodVersionId);
        plAddSDISecurityDept.setProperty(AddSDISecurityDept.PROPERTY_KEYID3, "(null)");
        plAddSDISecurityDept.setProperty(AddSDISecurityDept.PROPERTY_DEPARTMENTID, departmentid);
        plAddSDISecurityDept.setProperty(AddSDISecurityDept.PROPERTY_OPERATIONID, StringUtil.repeat(operations, departmentid.split(";").length, ";"));

        try {
            getActionProcessor().processAction(AddSDISecurityDept.ID, AddSDISecurityDept.VERSIONID, plAddSDISecurityDept);
        } catch (SapphireException ex) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. inside - addSDISecurityDepartment. Error is -" + ex.getMessage()));
        }
    }

    /**************************************************************************************
     * This method is used to un-register a shared department from a Label Method.
     * @param labelMethodId Label Method Id.
     * @param labelMethodVersionId Label Method Version Id.
     * @param departmentid Shared Department Id
     * @throws SapphireException OOB Sapphire exception.
     **************************************************************************************/
    private void deleteSDISecurityDepartment(String labelMethodId, String labelMethodVersionId, String departmentid) throws SapphireException {
        logger.debug("Processing " + ID + ". (Action) : Inside deleteSDISecurityDepartment (method)");
        PropertyList plAddSDISecurityDept = new PropertyList();
        plAddSDISecurityDept.setProperty(DeleteSDISecurityDep.PROPERTY_SDCID, __SDC_LABEL_METHOD);
        plAddSDISecurityDept.setProperty(DeleteSDISecurityDep.PROPERTY_KEYID1, labelMethodId);
        plAddSDISecurityDept.setProperty(DeleteSDISecurityDep.PROPERTY_KEYID2, labelMethodVersionId);
        plAddSDISecurityDept.setProperty(DeleteSDISecurityDep.PROPERTY_KEYID3, "(null)");
        plAddSDISecurityDept.setProperty(DeleteSDISecurityDep.PROPERTY_DEPARTMENTID, departmentid);

        try {
            getActionProcessor().processAction(DeleteSDISecurityDep.ID, DeleteSDISecurityDep.VERSIONID, plAddSDISecurityDept);
        } catch (SapphireException ex) {
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE, getTranslationProcessor().translate(" Aborting transaction. inside - deleteSDISecurityDepartment. Error is -" + ex.getMessage()));
        }
    }

    /***************************************************
     * This method is used to check mandatory fields.
     * @param strMandatoryField Mandatory field value.
     * @return true or false.
     * @throws SapphireException OOB Sapphire Exception.
     ***************************************************/
    private boolean checkNullOrEmpty(String strMandatoryField) throws SapphireException {
        logger.debug("Processing " + ID + ". (Action) : Inside checkNullOrEmpty (method)");
        boolean mandatoryFlag = true;
        if (null == strMandatoryField || "".equalsIgnoreCase(strMandatoryField)) {
            mandatoryFlag = false;
        }
        return mandatoryFlag;
    }
}
