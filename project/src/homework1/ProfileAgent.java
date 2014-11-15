/**
 * ***************************************************************
 * JADE - Java Agent DEvelopment Framework is a framework to develop
 * multi-agent systems in compliance with the FIPA specifications.
 * Copyright (C) 2000 CSELT S.p.A.
 *
 * GNU Lesser General Public License
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation,
 * version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 * **************************************************************
 */
package homework1;

import homework1.model.Artifact;
import homework1.model.ArtifactCategory;
import homework1.model.Gender;
import homework1.model.Occupation;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.util.Logger;
import java.util.ArrayList;


public class ProfileAgent extends Agent {
    private Logger myLogger = Logger.getJADELogger(getClass().getName());

    private Gender gender;
    private Occupation occupation;
    private int age;
    private ArrayList<ArtifactCategory> interests;
    private ArrayList<Artifact> visitedArtifacts;

    public ProfileAgent() {
        myLogger.log(Logger.INFO, "Profile Agent initialized");
    }

    private class ArtifactsDetailsProviderBehaviour extends CyclicBehaviour {

        public ArtifactsDetailsProviderBehaviour(Agent a) {
            super(a);
        }

        public void action() {
            ACLMessage  msg = myAgent.receive();
            if(msg != null){
                ACLMessage reply = msg.createReply();

                if(msg.getPerformative()== ACLMessage.REQUEST){
                    String content = msg.getContent();
                    if ((content != null) && (content.indexOf("ping") != -1)){
                        myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Received PING Request from "+msg.getSender().getLocalName());
                        reply.setPerformative(ACLMessage.INFORM);
                        reply.setContent("pong");
                    }
                    else{
                        myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Unexpected request ["+content+"] received from "+msg.getSender().getLocalName());
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("( UnexpectedContent ("+content+"))");
                    }

                }
                else {
                    myLogger.log(Logger.INFO, "Agent "+getLocalName()+" - Unexpected message ["+ACLMessage.getPerformative(msg.getPerformative())+"] received from "+msg.getSender().getLocalName());
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                    reply.setContent("( (Unexpected-act "+ACLMessage.getPerformative(msg.getPerformative())+") )");
                }
                send(reply);
            }
            else {
                    block();
            }
        }
    } // END of inner class WaitPingAndReplyBehaviour

    protected void setup() {
        // Getting arguments
        // Example arguments: MALE,UNEMPLOYED,21,Mythology,Science
        Object[] args = getArguments();

        if (args == null || args.length < 4) {
            myLogger.log(Logger.SEVERE, "Didn't pass any/enough arguments to the Profile Agent");
            return;
        }

        this.gender = Gender.valueOf((String) args[0]);
        this.occupation = Occupation.valueOf((String) args[1]);
        this.age = Integer.parseInt((String) args[2]);
        
        this.interests = new ArrayList<ArtifactCategory>();
          
        for (int i = 3; i < args.length; i++) {
            String interest = (String) args[i];
            this.interests.add(ArtifactCategory.valueOf(interest));
        }

        this.visitedArtifacts = new ArrayList<Artifact>();
        
        // Registration with the DF
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("ProfileAgent");
        sd.setName(getName());
        sd.setOwnership("TILAB");
        dfd.setName(getAID());
        dfd.addServices(sd);
        try {
            DFService.register(this,dfd);
            ArtifactsDetailsProviderBehaviour PingBehaviour = new  ArtifactsDetailsProviderBehaviour(this);
            addBehaviour(PingBehaviour);
        } catch (FIPAException e) {
            myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
            doDelete();
        }
    }
}

