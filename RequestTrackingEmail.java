package labvantage.custom.alcon.actions;

import sapphire.SapphireException;
import sapphire.action.BaseAction;
import sapphire.xml.PropertyList;

public class RequestTrackingEmail extends BaseAction {
    @Override
    public void processAction(PropertyList properties) throws SapphireException {
        String sampleId = properties.getProperty("sampleids");
        String sampleStatus = properties.getProperty("samplestatus");
        String requestIds = properties.getProperty("requestids");
    }
}
