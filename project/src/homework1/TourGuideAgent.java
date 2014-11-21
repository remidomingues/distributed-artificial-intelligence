package homework1;

import homework1.model.ArtifactCategory;
import homework1.model.ArtifactDescription;
import homework1.model.ArtifactGenre;
import homework1.model.User;
import jade.core.AID;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


public class TourGuideAgent extends Agent {
    private Logger myLogger = Logger.getJADELogger(getClass().getName());
    public static final int NTHREADS = 5;

    public TourGuideAgent() {
        myLogger.log(Logger.INFO, "Tour Guide Agent initialized");
    }

    private class TourBuilderBehaviour extends CyclicBehaviour {
        private static final int TOUR_SIZE = 5;

        public TourBuilderBehaviour(Agent a) {
            super(a);
        }

        private Object pickRandom(Object[] objects, Random random) {
            return objects[new Random().nextInt(objects.length)];
        }

        public void action() {
            //Wait message from ProfileAgent
            ACLMessage  msg = myAgent.blockingReceive();
            
            if(msg == null){
                myLogger.log(Logger.SEVERE, "Null message received from {0}", msg.getSender().getLocalName());
                return;
            }
            
            AgentMessage message = null;
            try {
                message = ((AgentMessage)msg.getContentObject());
            } catch(Exception e) {
                myLogger.log(Logger.SEVERE, "Invalid message object received from "+msg.getSender().getLocalName(), e);
                return;
            }
            
            if(message.getType().equals("get-tour")) {
                User user = null;
                try {
                    user = (User) message.getContent();
                } catch(Exception e) {
                    myLogger.log(Logger.SEVERE, "Invalid message content received from "+msg.getSender().getLocalName(), e);
                    return;
                }
                
                myLogger.log(Logger.INFO, "Agent {0} - Received get-tour request from {1}", new Object[]{getLocalName(), msg.getSender().getLocalName()});
                
                List<ArtifactDescription> descriptions = new LinkedList<>();
                ArtifactGenre[] genres = ArtifactGenre.values();
                Object[] interests = user.getInterests().toArray();
                Random random = new Random();
                for(int i = 0; i < TOUR_SIZE; i++) {
                    ArtifactCategory categ = (ArtifactCategory) pickRandom(interests, random);
                    ArtifactGenre genre = (ArtifactGenre) pickRandom(genres, random);
                    descriptions.add(new ArtifactDescription(categ, genre));    
                }

                //Send artifacts ID request to CuratorAgent
                ACLMessage requestMessage = new ACLMessage(ACLMessage.REQUEST);
                requestMessage.addReceiver(new AID("curator", false));

                AgentMessage agentMsg = new AgentMessage("get-artifacts", descriptions);

                try {
                    requestMessage.setContentObject(agentMsg);
                } catch (IOException ex) {
                    myLogger.log(Logger.SEVERE, "Exception while serializing object message (artifacts description)", ex);
                    return;
                }
                send(requestMessage);
                
            } else if(message.getType().equals("get-artifacts")) {
                List<Integer> artifacts = null;
                try {
                    artifacts = (List<Integer>) message.getContent();
                } catch(Exception e) {
                    myLogger.log(Logger.SEVERE, "Invalid message content received from "+msg.getSender().getLocalName(), e);
                    return;
                }
                
                myLogger.log(Logger.INFO, "Agent {0} - Received get-artifacts request from {1}", new Object[]{getLocalName(), msg.getSender().getLocalName()});
                
                //Send artifacts ID response to ProfilerAgent
                ACLMessage requestMessage = new ACLMessage(ACLMessage.INFORM);
                requestMessage.addReceiver(new AID("profiler", false));

                AgentMessage agentMsg = new AgentMessage("get-tour", artifacts);

                try {
                    requestMessage.setContentObject(agentMsg);
                } catch (IOException ex) {
                    myLogger.log(Logger.SEVERE, "Exception while serializing object message (artifacts ID)", ex);
                    return;
                }
                send(requestMessage);
                
            } else {
                myLogger.log(Logger.SEVERE, "Invalid message type received from {0}", msg.getSender().getLocalName());
            }
        }
    } // END of inner class TourBuilderBehaviour

    protected void setup() {
        //Start multiple children to handle messages
        ParallelBehaviour parallelBehaviour = new ParallelBehaviour(this, ParallelBehaviour.WHEN_ALL);
        for(int i = 0; i < NTHREADS; ++i) {
            parallelBehaviour.addSubBehaviour(new TourBuilderBehaviour(this));
        }
        this.addBehaviour(parallelBehaviour);

        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("TourGuideBuilder");
        sd.setName(getName());
        sd.setOwnership("TILAB");
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            myLogger.log(Logger.INFO, "Agent {0} - Agent registered to Directory Facilitator {1}", new Object[]{getLocalName()});
            DFService.register(this,dfd);
        } catch (FIPAException e) {
            myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Could not register to DF", e);
            doDelete();
        }
    }
}
