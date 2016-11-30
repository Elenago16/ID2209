import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CuratorAgent extends Agent{
    final double[] strategy = {0.5, 0.7, 0.9};
    private boolean isInterested;
    int price;
    
// Agent initialization
    @Override 
    protected void setup() {
        System.out.println("Agent " + getAID().getLocalName() + " is ready.");
        
        // register with DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType("buyer");
        sd1.setName(getLocalName());
        dfd.addServices(sd1);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException ex) {
            Logger.getLogger(CuratorAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        
         // Setting up the behaviours
        SequentialBehaviour sb = new SequentialBehaviour();
        sb.addSubBehaviour(new enterAuction());
        sb.addSubBehaviour(new getBids());
        addBehaviour(sb);
    }
    
    // Picking auction strategy
    private class enterAuction extends OneShotBehaviour {
        
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = blockingReceive(mt);
            int offer = Integer.parseInt(msg.getContent());
            int s = (int) (Math.random() * strategy.length);
            price = (int) (offer * strategy [s]);
            isInterested = Math.random() < 0.5; // Random boolean
            StringBuilder sb = new StringBuilder();
            sb.append("Agent " + getAID().getLocalName() + " evaluated item to " + 
                    price + " and ");
            if (!isInterested) sb.append ("not ");
            sb.append("interested.");
            System.out.println(sb);
        }
    } 
    
    // Participate in auction
    private class getBids extends SimpleBehaviour {
        boolean done = false;
        
        @Override
        public void action() {
            if (!isInterested) done = true;
            ACLMessage msg = receive();
            if (msg != null) {
                switch (msg.getPerformative()) {
                    case ACLMessage.CFP:
                        int bid = Integer.parseInt(msg.getContent());
                        if (bid<=price) {
                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.PROPOSE);
                            reply.setContent(Integer.toString(bid));
                            send(reply);
                            System.out.println("Agent " + getAID().getLocalName() + 
                                    " wishes to buy the item.");
                        }
                        break;
                    case ACLMessage.ACCEPT_PROPOSAL:
                        done = true;
                        break;
                    case ACLMessage.INFORM:
                        done = true;
                        break;
                }
            }
        }

        @Override
        public boolean done() {
            return done;
        }
        
    }
    
    @Override protected void takeDown() {
        // Deregister from the yellow pages
        try {DFService.deregister(this);}
        catch (FIPAException fe) {}
        // Printout a dismissal message
        System.out.println("Agent " + getAID().getName() + " terminating.");
    }
}