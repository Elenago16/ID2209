import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ManagerAgent extends Agent{
    
    // Price of the item to be sold
    public final int PRICE = 10000;
    
    private int bid;
    private AID[] buyers;
    
    // Agent initialization
    @Override 
    protected void setup() {
        System.out.println("Agent " + getAID().getLocalName() + " is ready.");
        bid = PRICE * 2;
        
        // Setting up the behaviours
        SequentialBehaviour sb = new SequentialBehaviour();
        sb.addSubBehaviour(new registerParticipants(this, 5000));
        sb.addSubBehaviour(new startAuction());
        sb.addSubBehaviour(new performAuction());
        addBehaviour(sb);
    }
    @Override protected void takeDown() {
        // Deregister from the yellow pages
        try {DFService.deregister(this);}
        catch (FIPAException fe) {}
        // Printout a dismissal message
        System.out.println("Agent " + getAID().getName() + " terminating.");
    }
    
    
    // Get the list of buyers from the DF
    private class registerParticipants extends WakerBehaviour {

        registerParticipants(Agent agent, long period) {
            super(agent, period);
        }

        @Override
        protected void onWake() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription tsd = new ServiceDescription();
            tsd.setType("buyer");
            template.addServices(tsd);
            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                if (result.length > 0) {
                    buyers = new AID[result.length];
                    for (int i = 0; i < result.length; i++) 
                        buyers[i] = result[i].getName();
                }
            } catch (FIPAException ex) {}
        }
    }
    
    // Start an auction
    private class startAuction extends OneShotBehaviour {

        @Override
        public void action() {
            ACLMessage msg = new ACLMessage(ACLMessage.CFP);
            for (AID buyer : buyers) {msg.addReceiver(buyer);}
            msg.setContent(Integer.toString(PRICE));
            send(msg);
            System.out.println("Agent " + getAID().getLocalName() 
                    + " started the auction.");
        }
    }
    
    // Perform the auction
    private class performAuction extends ParallelBehaviour {

        performAuction() {
            super(WHEN_ANY); // terminates when any child is done
            
            // Increment the price and send offers to the buyers
            addSubBehaviour(new TickerBehaviour(myAgent, 3000) {

                @Override
                protected void onTick() {
                    bid *= 0.9;
                    ACLMessage msg = new ACLMessage(ACLMessage.CFP);
                    for (AID buyer : buyers) {msg.addReceiver(buyer);}
                    msg.setContent(Integer.toString(bid));
                    send(msg);
                    System.out.println("Current bid: " + bid);
                }
            });
            
            // Receive the offer from the buyer and end the auction
            addSubBehaviour(new SimpleBehaviour() {

                boolean done = false;

                @Override
                public void action() {
                    ACLMessage msg = receive();
                    if (msg != null) {
                        if (msg.getPerformative() == ACLMessage.PROPOSE) {
                            if (Integer.parseInt(msg.getContent()) == bid) {
                                AID winner = msg.getSender();
                                System.out.println("Item sold to " + winner.getLocalName());
                                
                                ACLMessage winMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                                winMsg.addReceiver(winner);
                                winMsg.setContent("You won the auction");
                                send(winMsg);
                    
                                ACLMessage endMsg = new ACLMessage(ACLMessage.INFORM);
                                for (AID buyer : buyers) {endMsg.addReceiver(buyer);}
                                endMsg.setContent("Auction ended");
                                send(endMsg);
                                done = true;
                            }
                        }
                    }
                }

                @Override
                public boolean done() {
                    return done;
                }
            });
        }
    }
}