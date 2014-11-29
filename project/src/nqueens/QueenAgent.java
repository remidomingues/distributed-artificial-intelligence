package nqueens;

import jade.core.AID;

import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.util.Logger;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class QueenAgent extends Agent {
    private Logger myLogger = Logger.getJADELogger(getClass().getName());
    private List<Position> mPositionnedQueens = new LinkedList<>();
    private List<List<Position>> mSolutionsFound = new LinkedList<>();

    private int mNumQueens;
    private int mQueenIndex;
    private AID mPreviousQueen;
    
    public QueenAgent() {
    }
    
    
    public Logger getLogger() {
        return myLogger;
    }

    public int getNumQueens() {
        return mNumQueens;
    }

    public int getQueenIndex() {
        return mQueenIndex;
    }

    public AID getPreviousQueen() {
        return mPreviousQueen;
    }

    public List<Position> getPositionnedQueens() {
        return mPositionnedQueens;
    }
    
    private class PositionNextQueen extends OneShotBehaviour {

        public PositionNextQueen(Agent a) {
            super(a);
        }

        @Override
        public void action() {
            QueenAgent qAgent = (QueenAgent) myAgent;     
            
            // Pick a valid position
            int y = qAgent.getQueenIndex();
            for (int x = 0; x < qAgent.getNumQueens(); x++) {
                Boolean vulnerable = false;
                for (Position p : mPositionnedQueens) {
                    Boolean diag1 = (x - p.getX()) == (y - p.getY());
                    Boolean diag2 = (x - p.getX()) == -(y - p.getY());
                    if (x == p.getX() || y == p.getY() || diag1 || diag2 ) {
                        vulnerable = true;
                        break;
                    }
                }
                if (vulnerable) {
                    continue;
                }
                Position pickedPosition = new Position(x, y);
                mPositionnedQueens.add(pickedPosition);

                // If this is the last queen, we've found a solution.
                if (qAgent.getQueenIndex() >= qAgent.getNumQueens() - 1) {
                    // If the solution isn't already known, we show it and add it to the list of known solutions
                    if (!qAgent.solutionKnown(mPositionnedQueens)) {
                        myLogger.log(Logger.INFO, "@A solution was found:\n" + qAgent.solutionToString(mPositionnedQueens));
                        mSolutionsFound.add(new LinkedList<>(mPositionnedQueens));
                    }

                    // We try other positions to see if there are other solutions
                    mPositionnedQueens.remove(mPositionnedQueens.size() - 1);
                    continue;
                }

                // Otherwise, create a new queen and asking it to position itself in a valid position
                ContainerController cc = getContainerController();
                int newQueenIndex = qAgent.getQueenIndex() + 1;
                AgentController ac = null;
                String name = "queen" + newQueenIndex;
                while (ac == null) {
                    try {
                        ac = cc.createNewAgent(name, "nqueens.QueenAgent", new Object[]{newQueenIndex, qAgent.getAID(), mPositionnedQueens, mSolutionsFound});
                        ac.start();
                    } catch (StaleProxyException ex) {
                        //myLogger.log(Logger.SEVERE, null, ex);
                        name += "-bis";
                    }
                }

                // Wait for an answer (== no possible solution)
                ACLMessage response = qAgent.blockingReceive();
                AgentMessage msg;
                try {
                    msg = (AgentMessage) response.getContentObject();
                } catch (UnreadableException ex) {
                    myLogger.log(Logger.SEVERE, null, ex);
                    return;
                }
                
                if (msg.getType().equals("no-possible-solution")) {
                    // The new queen finished experimenting => we updated the list of solutions
                    mSolutionsFound = (List<List<Position>>) msg.getContent();
                    mPositionnedQueens.remove(mPositionnedQueens.size() -1);
                    continue;
                } else {
                    myLogger.log(Logger.SEVERE, "Unexpected msg type: " + msg.getType());
                    return;
                }
            }
            
            // If we got out of this loop == finished looking for solutions.
            // We tell that to the previous queen, giving the updated solutions ... and suicide
            if (qAgent.getQueenIndex() == 0) {
                myLogger.log(Logger.INFO, "FINISHED, solutions found: " + mSolutionsFound.size());
                for (List<Position> solution : mSolutionsFound) {
                    //myLogger.log(Logger.INFO,"\n" + solutionToString(solution) + "=================");
                }
            } else {
                sendMessage(qAgent.getPreviousQueen(), "no-possible-solution", mSolutionsFound);
            }
            qAgent.suicide();
        }
    } // END of inner class PositionNextQueen
    
    protected void sendMessage(AID receiver, String type, Object content) {
        ACLMessage requestMessage = new ACLMessage(ACLMessage.INFORM);
        requestMessage.addReceiver(receiver);
        AgentMessage agentMsg = new AgentMessage(type, content);
        try {
            requestMessage.setContentObject(agentMsg);
        } catch (IOException ex) {
            myLogger.log(Logger.SEVERE, "Exception while sending object message of type '"+type+"'", ex);
            return;
        }
        send(requestMessage);
    }
    
    public String solutionToString(List<Position> solution) {
        StringBuilder sb = new StringBuilder();
        for (Position p : solution) {
            for (int x = 0; x < mNumQueens; x++) {
                if (p.getX() == x) {
                    sb.append("X ");
                } else {
                    sb.append(". ");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    public Boolean solutionKnown(List<Position> solution) {
        for (List<Position> s : mSolutionsFound) {
            Boolean known = true;
            for (int i = 0; i < mNumQueens; i++) {
                if (!s.get(i).equals(solution.get(i))) {
                    known = false;
                    break;
                }
            }
            if (known) {
                return true;
            }
        }
        
        return false;
    }
    
    public void suicide() {
        doDelete();
        try {
            // Waiting for the agent to be deleted
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            myLogger.log(Logger.SEVERE, null, ex);
        }
    }
    
    protected void setup() {
        // Getting arguments
        Object[] args = getArguments();
        
        mNumQueens = 4;
        mQueenIndex = 0;
        mPreviousQueen = null;
        
        if (args != null) {
            if (args.length >= 1) {
                mQueenIndex = (Integer) args[0];
            }
            if (args.length >= 2) {
                mPreviousQueen = (AID) args[1];
            }
            if (args.length >= 3) {
                mPositionnedQueens = (LinkedList<Position>) args[2];
            }
            if (args.length >= 4) {
                mSolutionsFound = (List<List<Position>>) args[3];
            }
        }
        
        //myLogger.log(Logger.INFO, "Queen Agent initialized with: " + mQueenIndex + "/" + mNumQueens);
        addBehaviour(new PositionNextQueen(this));        
    }
}

