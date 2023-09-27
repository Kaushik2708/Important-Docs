package labvantage.custom.alcon.actions;

import sapphire.SapphireException;
import sapphire.accessor.AttachmentProcessor;
import sapphire.action.BaseAction;
import sapphire.attachment.Attachment;
import sapphire.error.ErrorDetail;
import sapphire.xml.PropertyList;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * $Author: GHOSHKA1 $
 * $Date: 2022-04-28 17:11:54 +0530 (Thu, 28 Apr 2022) $
 * $Revision: 53 $
 */

/*****************************************************************************************************
 * $Revision: 53 $
 * Description: This class is for MDLIMS-1109 - Label Method department control and manage attachment.
 * @author Kaushik Ghosh
 * @version 1
 ******************************************************************************************************/

public class LabelMethodManageAttachment extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 53 $";
    private static final String ID = "LabelMethodManageAttachment";
    private static final String VERSIONID = "1";

    private static final String __SDC_ID = "sdcid";
    private static final String __PROP_KEY_ID_1 = "keyid1";
    private static final String __PROP_KEY_ID_2 = "keyid2";
    private static final String __PROP_KEY_ID_3 = "keyid3";
    private static final String __PROP_FILE_NAME = "filename";
    private static final String __PROP_ATTACHMENT_DESC = "attachmentdesc";
    private static final String __PROP_SRC_FILE_NAME = "sourcefilename";
    private static final String __PROP_TYPE_FLAG = "typeflag";

    @Override
    public void processAction(PropertyList properties) throws SapphireException {
        logger.info("=============== Start Processing Action: " + ID + ", Version:" + VERSIONID + "===============");
        // ***************** Getting all properties from page ********************
        String sdcid = properties.getProperty(__SDC_ID);
        String keyid1 = properties.getProperty(__PROP_KEY_ID_1);
        String keyid2 = ("".equalsIgnoreCase(properties.getProperty(__PROP_KEY_ID_2)) || properties.getProperty(__PROP_KEY_ID_2) == null) ? "(null)" : properties.getProperty(__PROP_KEY_ID_2);
        String keyid3 = ("".equalsIgnoreCase(properties.getProperty(__PROP_KEY_ID_3)) || properties.getProperty(__PROP_KEY_ID_3) == null) ? "(null)" : properties.getProperty(__PROP_KEY_ID_3);
        String absolutePath = properties.getProperty(__PROP_FILE_NAME);
        String fileName = getFileName(absolutePath);
        String attachmentdesc = fileName;
        String sourcefilename = fileName;
        // ******************* Only Upload option is provided hence typeflag = "U" always.
        String typeflag = "U";
        // ******************* Checking mandatory properties SDC Id, Keyid 1
        if(null==sdcid || "".equalsIgnoreCase(sdcid)){
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE,getTranslationProcessor().translate("Aborting transaction. mandatory property: " +__SDC_ID+" can't be blank or null."));
        }

        if(null==keyid1 || "".equalsIgnoreCase(keyid1)){
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE,getTranslationProcessor().translate("Aborting transaction. mandatory property: " +__PROP_KEY_ID_1+" can't be blank or null."));
        }
        if(null==absolutePath || "".equalsIgnoreCase(absolutePath)){
            throw new SapphireException("General Error", ErrorDetail.TYPE_FAILURE,getTranslationProcessor().translate("Aborting transaction. mandatory property: " +__PROP_FILE_NAME+" can't be blank or null."));
        }
        // ******************** Creating property list of Attachment for adding into SDIAttachment
        PropertyList plAttachment = new PropertyList();
        plAttachment.setProperty(__SDC_ID, sdcid);
        plAttachment.setProperty(__PROP_KEY_ID_1, keyid1);
        plAttachment.setProperty(__PROP_KEY_ID_2, keyid2);
        plAttachment.setProperty(__PROP_KEY_ID_3, keyid3);
        plAttachment.setProperty(__PROP_FILE_NAME, absolutePath);
        plAttachment.setProperty(__PROP_ATTACHMENT_DESC, attachmentdesc);
        plAttachment.setProperty(__PROP_SRC_FILE_NAME, sourcefilename);
        plAttachment.setProperty(__PROP_TYPE_FLAG, typeflag);
        // ********************* Creating attachment object by passing Attachment properties and Connection Id
        Attachment attch = Attachment.getAttachment(plAttachment, getConnectionId());
        // ******************** Creating attachment processor object
        AttachmentProcessor attchPr = new AttachmentProcessor(getConnectionid());
        // ******************** Adding attachment details to SDIAttachment table using Attachment Processor
        attchPr.addSDIAttachment(attch);
    }

    /*************************************************************
     * This method is used to extract filename from absoulte path.
     * @param absoulePath File absolute path
     * @return String containing File Name
     * @throws SapphireException OOB Sapphire Exception.
     **************************************************************/
    private String getFileName(String absoulePath) throws SapphireException {
        logger.debug("Processing " + ID + ". (Action) : Inside getFileName (method)");
        Path path = Paths.get(absoulePath);
        String fileName = "".equalsIgnoreCase(path.getFileName().toString()) ? "" : path.getFileName().toString();
        return fileName;
    }
}
