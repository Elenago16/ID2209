import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

public class Main {
    
    public static void main(String[] args)  throws StaleProxyException{
        
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);
        rt.createMainContainer(new ProfileImpl()).createNewAgent("rma", "jade.tools.rma.rma", new Object[]{}).start();
        
        Profile p = new ProfileImpl();
        p.setParameter(Profile.CONTAINER_NAME, "OriginalContainer");
        AgentContainer c = rt.createAgentContainer(p);
        c.createNewAgent("Manager", "AuctionAgent", new Object[]{}).start();
    }

}