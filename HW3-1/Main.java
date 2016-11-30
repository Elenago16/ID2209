import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

public class Main {
    
    public static final int QUEENS = 8;

    public static void main(String[] args)  throws StaleProxyException{
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true);
        rt.createMainContainer(new ProfileImpl());
        
        AgentContainer c = rt.createAgentContainer(new ProfileImpl());
        for (int i = 0; i < QUEENS; i++)
            c.createNewAgent("Queen" + i, "QueenAgent", new Object[]{i, QUEENS}).start();
    }

}
