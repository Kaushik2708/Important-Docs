package labvantage.custom.alcon.actions;

import sapphire.SapphireException;
import sapphire.action.BaseAction;
import sapphire.xml.PropertyList;

/**
 * $Author: BAGCHAN1 $
 * $Date: 2022-04-20 01:39:24 +0530 (Wed, 20 Apr 2022) $
 * $Revision: 48 $
 */

/*****************************************************************************************************
 * Description: This class is used to release a Copied DataItem.
 ******************************************************************************************************/


public class ReleaseCopiedDataItem extends BaseAction {
    public static final String DEVOPS_ID = "$Revision: 48 $";

    public static final String ACTION_ID = "ReleaseCopiedDataItem";
    public static final String ACTION_VERSION_ID = "1";

    @Override
    public void processAction(PropertyList properties) throws SapphireException {

        PropertyList pl = new PropertyList();
        pl.clear();
        pl.setProperty("sdcid", properties.getProperty("sdcid"));
        pl.setProperty("keyid1", properties.getProperty("keyid1"));
        pl.setProperty("paramlistid", properties.getProperty("paramlistid"));
        pl.setProperty("paramlistversionid", properties.getProperty("paramlistversionid"));
        pl.setProperty("variantid", properties.getProperty("variantid"));
        pl.setProperty("dataset", properties.getProperty("dataset"));
        pl.setProperty("paramid", properties.getProperty("paramid"));
        pl.setProperty("paramtype", properties.getProperty("paramtype"));
        pl.setProperty("replicateid", properties.getProperty("replicateid"));
        pl.setProperty("enteredtext", properties.getProperty("enteredtext"));
        pl.setProperty("releasedflag", "Y");

        getActionProcessor().processAction("EnterDataItem", "1", pl);

        if (!"".equalsIgnoreCase(properties.getProperty("enteredtext"))) {
            getActionProcessor().processAction("ReleaseDataItem", "1", pl);
        }
    }

}
