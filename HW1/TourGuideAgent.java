import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.FSMBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import java.io.IOException;
import java.util.ArrayList;

public class TourGuideAgent extends Agent{
   private Artifact[] catalog;
   private AID curator;
    
    // Agent initialization
    @Override
    protected void setup() {
        System.out.println("Agent "+getAID().getLocalName()+" is ready.");
        
        
        // register with DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd1  = new ServiceDescription();
        sd1.setType("tour-guide");
        sd1.setName(getLocalName());
        dfd.addServices(sd1);
        try {DFService.register(this, dfd);}
        catch (FIPAException fe) {}
        
        FSMBehaviour fsm = new FSMBehaviour();
        fsm.registerFirstState(new subscribeToCuratorsBehaviour(this, 3000), "A");
        fsm.registerState(new getCatalogBehaviour(), "B");
        fsm.registerLastState(new handleTourRequests(), "C");
        fsm.registerDefaultTransition("A", "B");
        fsm.registerDefaultTransition("B", "C");
        addBehaviour(fsm);
    }
    // Subsribing to curators
    private class subscribeToCuratorsBehaviour extends WakerBehaviour {

        private subscribeToCuratorsBehaviour(Agent a, long timeout) {
            super(a, timeout);
        }
        
        @Override
        protected void onWake() {
            
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription tsd = new ServiceDescription();
            tsd.setType("curator");
            template.addServices(tsd);
            SearchConstraints sc = new SearchConstraints();
            sc.setMaxResults(new Long(1));
            send(DFService.createSubscriptionMessage(myAgent, getDefaultDF(), template, sc));  
        }

    }
    
    private class getCatalogBehaviour extends OneShotBehaviour {
        @Override
        public void action() {
        
            // getting curator's AID
            ACLMessage msg = blockingReceive(MessageTemplate.MatchSender(getDefaultDF()));    
            try {
                DFAgentDescription[] dfds = 
                    DFService.decodeNotification(msg.getContent());
            curator = dfds[0].getName();
            System.out.println(getAID().getLocalName() + " got a curator: " 
                    + curator.getLocalName());
            }
            catch (Exception ex) {}
            
            // sending request for the museum catalog
            ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
            req.addReceiver(curator);
            req.setContent("museum-catalog");
            send(req);
            
            // handling the message
            ACLMessage reply = blockingReceive(MessageTemplate.MatchSender(curator));
            try {
                Artifact[] museum = (Artifact[]) reply.getContentObject();
                setCatalog (museum);
            }
            catch (UnreadableException ex) {}
        }
    }
    
    // handle messages from profiler agent
    private class handleTourRequests extends CyclicBehaviour {
        @Override public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = receive(mt);
            if (msg!=null) {
                String interest = msg.getContent();
                ArrayList<Integer> res = new ArrayList<>();
                for (Artifact catalog1 : catalog) {
                    if (catalog1.getGenre().equals(interest)) {
                        res.add(catalog1.getId());
                    }
                }
                try {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContentObject(res);
                    send(reply);
                } 
                catch (IOException ex) {}          
            }
        block();
        }
    }
    
    private void setCatalog(Artifact[] catalog) {
        this.catalog = catalog;
    }
    
    private void setCurator(AID curator) {
        this.curator = curator;
    }
    
    // Agent termination
    @Override
    protected void takeDown() {
        // Deregister from the yellow pages
        try {DFService.deregister(this);}
        catch (FIPAException fe) {}
        // Printout a dismissal message
        System.out.println("Agent " + getAID().getName() + " terminating.");
    }
}
