import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.states.MsgReceiver;
import java.io.IOException;
import java.util.ArrayList;

public class CuratorAgent extends Agent{
    private Artifact[] museum;
    
    // Agent initialization
    @Override 
    protected void setup() {
        System.out.println("Agent " + getAID().getLocalName() + " is ready.");
        addArtifacts();
        
        // register with DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType("curator");
        sd1.setName(getLocalName());
        dfd.addServices(sd1);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException ex) {}
        
        // receive the messages
        MessageTemplate mcat = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                 addBehaviour(new receiveMessages(myAgent, mcat));
            } 
        });
    }
    
    private class receiveMessages extends MsgReceiver {
        
        private receiveMessages(Agent a, MessageTemplate m) {
            super(a, m, Long.MAX_VALUE,null,null);
        }
         
        @Override
        protected void handleMessage(ACLMessage msg) {
            
            if (msg.getContent().equals("museum-catalog")) {
                //System.out.println("Curator got a catalog request.");
                try {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContentObject(museum);
                    send(reply);
                } 
                catch (IOException ex) {} 
            }
            else {
                try {
                    ArrayList<Integer> items = (ArrayList) msg.getContentObject();
                    ArrayList<Artifact> res = new ArrayList();
                    items.stream().forEach((item) -> {
                        for (Artifact museum1 : museum) {
                            if (museum1.getId() == item) {
                                res.add(museum1);
                            }
                        }
                    });
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContentObject(res);
                    send(reply);
                } catch (UnreadableException | IOException ex) {}
            } 
        }
    }
    
    // Agent termination
    @Override protected void takeDown() {
        // Deregister from the yellow pages
        try {DFService.deregister(this);}
        catch (FIPAException fe) {}
        // Printout a dismissal message
        System.out.println("Agent " + getAID().getName() + " terminating.");
    }
    
    private void addArtifacts() {
    museum = new Artifact[3];
    museum [0]= new Artifact(67, "Mona Lisa", "Leonardo da Vinci", "Painting");
    museum [1] = new Artifact(12, "The Scream", "Edvard Munch", "Painting");
    museum [2] = new Artifact(75, "David", "Michelangelo", "Sculpture");
    }
}
