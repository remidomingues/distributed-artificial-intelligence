package nqueens;


import homework1.model.Artifact;
import homework1.model.ArtifactCategory;
import homework1.model.AuctionDescription;
import homework1.model.Gender;
import homework1.model.User;
import homework1.model.Occupation;
import jade.core.AID;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;


public class QueenAgent extends Agent {
    private Logger myLogger = Logger.getJADELogger(getClass().getName());
    private List<AID> previousQueens = new LinkedList<AID>();

    public QueenAgent() {
        myLogger.log(Logger.INFO, "Profile Agent initialized");
    }
    
    
    public Logger getLogger() {
        return myLogger;
    }
    

    private class PositionNextQueen extends OneShotBehaviour {
        private int mNumQueens;
        private int mQueenIndex;

        public PositionNextQueen(Agent a, int numQueens, int queenIndex) {
            super(a);
            mNumQueens = numQueens;
            mQueenIndex = queenIndex;
        }

        @Override
        public void action() {
            QueenAgent profileAgent = (QueenAgent) myAgent;            

            // Create a queen and asking it to position itself in a valid position
            ContainerController cc = getContainerController();
            int newQueenIndex = mQueenIndex + 1;
            AgentController ac;
            try {
                ac = cc.createNewAgent("queen" + newQueenIndex,
                        "nqueens.QueenAgent",
                        new Object[]{mNumQueens, newQueenIndex});
                ac.start();
            } catch (StaleProxyException ex) {
                myLogger.log(Logger.SEVERE, null, ex);
                return;
            }
            
            ACLMessage requestMessage = new ACLMessage(ACLMessage.REQUEST);
            requestMessage.addReceiver(new AID("placenextqueensnamehere", false));
            
            AgentMessage agentMsg = new AgentMessage("take-position", null);

            try {
                requestMessage.setContentObject(agentMsg);
            } catch (IOException ex) {
                myLogger.log(Logger.SEVERE, "Exception while sending object message", ex);
                return;
            }
            send(requestMessage);
        }
    } // END of inner class PositionNextQueen
    
    protected void setup() {
        // Getting arguments
        Object[] args = getArguments();
        
        int numQueens = 8;
        int queenIndex = 1;
        if (args.length >= 2) {
            numQueens = (Integer) args[0];
            queenIndex = (Integer) args[1];
        }

        addBehaviour(new PositionNextQueen(this, numQueens, queenIndex));
        
    }
}

